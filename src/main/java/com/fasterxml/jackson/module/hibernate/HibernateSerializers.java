package com.fasterxml.jackson.module.hibernate;

import java.util.*;

import org.hibernate.collection.PersistentCollection;
import org.hibernate.collection.PersistentMap;
import org.hibernate.proxy.HibernateProxy;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.type.CollectionType;
import org.codehaus.jackson.map.type.MapType;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;

import com.fasterxml.jackson.module.hibernate.HibernateModule.Feature;

public class HibernateSerializers extends Serializers.None
{
    protected final int _moduleFeatures;
    
    public HibernateSerializers(int features)
    {
        _moduleFeatures = features;
    }

    @Override
    public JsonSerializer<?> findSerializer(
            SerializationConfig config, JavaType type,
            BeanDescription beanDesc, BeanProperty beanProperty )
    {
        Class<?> raw = type.getRawClass();

        /* Note: PersistentCollection does not implement Collection, so we
         * may get some types here...
         */
        if (PersistentCollection.class.isAssignableFrom(raw)) {
            // TODO: handle iterator types? Or PersistentArrayHolder?
        }
        
        if (HibernateProxy.class.isAssignableFrom(raw)) {
            return new HibernateProxySerializer(beanProperty, isEnabled(Feature.FORCE_LAZY_LOADING));
        }
        return null;
    }

    @Override
    public JsonSerializer<?> findCollectionSerializer(SerializationConfig config,
            CollectionType type, BeanDescription beanDesc, BeanProperty property,
            TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer)
    {
        Class<?> raw = type.getRawClass();
        // only handle PersistentCollection style collections...
        if (PersistentCollection.class.isAssignableFrom(raw)) {
            /* And for those, figure out "fallback type"; we MUST have some idea of
             * type to deserialize, aside from nominal PersistentXxx type.
             */
            return new PersistentCollectionSerializer(property, _figureFallbackType(config, type),
                    isEnabled(Feature.FORCE_LAZY_LOADING));
        }
        return null;
    }

    @Override
    public JsonSerializer<?> findMapSerializer(SerializationConfig config,
            MapType type, BeanDescription beanDesc, BeanProperty property,
            JsonSerializer<Object> keySerializer,
            TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer)
    {
        Class<?> raw = type.getRawClass();
        if (PersistentMap.class.isAssignableFrom(raw)) {
            return new PersistentCollectionSerializer(property, _figureFallbackType(config, type),
                    isEnabled(Feature.FORCE_LAZY_LOADING));
        }
        return null;
    }
    
    public final boolean isEnabled(HibernateModule.Feature f) {
        return (_moduleFeatures & f.getMask()) != 0;
    }

    protected JavaType _figureFallbackType(SerializationConfig config,
            JavaType persistentType)
    {
        // Alas, PersistentTypes are NOT generics-aware... meaning can't specify parameterization
        Class<?> raw = persistentType.getRawClass();
        TypeFactory tf = config.getTypeFactory();
        if (Map.class.isAssignableFrom(raw)) {
            return tf.constructMapType(Map.class, Object.class, Object.class);
        }
        if (List.class.isAssignableFrom(raw)) {
            return tf.constructCollectionType(List.class, Object.class);
        }
        if (Set.class.isAssignableFrom(raw)) {
            return tf.constructCollectionType(Set.class, Object.class);
        }
        // ok, just Collection of some kind
        return tf.constructCollectionType(Collection.class, Object.class);
    }
}
