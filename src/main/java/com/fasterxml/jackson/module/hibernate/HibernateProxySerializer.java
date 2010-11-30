package com.fasterxml.jackson.module.hibernate;

import java.io.IOException;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

/**
 * Serializer to use for values proxied using {@link HibernateProxy}.
 */
public class HibernateProxySerializer extends JsonSerializer<HibernateProxy>
{
    protected final boolean _forceLazyLoading;

    /*
    /**********************************************************
    /* Life cycle
    /**********************************************************
     */
    public HibernateProxySerializer(boolean forceLazyLoading)
    {
        _forceLazyLoading = forceLazyLoading;
    }

    /*
    /**********************************************************
    /* JsonSerializer impl
    /**********************************************************
     */
    
    @Override
    public void serialize(HibernateProxy value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonProcessingException
    {
        Object proxiedValue = findProxied(value);
        if (proxiedValue == null) {
            jgen.writeNull();
        } else {
            provider.defaultSerializeValue(proxiedValue, jgen);
        }
    }

    public void serializeWithType(HibernateProxy value, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonProcessingException
    {
        Object proxiedValue = findProxied(value);
        if (proxiedValue == null) {
            jgen.writeNull();
        } else {
            // slightly trickier, as we must call matching method
            provider.findValueSerializer(proxiedValue.getClass()).serializeWithType(proxiedValue, jgen, provider, typeSer);
        }
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    /**
     * Helper method for finding value being proxied, if it is available
     * or if it is to be forced to be loaded.
     */
    protected Object findProxied(HibernateProxy proxy)
    {
        LazyInitializer init = proxy.getHibernateLazyInitializer();
        if (!_forceLazyLoading && init.isUninitialized()) {
            return null;
        }
        return init.getImplementation();
    }
}
