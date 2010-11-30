package com.fasterxml.jackson.module.hibernate;

import javax.persistence.Transient;

import org.hibernate.collection.PersistentCollection;
import org.hibernate.proxy.HibernateProxy;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.type.JavaType;

public class HibernateSerializers implements Serializers
{

    public JsonSerializer<?> findSerializer(JavaType type, SerializationConfig config,
            BeanDescription beanDesc)
    {
        Class<?> raw = type.getRawClass();
        /* All Hibernate collection types (including maps!) implement this interface, so:
         */
        if (PersistentCollection.class.isAssignableFrom(raw)) {
//            return new PersistentCollectionSerializer(type, config);
        }

        if (HibernateProxy.class.isAssignableFrom(raw)) {
//            return new HibernateProxySerializer(type, config);
        }
        return null;
    }

}
