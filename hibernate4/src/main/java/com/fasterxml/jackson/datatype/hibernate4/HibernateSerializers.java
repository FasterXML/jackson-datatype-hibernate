package com.fasterxml.jackson.datatype.hibernate4;

import org.hibernate.engine.spi.Mapping;
import org.hibernate.proxy.HibernateProxy;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.*;
import com.fasterxml.jackson.datatype.hibernate4.Hibernate4Module.Feature;

public class HibernateSerializers extends Serializers.Base
{
    protected final boolean _forceLoading;
    protected final boolean _serializeIdentifiers;
    protected final boolean _usePersistentClassForIdentifier;
    protected final Mapping _mapping;

    public HibernateSerializers(int features) {
        this(null, features);
    }

    public HibernateSerializers(Mapping mapping, int features)
    {
        _forceLoading = Feature.FORCE_LAZY_LOADING.enabledIn(features);
        _serializeIdentifiers = Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS.enabledIn(features);
        _usePersistentClassForIdentifier = Feature.SERIALIZE_IDENTIFIER_USE_PERSISTENT_CLASS.enabledIn(features);
        _mapping = mapping;
    }

    @Override
    public JsonSerializer<?> findSerializer(SerializationConfig config,
            JavaType type, BeanDescription beanDesc)
    {
        Class<?> raw = type.getRawClass();
        if (HibernateProxy.class.isAssignableFrom(raw)) {
            return new HibernateProxySerializer(_forceLoading, _serializeIdentifiers, _usePersistentClassForIdentifier, _mapping);
        }
        return null;
    }
}
