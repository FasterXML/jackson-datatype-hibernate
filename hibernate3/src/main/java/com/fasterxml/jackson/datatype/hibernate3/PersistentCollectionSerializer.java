package com.fasterxml.jackson.datatype.hibernate3;

import java.io.IOException;

import javax.persistence.*;

import org.hibernate.collection.PersistentCollection;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.*;
import com.fasterxml.jackson.datatype.hibernate3.Hibernate3Module.Feature;

/**
 * Wrapper serializer used to handle aspects of lazy loading that can be used
 * for Hibernate collection datatypes; which includes both <code>Collection</code>
 * and <code>Map</code> types (unlike in JDK).
 */
public class PersistentCollectionSerializer
    extends JsonSerializer<Object>
    implements ContextualSerializer
{
    protected final int _features;

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
    public PersistentCollectionSerializer(JsonSerializer<?> serializer, int features)
    {
        _serializer = (JsonSerializer<Object>) serializer;
        _features = features;
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
        /* 18-Oct-2013, tatu: Whether this is for the primary property or secondary is
         *   not quite certain; presume primary one for now.
         */
        JsonSerializer<?> ser = provider.handlePrimaryContextualization(_serializer, property);
        
        // If we use eager loading, or force it, can just return underlying serializer as is
        if (Feature.FORCE_LAZY_LOADING.enabledIn(_features) || !usesLazyLoading(property)) {
            return ser;
        }
        if (ser != _serializer) {
            return new PersistentCollectionSerializer(ser, _features);
        }
        return this;
    }
    
    /*
    /**********************************************************************
    /* JsonSerializer impl
    /**********************************************************************
     */

    // since 2.3
    @Override
    public boolean isEmpty(Object value)
    {
        if (value == null) { // is null ever passed?
            return true;
        }
        if (value instanceof PersistentCollection) {
            return findLazyValue((PersistentCollection) value) == null;
        }
        return _serializer.isEmpty(value);
    }
    
    @Override
    public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException
    {
        if (value instanceof PersistentCollection) {
            PersistentCollection coll = (PersistentCollection) value;
            // If lazy-loaded, not yet loaded, may serialize as null?
            if (!Feature.FORCE_LAZY_LOADING.enabledIn(_features) && !coll.wasInitialized()) {
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
        throws IOException
    {
        if (value instanceof PersistentCollection) {
            PersistentCollection coll = (PersistentCollection) value;
            if (!Feature.FORCE_LAZY_LOADING.enabledIn(_features) && !coll.wasInitialized()) {
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

    protected Object findLazyValue(PersistentCollection coll)
    {
        // If lazy-loaded, not yet loaded, may serialize as null?
        if (!Feature.FORCE_LAZY_LOADING.enabledIn(_features) && !coll.wasInitialized()) {
            return null;
        }
        return coll.getValue();
    }
    
    /**
     * Method called to see whether given property indicates it uses lazy
     * resolution of reference contained.
     */
    protected boolean usesLazyLoading(BeanProperty property)
    {
        if (property != null) {
            // As per [Issue#36]
            ElementCollection ec = property.getAnnotation(ElementCollection.class);
            if (ec != null) {
                return (ec.fetch() == FetchType.LAZY);
            }
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
            // As per [Issue#53]
            return !Feature.REQUIRE_EXPLICIT_LAZY_LOADING_MARKER.enabledIn(_features);
        }
        return false;
    }
}
