package com.fasterxml.jackson.datatype.hibernate4;

import java.beans.Introspector;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

import javax.persistence.EntityNotFoundException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;
import com.fasterxml.jackson.databind.util.NameTransformer;

import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.proxy.pojo.BasicLazyInitializer;

/**
 * Serializer to use for values proxied using {@link org.hibernate.proxy.HibernateProxy}.
 *<p>
 * TODO: should try to make this work more like Jackson
 * <code>BeanPropertyWriter</code>, possibly sub-classing
 * it -- it handles much of functionality we need, and has
 * access to more information than value serializers (like
 * this one) have.
 */
public class HibernateProxySerializer
    extends JsonSerializer<HibernateProxy>
    implements ContextualSerializer
{
    /**
     * Property that has proxy value to handle
     */
    protected final BeanProperty _property;

    protected final boolean _forceLazyLoading;
    protected final boolean _serializeIdentifier;
    protected final boolean _nullMissingEntities;
    protected final boolean _wrappedIdentifier;
    protected final Mapping _mapping;

    // @since 2.11.2 (datatype-hibernate#97)
    protected final NameTransformer _unwrapper;

    /**
     * For efficient serializer lookup, let's use this; most
     * of the time, there's just one type and one serializer.
     */
    protected PropertySerializerMap _dynamicSerializers;
    
    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */

    @Deprecated // since 2.12
    public HibernateProxySerializer(boolean forceLazyLoading)
    {
        this(forceLazyLoading, false, false, true, null, null, null);
    }

    @Deprecated // since 2.12
    public HibernateProxySerializer(boolean forceLazyLoading, boolean serializeIdentifier) {
        this(forceLazyLoading, serializeIdentifier, false, true,
                null, null, null);
    }

    @Deprecated // since 2.12
    public HibernateProxySerializer(boolean forceLazyLoading, boolean serializeIdentifier, Mapping mapping) {
        this(forceLazyLoading, serializeIdentifier, false,  true,
                mapping, null, null);
    }

    @Deprecated // since 2.12
    public HibernateProxySerializer(boolean forceLazyLoading, boolean serializeIdentifier,
            boolean nullMissingEntities, Mapping mapping) {
        this(forceLazyLoading, serializeIdentifier, nullMissingEntities, true,
                mapping, null, null);
    }

    @Deprecated // since 2.12
    public HibernateProxySerializer(boolean forceLazyLoading, boolean serializeIdentifier,
            boolean nullMissingEntities, Mapping mapping, BeanProperty property) {
        this(forceLazyLoading, serializeIdentifier, nullMissingEntities, true,
                mapping, property, null);
    }

    /**
     * @since 2.12
     */
    public HibernateProxySerializer(boolean forceLazyLoading, boolean serializeIdentifier,
            boolean nullMissingEntities, boolean wrappedIdentifier,
            Mapping mapping)
    {
        this(forceLazyLoading, serializeIdentifier, nullMissingEntities, wrappedIdentifier,
                mapping, null, null);
    }

    /**
     * @since 2.12
     */
    public HibernateProxySerializer(boolean forceLazyLoading, boolean serializeIdentifier,
            boolean nullMissingEntities, boolean wrappedIdentifier,
            Mapping mapping, BeanProperty property, NameTransformer unwrapper)
    {
        _forceLazyLoading = forceLazyLoading;
        _serializeIdentifier = serializeIdentifier;
        _nullMissingEntities = nullMissingEntities;
        _wrappedIdentifier = wrappedIdentifier;
        _mapping = mapping;
        _property = property;
        _unwrapper = unwrapper;

        _dynamicSerializers = PropertySerializerMap.emptyForProperties();
    }

    /**
     * @since 2.12
     */
    protected HibernateProxySerializer(HibernateProxySerializer base,
            BeanProperty property, NameTransformer unwrapper)
    {
        _forceLazyLoading = base._forceLazyLoading;
        _serializeIdentifier = base._serializeIdentifier;
        _nullMissingEntities = base._nullMissingEntities;
        _wrappedIdentifier = base._wrappedIdentifier;
        _mapping = base._mapping;
        _property = property;
        _unwrapper = unwrapper;

        _dynamicSerializers = PropertySerializerMap.emptyForProperties();
    }
    
    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        return new HibernateProxySerializer(this, property, _unwrapper);
    }

    @Override
    public JsonSerializer<HibernateProxy> unwrappingSerializer(final NameTransformer unwrapper)
    {
        return new HibernateProxySerializer(this, _property, unwrapper);
    }

    @Override
    public boolean isUnwrappingSerializer() {
        return _unwrapper != null;
    }

    /*
    /**********************************************************************
    /* JsonSerializer impl
    /**********************************************************************
     */

    @Override
    public boolean isEmpty(SerializerProvider provider, HibernateProxy value) {
        return (value == null) || (findProxied(value) == null);
    }

    @Override
    public void serialize(HibernateProxy value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException
    {
        Object proxiedValue = findProxied(value);
        // TODO: figure out how to suppress nulls, if necessary? (too late for that here)
        if (proxiedValue == null) {
            provider.defaultSerializeNull(jgen);
            return;
        }
        findSerializer(provider, proxiedValue).serialize(proxiedValue, jgen, provider);
    }

    @Override
    public void serializeWithType(HibernateProxy value, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException
    {
        Object proxiedValue = findProxied(value);
        if (proxiedValue == null) {
            provider.defaultSerializeNull(jgen);
            return;
        }
        /* This isn't exactly right, since type serializer really refers to proxy
         * object, not value. And we really don't either know static type (necessary
         * to know how to apply additional type info) or other things;
         * so it's not going to work well. But... we'll do out best.
         */
        findSerializer(provider, proxiedValue).serializeWithType(proxiedValue, jgen, provider, typeSer);
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        throws JsonMappingException
    {
        SerializerProvider prov = visitor.getProvider();
        if ((prov == null) || (_property == null)) {
            super.acceptJsonFormatVisitor(visitor, typeHint);
        } else {
            JavaType type = _property.getType();
            prov.findPrimaryPropertySerializer(type, _property)
                .acceptJsonFormatVisitor(visitor, type);
        }
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    protected JsonSerializer<Object> findSerializer(SerializerProvider provider, Object value)
        throws IOException
    {
        /* TODO: if Hibernate did use generics, or we wanted to allow use of Jackson
         *  annotations to indicate type, should take that into account.
         */
        Class<?> type = value.getClass();
        /* we will use a map to contain serializers found so far, keyed by type:
         * this avoids potentially costly lookup from global caches and/or construction
         * of new serializers
         */
        /* 18-Oct-2013, tatu: Whether this is for the primary property or secondary is
         *   really anyone's guess at this point; proxies can exist at any level?
         */
        PropertySerializerMap.SerializerAndMapResult result =
                _dynamicSerializers.findAndAddPrimarySerializer(type, provider, _property);
        if (_dynamicSerializers != result.map) {
            _dynamicSerializers = result.map;
        }
        if (_unwrapper != null)
        {
            return result.serializer.unwrappingSerializer(_unwrapper);
        }
        return result.serializer;
    }

    /**
     * Helper method for finding value being proxied, if it is available
     * or if it is to be forced to be loaded.
     */
    protected Object findProxied(final HibernateProxy proxy)
    {
        LazyInitializer init = proxy.getHibernateLazyInitializer();
        if (!_forceLazyLoading && init.isUninitialized()) {
            if (_serializeIdentifier) {
                final Object idValue = init.getIdentifier();
                final Object result;
                if (_wrappedIdentifier) {
                  final HashMap<String, Object> map = new HashMap<>();
                  map.put(getIdentifierPropertyName(init), idValue);
                  result = map;
                } else {
                    result = idValue;
                }
                return result;
            }
            return null;
        }
        try {
            return init.getImplementation();
        } catch (EntityNotFoundException e) {
            if (_nullMissingEntities) {
                return null;
            } else {
                throw e;
            }
        }
    }

    /**
     * Helper method to retrieve the name of the identifier property of the
     * specified lazy initializer.
     * @param init Lazy initializer to obtain identifier property name from.
     * @return Name of the identity property of the specified lazy initializer.
     */
    private String getIdentifierPropertyName(final LazyInitializer init) {
      String idName;
      if (_mapping != null) {
        idName = _mapping.getIdentifierPropertyName(init.getEntityName());
      } else {
        final SessionImplementor session = init.getSession();
        if (session != null) {
          idName = session.getFactory().getIdentifierPropertyName(init.getEntityName());
        } else {
          idName = ProxyReader.getIdentifierPropertyName(init);
          if (idName == null) {
            idName = init.getEntityName();
          }
        }
      }
      return idName;
    }

    /**
     * Inspects a Hibernate proxy to try and determine the name of the identifier property
     * (Hibernate proxies know the getter of the identifier property because it receives special
     * treatment in the invocation handler). Alas, the field storing the method reference is
     * private and has no getter, so we must resort to ugly reflection hacks to read its value ...
     */
    protected static class ProxyReader {

        // static final so the JVM can inline the lookup
        private static final Field getIdentifierMethodField;

        static {
            try {
                getIdentifierMethodField = BasicLazyInitializer.class.getDeclaredField("getIdentifierMethod");
                getIdentifierMethodField.setAccessible(true);
            } catch (Exception e) {
            	// should never happen: the field exists in all versions of hibernate 4 and 5
                throw new RuntimeException(e);
            }
        }

        /**
         * @return the name of the identifier property, or null if the name could not be determined
         */
        static String getIdentifierPropertyName(LazyInitializer init) {
            try {
                Method idGetter = (Method) getIdentifierMethodField.get(init);
                if (idGetter == null) {
                	return null;
                }
                String name = idGetter.getName();
                if (name.startsWith("get")) {
                    name = Introspector.decapitalize(name.substring(3));
                }
                return name;
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
