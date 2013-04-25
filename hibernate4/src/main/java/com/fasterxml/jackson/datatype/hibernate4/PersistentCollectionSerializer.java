package com.fasterxml.jackson.datatype.hibernate4;

import java.io.IOException;

import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.hibernate.collection.spi.PersistentCollection;


import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.*;

/**
 * Wrapper serializer used to handle aspects of lazy loading that can be used
 * for Hibernate collection datatypes; which includes both <code>Collection</code>
 * and <code>Map</code> types (unlike in JDK).
 */
public class PersistentCollectionSerializer
    extends JsonSerializer<Object>
    implements ContextualSerializer
{
    /**
     * Whether loading of values is forced for lazy references.
     */
    protected final boolean _forceLazyLoading;

    /**
     * Serializer that does actual value serialization when value
     * is available (either already or with forced access).
     */
    protected final JsonSerializer<Object> _serializer;

    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */

    @SuppressWarnings("unchecked")
    public PersistentCollectionSerializer(boolean forceLazyLoading,
            JsonSerializer<?> serializer)
    {
        _forceLazyLoading = forceLazyLoading;
        _serializer = (JsonSerializer<Object>) serializer;
    }

    /**
     * We need to resolve actual serializer once we know the context; specifically
     * must know type of property being serialized.
     * If not known
     */
    @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider,
            BeanProperty property)
        throws JsonMappingException
    {
        // If we use eager loading, or force it, can just return underlying serializer as is
        if (_forceLazyLoading || !usesLazyLoading(property)) {
            if (_serializer instanceof ContextualSerializer) {
                return ((ContextualSerializer) _serializer).createContextual(provider, property);
            }
            return _serializer;
        }
        // Otherwise this instance is to be used
        return this;
    }
    
    /*
    /**********************************************************************
    /* JsonSerializer impl
    /**********************************************************************
     */
    
    @Override
    public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonProcessingException
    {
        if (value instanceof PersistentCollection) {
            PersistentCollection coll = (PersistentCollection) value;
            // If lazy-loaded, not yet loaded, may serialize as null?
            if (!_forceLazyLoading && !coll.wasInitialized()) {
                provider.defaultSerializeNull(jgen);
                return;
            }
            value = coll.getValue();
            if (value == null) {
                provider.defaultSerializeNull(jgen);
                return;
            }
        }
        if (_serializer == null) { // sanity check...
            throw new JsonMappingException("PersistentCollection does not have serializer set");
        }
        _serializer.serialize(value, jgen, provider);
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonProcessingException
    {
        if (value instanceof PersistentCollection) {
            PersistentCollection coll = (PersistentCollection) value;
            if (!_forceLazyLoading && !coll.wasInitialized()) {
                provider.defaultSerializeNull(jgen);
                return;
            }
            value = coll.getValue();
            if (value == null) {
                provider.defaultSerializeNull(jgen);
                return;
            }
        }
        if (_serializer == null) { // sanity check...
            throw new JsonMappingException("PersistentCollection does not have serializer set");
        }
        _serializer.serializeWithType(value, jgen, provider, typeSer);
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */
    
    /**
     * Method called to see whether given property indicates it uses lazy
     * resolution of reference contained.
     */
    protected boolean usesLazyLoading(BeanProperty property)
    {
        if (property != null) {
            OneToMany ann1 = property.getAnnotation(OneToMany.class);
            if (ann1 != null) {
                return (ann1.fetch() == FetchType.LAZY);
            }
            OneToOne ann2 = property.getAnnotation(OneToOne.class);
            if (ann2 != null) {
                return (ann2.fetch() == FetchType.LAZY);
            }
            ManyToOne ann3 = property.getAnnotation(ManyToOne.class);
            if (ann3 != null) {
                return (ann3.fetch() == FetchType.LAZY);
            }
            ManyToMany ann4 = property.getAnnotation(ManyToMany.class);
            if (ann4 != null) {
                return (ann4.fetch() == FetchType.LAZY);
            }
        }
        return false;
    }
}
