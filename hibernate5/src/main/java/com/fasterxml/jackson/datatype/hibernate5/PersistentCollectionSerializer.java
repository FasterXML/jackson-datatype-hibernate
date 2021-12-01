package com.fasterxml.jackson.datatype.hibernate5;

import java.io.IOException;
import java.util.*;

import javax.persistence.*;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;
import com.fasterxml.jackson.databind.util.NameTransformer;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module.Feature;

import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.mapping.Bag;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorImpl;

/**
 * Wrapper serializer used to handle aspects of lazy loading that can be used
 * for Hibernate collection datatypes; which includes both <code>Collection</code>
 * and <code>Map</code> types (unlike in JDK).
 */
public class PersistentCollectionSerializer
    extends ContainerSerializer<Object>
    implements ContextualSerializer, ResolvableSerializer
{
    private static final long serialVersionUID = 1L; // since 2.7

    /**
     * Type for which underlying serializer was created.
     *
     * @since 2.7
     */
    protected final JavaType _originalType;

    /**
     * Hibernate-module features set, if any.
     */
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
    public PersistentCollectionSerializer(JavaType containerType,
            JsonSerializer<?> serializer, int features, SessionFactory sessionFactory) {
        super(containerType);
        _originalType = containerType;
        _serializer = (JsonSerializer<Object>) serializer;
        _features = features;
        _sessionFactory = sessionFactory;
    }

    /**
     * @since 2.7
     */
    @SuppressWarnings("unchecked")
    protected PersistentCollectionSerializer(PersistentCollectionSerializer base, JsonSerializer<?> serializer)
    {
        super(base);
        _originalType = base._originalType;
        _serializer = (JsonSerializer<Object>) serializer;
        _features = base._features;
        _sessionFactory = base._sessionFactory;
    }

    @Override
    public PersistentCollectionSerializer unwrappingSerializer(NameTransformer unwrapper) {
        return _withSerializer(_serializer.unwrappingSerializer(unwrapper));
    }

    protected PersistentCollectionSerializer _withSerializer(JsonSerializer<?> ser) {
        if ((ser == _serializer) || (ser == null)) {
            return this;
        }
        return new PersistentCollectionSerializer(this, ser);
    }

    // from `ContainerSerializer`
    @Override
    protected ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts)
    {
        ContainerSerializer<?> ser0 = _containerSerializer();
        if (ser0 != null) {
            return _withSerializer(ser0.withValueTypeSerializer(vts));
        }
        // 03-Jan-2016, tatu: Not sure what to do here; most likely can not make it work without
        //    knowing how to pass various calls... so in a way, should limit to only accepting
        //    ContainerSerializers as delegates.
        return this;
    }

    /*
    /**********************************************************************
    /* Contextualization
    /**********************************************************************
     */

    @Override
    public void resolve(SerializerProvider provider) throws JsonMappingException
    {
        if (_serializer instanceof ResolvableSerializer) {
            ((ResolvableSerializer) _serializer).resolve(provider);
        }
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
        return _withSerializer(ser);
    }

    /*
    /**********************************************************************
    /* JsonSerializer simple accessors, metadata
    /**********************************************************************
     */

    @Override // since 2.6
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
    public boolean isUnwrappingSerializer() {
        return _serializer.isUnwrappingSerializer();
    }

    @Override
    public boolean usesObjectId() {
        return _serializer.usesObjectId();
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        throws JsonMappingException
    {
        _serializer.acceptJsonFormatVisitor(visitor, typeHint);
    }

    /*
    /**********************************************************************
    /* ContainerSerializer methods
    /**********************************************************************
     */

    @Override
    public JavaType getContentType() {
        ContainerSerializer<?> ser = _containerSerializer();
        if (ser != null) {
            return ser.getContentType();
        }
        return _originalType.getContentType();
    }

    @Override
    public JsonSerializer<?> getContentSerializer() {
        ContainerSerializer<?> ser = _containerSerializer();
        if (ser != null) {
            return ser.getContentSerializer();
        }
        // no idea, alas
        return null;
    }

    @Override
    public boolean hasSingleElement(Object value) {
        if (value instanceof Collection<?>) {
            return ((Collection<?>) value).size() == 1;
        }
        if (value instanceof Map<?,?>) {
            return ((Map<?,?>) value).size() == 1;
        }
        return false;
    }

    /*
    /**********************************************************************
    /* JsonSerializer, actual serialization
    /**********************************************************************
     */

    @Override
    public void serialize(Object value, JsonGenerator g, SerializerProvider provider)
        throws IOException
    {
        if (value instanceof PersistentCollection) {
            value = findLazyValue((PersistentCollection) value);
            if (value == null) {
                provider.defaultSerializeNull(g);
                return;
            }
        }
        if (_serializer == null) { // sanity check...
            throw JsonMappingException.from(g, "PersistentCollection does not have serializer set");
        }

        // 30-Jul-2016, tatu: wrt [datatype-hibernate#93], should NOT have to do anything here;
        //     only affects polymophic cases
        _serializer.serialize(value, g, provider);
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator g, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException
    {
        if (value instanceof PersistentCollection) {
            value = findLazyValue((PersistentCollection) value);
            if (value == null) {
                provider.defaultSerializeNull(g);
                return;
            }
        }
        if (_serializer == null) { // sanity check...
            throw JsonMappingException.from(g, "PersistentCollection does not have serializer set");
        }

        // 30-Jul-2016, tatu: wrt [datatype-hibernate#93], conversion IS needed here (or,
        //    if we could figure out, type id)

        // !!! TODO: figure out how to replace type id without having to replace collection
        if (Feature.REPLACE_PERSISTENT_COLLECTIONS.enabledIn(_features)) {
            value = convertToJavaCollection(value); // Strip PersistentCollection
        }
        _serializer.serializeWithType(value, g, provider, typeSer);
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    protected ContainerSerializer<?> _containerSerializer() {
        if (_serializer instanceof ContainerSerializer) {
            return (ContainerSerializer<?>) _serializer;
        }
        return null;
    }

    protected Object findLazyValue(PersistentCollection coll) {
        // If lazy-loaded, not yet loaded, may serialize as null?
        if (!Feature.FORCE_LAZY_LOADING.enabledIn(_features) && !coll.wasInitialized()) {
            return null;
        }
        if (_sessionFactory != null) {
            // 08-Feb-2017, tatu: and not closing this is not problematic... ?
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

//        boolean isJTA = ((SessionImplementor) session).getTransactionCoordinator()
//                .getTransactionContext().getTransactionEnvironment()
//                .getTransactionFactory()
//                .compatibleWithJtaSynchronization();
        //Above is removed after Hibernate 5
        boolean isJTA = SessionReader.isJTA(session);

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

            boolean replaceCollection = Hibernate5Module.Feature.REPLACE_PERSISTENT_COLLECTIONS.enabledIn(_features);
            // As per [Issue#36]
            ElementCollection ec = property.getAnnotation(ElementCollection.class);
            if (ec != null) {
                return replaceCollection || (ec.fetch() == FetchType.LAZY);
            }
            OneToMany ann1 = property.getAnnotation(OneToMany.class);
            if (ann1 != null) {
                return replaceCollection || (ann1.fetch() == FetchType.LAZY);
            }
            OneToOne ann2 = property.getAnnotation(OneToOne.class);
            if (ann2 != null) {
                return replaceCollection || (ann2.fetch() == FetchType.LAZY);
            }
            ManyToOne ann3 = property.getAnnotation(ManyToOne.class);
            if (ann3 != null) {
                return replaceCollection || (ann3.fetch() == FetchType.LAZY);
            }
            ManyToMany ann4 = property.getAnnotation(ManyToMany.class);
            if (ann4 != null) {
                return replaceCollection || (ann4.fetch() == FetchType.LAZY);
            }
            // As per [Issue#53]
            return !Hibernate5Module.Feature.REQUIRE_EXPLICIT_LAZY_LOADING_MARKER.enabledIn(_features);
        }
        return false;
    }

    // since 2.8.2
    private Object convertToJavaCollection(Object value) {
        if (!(value instanceof PersistentCollection)) {
            return value;
        }

        if (value instanceof Set) {
            return convertToSet((Set<?>) value);
        }

        if (value instanceof List || value instanceof Bag) {
            return convertToList((List<?>) value);
        }

        if (value instanceof Map) {
            return convertToMap((Map<?, ?>) value);
        }

        throw new IllegalArgumentException("Unsupported PersistentCollection subtype: " + value.getClass());
    }

    private Object convertToList(List<?> value) {
        return new ArrayList<>(value);
    }

    private Object convertToMap(Map<?, ?> value) {
        return new HashMap<>(value);
    }

    private Object convertToSet(Set<?> value) {
        return new HashSet<>(value);
    }

    protected static class SessionReader
    {
        public static boolean isJTA(Session session)
        {
            if (session instanceof EntityManager) {
                try {
                    session.getTransaction();
                    return false;
                } catch (final IllegalStateException e) {
                    // EntityManager is required to throw an IllegalStateException if it's JTA-managed
                    return true;
                }
            }
            if (session instanceof SessionImplementor) {
                // 23-Aug-2018, tatu: Unfortunately, Hibernate ORM has a pretty severe backwards-compatibility
                //    breakage between 5.1 and 5.2, due to move of `TransactionCoordinator` being moved to
                //    different package. As such, we can not cast it... and it's unclear if even calling the
                //    method directly is kosher.
                final Object transactionCoordinator = ((SessionImplementor) session).getTransactionCoordinator();
                return (transactionCoordinator instanceof JtaTransactionCoordinatorImpl);
            }
            // If in doubt, do without (transaction)
            return true;
        }
    }
}
