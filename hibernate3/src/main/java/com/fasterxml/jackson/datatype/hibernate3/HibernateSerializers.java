package com.fasterxml.jackson.datatype.hibernate3;

import org.hibernate.proxy.HibernateProxy;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.*;
import com.fasterxml.jackson.datatype.hibernate3.Hibernate3Module.Feature;

public class HibernateSerializers extends Serializers.Base
{
    protected final boolean _forceLoading;
    
    public HibernateSerializers(int features) {
        _forceLoading = Feature.FORCE_LAZY_LOADING.enabledIn(features);
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
