package com.fasterxml.jackson.datatype.hibernate4;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;

/**
 * Serializer to use for values proxied using {@link HibernateProxy}.
 *<p>
 * TODO: should try to make this work more like Jackson
 * <code>BeanPropertyWriter</code>, possibly sub-classing
 * it -- it handles much of functionality we need, and has
 * access to more information than value serializers (like
 * this one) have.
 */
public class HibernateProxySerializer
        extends JsonSerializer<HibernateProxy>
{
    /**
     * Property that has proxy value to handle
     */
    protected final BeanProperty _property;

    protected final boolean _forceLazyLoading;
    protected final boolean _serializeIdentifier;
    private boolean _usePersistentClassForIdentifier;
    protected final Mapping _mapping;
    static final ConcurrentHashMap<CacheKey, Field> _idFieldCache = new ConcurrentHashMap<HibernateProxySerializer.CacheKey, Field>();

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

    public HibernateProxySerializer(boolean forceLazyLoading)
    {
        this(forceLazyLoading, false, null);
    }

    public HibernateProxySerializer(boolean forceLazyLoading, boolean serializeIdentifier) {
        this(forceLazyLoading, serializeIdentifier, null);
    }

    public HibernateProxySerializer(boolean forceLazyLoading, boolean serializeIdentifier, Mapping mapping) {
        this(forceLazyLoading, serializeIdentifier, false, mapping);
    }

    public HibernateProxySerializer(boolean forceLazyLoading, boolean serializeIdentifier,
            boolean usePersistentClassForIdentifier, Mapping mapping) {
        _forceLazyLoading = forceLazyLoading;
        _serializeIdentifier = serializeIdentifier;
        _usePersistentClassForIdentifier = usePersistentClassForIdentifier;
        _mapping = mapping;
        _dynamicSerializers = PropertySerializerMap.emptyMap();
        _property = null;
    }

    /*
    /**********************************************************************
    /* JsonSerializer impl
    /**********************************************************************
     */

    // since 2.3
    @Override
    public boolean isEmpty(HibernateProxy value)
    {
        return (value == null) || (findProxied(value) == null);
    }

    @Override
    public void serialize(HibernateProxy value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException
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
            throws IOException, JsonProcessingException
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

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    protected JsonSerializer<Object> findSerializer(SerializerProvider provider, Object value)
            throws IOException, JsonProcessingException
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
        return result.serializer;
    }

    /**
     * Helper method for finding value being proxied, if it is available
     * or if it is to be forced to be loaded.
     */
    protected Object findProxied(HibernateProxy proxy)
    {
        LazyInitializer init = proxy.getHibernateLazyInitializer();
        if (!_forceLazyLoading && init.isUninitialized()) {
            if (_serializeIdentifier) {
                final String idName;
                if (_mapping != null) {
                    idName = _mapping.getIdentifierPropertyName(init.getEntityName());
                } else {
                    final SessionImplementor session = init.getSession();
                    if (session != null) {
                        idName = session.getFactory().getIdentifierPropertyName(init.getEntityName());
                    } else {
                        idName = init.getEntityName();
                    }
                }
                final Object idValue = init.getIdentifier();
                if (_usePersistentClassForIdentifier) {
                    Class<?> persistentClass = init.getPersistentClass();
                    Object identifierClass = createTargetIdentifierClassAndSetIdValue(persistentClass, idName, idValue);
                    return identifierClass;
                } else {
                    HashMap<String, Object> map = new HashMap<String, Object>();
                    map.put(idName, idValue);
                    return map;
                }
            }
            return null;
        }
        return init.getImplementation();
    }


    Object createTargetIdentifierClassAndSetIdValue(Class<?> persistentClass, final String idName, final Object idValue) {
        try {
            Constructor<?> defaultConstructor = persistentClass.getDeclaredConstructor();
            defaultConstructor.setAccessible(true);
            Object instance = defaultConstructor.newInstance();
            Field idField = getIdField(persistentClass, idName, instance);
            if(!idField.isAccessible()){
                idField.setAccessible(true);
            }
            idField.set(instance, idValue);
            return instance;
        } catch (Exception e) {
            throw new IllegalStateException("Error creating identifier class [" + persistentClass.getSimpleName() + "] and "
                    + "setting idValue with [" + idName + "=" + idValue + "]. Default constructor present and field available?",
                    e);
        }
    }

    private Field getIdField(Class<?> persistentClass, String idName, Object instance) throws NoSuchMethodException {
        CacheKey cacheKey = new CacheKey(persistentClass.getCanonicalName(), idName);
        Field field = _idFieldCache.get(cacheKey);
        if (field != null) {
            return field;
        }
        else {
            synchronized (_idFieldCache) {
                field = _idFieldCache.get(cacheKey);
                if (field == null) {
                    field = findField(persistentClass, idName, instance);
                    _idFieldCache.put(cacheKey, field);
                }
                return field;
            }
        }
    }

    private Field findField(Class<?> persistentClass, String idName, Object instance) throws NoSuchMethodException {
        if (persistentClass == null) {
            return null;
        }
        Field field;
        try {
            field = persistentClass.getDeclaredField(idName);
        } catch (NoSuchFieldException e) {
            field = findField(persistentClass.getSuperclass(), idName, instance);
        }
        return field;
    }

    private static class CacheKey {
        private final String clazz;
        private final String idName;

        public CacheKey(String clazz, String idName) {
            super();
            this.clazz = clazz;
            this.idName = idName;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
            result = prime * result + ((idName == null) ? 0 : idName.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            CacheKey other = (CacheKey) obj;
            if (clazz == null) {
                if (other.clazz != null)
                    return false;
            } else if (!clazz.equals(other.clazz))
                return false;
            if (idName == null) {
                if (other.idName != null)
                    return false;
            } else if (!idName.equals(other.idName))
                return false;
            return true;
        }

    }
    
    public static void clearIdFieldCache(){
        _idFieldCache.clear();
    }

}
