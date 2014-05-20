package com.fasterxml.jackson.datatype.hibernate4;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.type.*;

public class HibernateSerializerModifier
    extends BeanSerializerModifier
{
    protected final int _features;
    
    public HibernateSerializerModifier(int features) {
        _features = features;

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
        return new PersistentCollectionSerializer(serializer, _features);
    }

    @Override
    public JsonSerializer<?> modifyMapSerializer(SerializationConfig config,
            MapType valueType, BeanDescription beanDesc, JsonSerializer<?> serializer) {
        return new PersistentCollectionSerializer(serializer, _features);
    }
}
