package com.fasterxml.jackson.datatype.hibernate3;

import java.io.IOException;

import org.hibernate.collection.PersistentCollection;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.*;

/**
 * Wrapper serializer used to handle aspects of lazy loading that can be used
 * for Hibernate collection datatypes.
 */
public class PersistentCollectionSerializer
    extends JsonSerializer<PersistentCollection>
    implements ContextualSerializer
{
    protected final boolean _forceLazyLoading;
    
    /**
     * This is the nominal type used to locate actual serializer to use
     * for contents, if this collection is to be serialized.
     */
    protected final JavaType _serializationType;

    /**
     * Serializer to which we delegate if serialization is not blocked.
     */
    protected final JsonSerializer<Object> _serializer;

    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */
    
    public PersistentCollectionSerializer(JavaType type, boolean forceLazyLoading) {
        this(type, forceLazyLoading, null);
    }

    @SuppressWarnings("unchecked")
    public PersistentCollectionSerializer(JavaType type, boolean forceLazyLoading,
            JsonSerializer<?> serializer)
    {
        _serializationType = type;
        _forceLazyLoading = forceLazyLoading;
        _serializer = (JsonSerializer<Object>) serializer;
    }

    /**
     * We need to resolve actual serializer once we know the context; specifically
     * must know type of property being serialized.
     * If not known
     */
    public JsonSerializer<PersistentCollection> createContextual(SerializerProvider provider,
            BeanProperty property)
        throws JsonMappingException
    {
        /* If we have property, should be able to get actual polymorphic type
         * information.
         * May need to refine in future, in case nested types are used, since
         * 'property' refers to field/method and main type, but contents of
         * that type may also be resolved... in which case this would fail.
         */
        JsonSerializer<?> ser = provider.findValueSerializer(_serializationType, property);
        JavaType type = null;
        if (property != null) {
            type = property.getType();
        }
        return new PersistentCollectionSerializer(type, _forceLazyLoading, ser);
    }
    
    /*
    /**********************************************************************
    /* JsonSerializer impl
    /**********************************************************************
     */
    
    @Override
    public void serialize(PersistentCollection coll, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonProcessingException
    {
        // If lazy-loaded, not yet loaded, may serialize as null?
        if (!_forceLazyLoading && !coll.wasInitialized()) {
            provider.defaultSerializeNull(jgen);
            return;
        }
        Object value = coll.getValue();
        if (value == null) {
            provider.defaultSerializeNull(jgen);
        } else {
            if (_serializer == null) { // sanity check...
                throw new JsonMappingException("PersistentCollection does not have serializer set");
            }
            _serializer.serialize(value, jgen, provider);
        }
    }
    
    public void serializeWithType(PersistentCollection coll, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonProcessingException
    {
        if (!_forceLazyLoading && !coll.wasInitialized()) {
            provider.defaultSerializeNull(jgen);
            return;
        }
        Object value = coll.getValue();
        if (value == null) {
            provider.defaultSerializeNull(jgen);
        } else {
            if (_serializer == null) { // sanity check...
                throw new JsonMappingException("PersistentCollection does not have serializer set");
            }
            _serializer.serializeWithType(value, jgen, provider, typeSer);
        }
    }
}
