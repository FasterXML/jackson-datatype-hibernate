package com.fasterxml.jackson.datatype.hibernate6;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import org.hibernate.SessionFactory;

public class Hibernate6SerializerModifier
    extends BeanSerializerModifier
{
    protected final int _features;

    protected final SessionFactory _sessionFactory;

    public Hibernate6SerializerModifier(int features, SessionFactory sessionFactory) {
        _features = features;
        _sessionFactory = sessionFactory;
    }

    /*
    @Override
    public JsonSerializer<?> modifySerializer(SerializationConfig config,
            BeanDescription beanDesc, JsonSerializer<?> serializer) {
        return serializer;
    }
    */

    @Override
    public JsonSerializer<?> modifyCollectionSerializer(SerializationConfig config,
            CollectionType valueType, BeanDescription beanDesc, JsonSerializer<?> serializer) {
        return new PersistentCollectionSerializer(valueType, serializer, _features, _sessionFactory);
    }

    @Override
    public JsonSerializer<?> modifyMapSerializer(SerializationConfig config,
            MapType valueType, BeanDescription beanDesc, JsonSerializer<?> serializer) {
        return new PersistentCollectionSerializer(valueType, serializer, _features, _sessionFactory);
    }
}
