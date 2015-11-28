package com.fasterxml.jackson.datatype.hibernate4;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.datatype.hibernate4.Hibernate4Module.Feature;

import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;

import javax.persistence.*;

import java.io.IOException;

/**
 * Wrapper serializer used to handle aspects of lazy loading that can be used
 * for Hibernate collection datatypes; which includes both <code>Collection</code>
 * and <code>Map</code> types (unlike in JDK).
 */
public class PersistentCollectionSerializer
        extends JsonSerializer<Object>
        implements ContextualSerializer {
    protected final int _features;

    /**
     * Serializer that does actual value serialization when value
     * is available (either already or with forced access).
     */
    protected final JsonSerializer<Object> _serializer;

    protected final SessionFactory _sessionFactory;

    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */

    @SuppressWarnings("unchecked")
    public PersistentCollectionSerializer(JsonSerializer<?> serializer, int features, SessionFactory sessionFactory) {
        _serializer = (JsonSerializer<Object>) serializer;
        _features = features;
        _sessionFactory = sessionFactory;
    }

    /**
     * We need to resolve actual serializer once we know the context; specifically
     * must know type of property being serialized.
     */
    @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider,
            BeanProperty property)
        throws JsonMappingException
    {
        // 18-Oct-2013, tatu: Whether this is for the primary property or secondary is
        //   not quite certain; presume primary one for now.
        JsonSerializer<?> ser = provider.handlePrimaryContextualization(_serializer, property);

        // If we use eager loading, can just return underlying serializer as is
        if (!usesLazyLoading(property)) {
            return ser;
        }
        if (ser != _serializer) {
            return new PersistentCollectionSerializer(ser, _features, _sessionFactory);
        }
        return this;
    }

    /*
    /**********************************************************************
    /* JsonSerializer impl
    /**********************************************************************
     */

    // since 2.3
    @Deprecated // since 2.5
    @Override
    public boolean isEmpty(Object value) {
        if (value == null) { // is null ever passed?
            return true;
        }
        if (value instanceof PersistentCollection) {
            Object lazy = findLazyValue((PersistentCollection) value);
            return (lazy == null) || _serializer.isEmpty(lazy);
        }
        return _serializer.isEmpty(value);
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, Object value)
    {
        if (value == null) { // is null ever passed?
            return true;
        }
        if (value instanceof PersistentCollection) {
            Object lazy = findLazyValue((PersistentCollection) value);
            return (lazy == null) || _serializer.isEmpty(provider, lazy);
        }
        return _serializer.isEmpty(provider, value);
    }
    
    @Override
    public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException
    {
        if (value instanceof PersistentCollection) {
            value = findLazyValue((PersistentCollection) value);
            if (value == null) {
                provider.defaultSerializeNull(jgen);
                return;
            }
        }
        if (_serializer == null) { // sanity check...
            throw JsonMappingException.from(jgen, "PersistentCollection does not have serializer set");
        }
        _serializer.serialize(value, jgen, provider);
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException
    {
        if (value instanceof PersistentCollection) {
            value = findLazyValue((PersistentCollection) value);
            if (value == null) {
                provider.defaultSerializeNull(jgen);
                return;
            }
        }
        if (_serializer == null) { // sanity check...
            throw JsonMappingException.from(jgen, "PersistentCollection does not have serializer set");
        }
        _serializer.serializeWithType(value, jgen, provider, typeSer);
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    protected Object findLazyValue(PersistentCollection coll) {
        // If lazy-loaded, not yet loaded, may serialize as null?
        if (!Feature.FORCE_LAZY_LOADING.enabledIn(_features) && !coll.wasInitialized()) {
            return null;
        }

        if(_sessionFactory != null) {
            Session session = openTemporarySessionForLoading(coll);
            initializeCollection(coll, session);
        }

        return coll.getValue();
    }

    // Most of the code bellow is from Hibernate AbstractPersistentCollection
    private Session openTemporarySessionForLoading(PersistentCollection coll) {

        final SessionFactory sf = _sessionFactory;
        final Session session = sf.openSession();

        PersistenceContext persistenceContext = ((SessionImplementor) session).getPersistenceContext();
        persistenceContext.setDefaultReadOnly(true);
        session.setFlushMode(FlushMode.MANUAL);

        persistenceContext.addUninitializedDetachedCollection(
                ((SessionFactoryImplementor) _sessionFactory).getCollectionPersister(coll.getRole()),
                coll
        );

        return session;
    }

    private void initializeCollection(PersistentCollection coll, Session session) {

        boolean isJTA = ((SessionImplementor) session).getTransactionCoordinator()
                .getTransactionContext().getTransactionEnvironment()
                .getTransactionFactory()
                .compatibleWithJtaSynchronization();

        if (!isJTA) {
            session.beginTransaction();
        }

        coll.setCurrentSession(((SessionImplementor) session));
        Hibernate.initialize(coll);

        if (!isJTA) {
            session.getTransaction().commit();
        }
        session.close();
    }

    /**
     * Method called to see whether given property indicates it uses lazy
     * resolution of reference contained.
     */
    protected boolean usesLazyLoading(BeanProperty property) {
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
