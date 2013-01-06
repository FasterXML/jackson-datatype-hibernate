package com.fasterxml.jackson.datatype.hibernate4;

import org.hibernate.proxy.HibernateProxy;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.*;

public class HibernateSerializers extends Serializers.Base
{
    protected final boolean _forceLoading;
    
    public HibernateSerializers(boolean forceLoading)
    {
        _forceLoading = forceLoading;
    }

    @Override
    public JsonSerializer<?> findSerializer(SerializationConfig config,
            JavaType type, BeanDescription beanDesc)
    {
        Class<?> raw = type.getRawClass();
        if (HibernateProxy.class.isAssignableFrom(raw)) {
            return new HibernateProxySerializer(_forceLoading);
        }
        return null;
    }
}
