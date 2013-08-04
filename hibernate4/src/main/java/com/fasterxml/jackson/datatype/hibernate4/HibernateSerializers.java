package com.fasterxml.jackson.datatype.hibernate4;

import org.hibernate.engine.spi.Mapping;
import org.hibernate.proxy.HibernateProxy;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.*;

public class HibernateSerializers extends Serializers.Base
{
    protected final boolean _forceLoading;
    protected final boolean _serializeIdentifiers;
    protected final Mapping _mapping;

    public HibernateSerializers(boolean forceLoading)
    {
        this(forceLoading, false, null);
    }

    public HibernateSerializers(boolean forceLoading, boolean serializeIdentifiers)
    {
        this(forceLoading, serializeIdentifiers, null);
    }

    public HibernateSerializers(boolean forceLoading, boolean serializeIdentifiers, Mapping mapping)
    {
        _forceLoading = forceLoading;
        _serializeIdentifiers = serializeIdentifiers;
        _mapping = mapping;
    }

    @Override
    public JsonSerializer<?> findSerializer(SerializationConfig config,
            JavaType type, BeanDescription beanDesc)
    {
        Class<?> raw = type.getRawClass();
        if (HibernateProxy.class.isAssignableFrom(raw)) {
            return new HibernateProxySerializer(_forceLoading, _serializeIdentifiers, _mapping);
        }
        return null;
    }
}
