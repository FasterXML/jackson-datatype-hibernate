package com.fasterxml.jackson.datatype.hibernate4;

import java.util.*;

import org.hibernate.collection.internal.PersistentMap;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.proxy.HibernateProxy;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.*;
import com.fasterxml.jackson.databind.type.*;
import com.fasterxml.jackson.datatype.hibernate4.Hibernate4Module.Feature;


public class HibernateSerializers extends Serializers.Base
{
    protected final int _moduleFeatures;
    
    public HibernateSerializers(int features)
    {
        _moduleFeatures = features;
    }

    @Override
    public JsonSerializer<?> findSerializer(SerializationConfig config,
            JavaType type, BeanDescription beanDesc)
    {
        Class<?> raw = type.getRawClass();

        /* 13-Jul-2012, tatu: There's a bug in Jackson 2.0 which will call this
         *    method in some cases for Collections (and Maps); let's skip
         *    those cases and wait for the "real" call
         */
        if (Collection.class.isAssignableFrom(raw)
                || Map.class.isAssignableFrom(raw)) {
            return null;
        }
        
        /* Note: PersistentCollection does not implement Collection, so we
         * may get some types here; most do implement Collection too however
         */
        if (PersistentCollection.class.isAssignableFrom(raw)) {
            // TODO: handle iterator types? Or PersistentArrayHolder?
            JavaType elementType = _figureFallbackType(config, type);
            return new PersistentCollectionSerializer(elementType,
                    isEnabled(Feature.FORCE_LAZY_LOADING));
        }
        if (HibernateProxy.class.isAssignableFrom(raw)) {
            return new HibernateProxySerializer(isEnabled(Feature.FORCE_LAZY_LOADING), isEnabled(Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS));
        }
        return null;
    }

    @Override
    public JsonSerializer<?> findCollectionSerializer(SerializationConfig config,
            CollectionType type, BeanDescription beanDesc,
            TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer)
    {
        Class<?> raw = type.getRawClass();
        // only handle PersistentCollection style collections...
        if (PersistentCollection.class.isAssignableFrom(raw)) {
            /* And for those, figure out "fallback type"; we MUST have some idea of
             * type to deserialize, aside from nominal PersistentXxx type.
             */
            return new PersistentCollectionSerializer(_figureFallbackType(config, type),
                    isEnabled(Feature.FORCE_LAZY_LOADING));
        }
        return null;
    }

    @Override
    public JsonSerializer<?> findMapSerializer(SerializationConfig config,
            MapType type, BeanDescription beanDesc,
            JsonSerializer<Object> keySerializer,
            TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer)
    {
        Class<?> raw = type.getRawClass();
        // 05-Jun-2012, tatu: PersistentMap DOES implement java.util.Map...
        if (PersistentMap.class.isAssignableFrom(raw)) {
            return new PersistentCollectionSerializer(_figureFallbackType(config, type),
                    isEnabled(Feature.FORCE_LAZY_LOADING));
        }
        return null;
    }
    
    public final boolean isEnabled(Hibernate4Module.Feature f) {
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
