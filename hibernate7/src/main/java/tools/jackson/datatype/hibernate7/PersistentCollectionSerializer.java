package tools.jackson.datatype.hibernate7;

import java.util.*;

import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorImpl;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdContainerSerializer;
import tools.jackson.databind.util.NameTransformer;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

/**
 * Wrapper serializer used to handle aspects of lazy loading that can be used
 * for Hibernate collection datatypes; which includes both <code>Collection</code>
 * and <code>Map</code> types (unlike in JDK).
 */
public class PersistentCollectionSerializer
    extends StdContainerSerializer<Object>
{
    /**
     * Type for which underlying serializer was created.
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
    protected final ValueSerializer<Object> _serializer;

    protected final SessionFactory _sessionFactory;

    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */

    @SuppressWarnings("unchecked")
    public PersistentCollectionSerializer(JavaType containerType,
            ValueSerializer<?> serializer, int features, SessionFactory sessionFactory) {
        super(containerType, null);
        _originalType = containerType;
        _serializer = (ValueSerializer<Object>) serializer;
        _features = features;
        _sessionFactory = sessionFactory;
    }

    @SuppressWarnings("unchecked")
    protected PersistentCollectionSerializer(PersistentCollectionSerializer base, ValueSerializer<?> serializer)
    {
        super(base);
        _originalType = base._originalType;
        _serializer = (ValueSerializer<Object>) serializer;
        _features = base._features;
        _sessionFactory = base._sessionFactory;
    }

    @Override
    public PersistentCollectionSerializer unwrappingSerializer(NameTransformer unwrapper) {
        return _withSerializer(_serializer.unwrappingSerializer(unwrapper));
    }

    protected PersistentCollectionSerializer _withSerializer(ValueSerializer<?> ser) {
        if ((ser == _serializer) || (ser == null)) {
            return this;
        }
        return new PersistentCollectionSerializer(this, ser);
    }

    // from `ContainerSerializer`
    @Override
    protected StdContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts)
    {
        StdContainerSerializer<?> ser0 = _containerSerializer();
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
    public void resolve(SerializationContext provider) throws DatabindException
    {
        _serializer.resolve(provider);
    }

    /**
     * We need to resolve actual serializer once we know the context; specifically
     * must know type of property being serialized.
     */
    @Override
    public ValueSerializer<?> createContextual(SerializationContext provider,
            BeanProperty property)
        throws DatabindException
    {
        // 18-Oct-2013, tatu: Whether this is for the primary property or secondary is
        //   not quite certain; presume primary one for now.
        ValueSerializer<?> ser = provider.handlePrimaryContextualization(_serializer, property);

        // If we use eager loading, can just return underlying serializer as is
        if (!usesLazyLoading(property)) {
            return ser;
        }
        return _withSerializer(ser);
    }

    /*
    /**********************************************************************
    /* ValueSerializer simple accessors, metadata
    /**********************************************************************
     */

    @Override
    public boolean isEmpty(SerializationContext provider, Object value)
    {
        if (value == null) { // is null ever passed?
            return true;
        }
        if (value instanceof PersistentCollection pc) {
            Object lazy = findLazyValue(pc);
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
        throws DatabindException
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
        StdContainerSerializer<?> ser = _containerSerializer();
        if (ser != null) {
            return ser.getContentType();
        }
        return _originalType.getContentType();
    }

    @Override
    public ValueSerializer<?> getContentSerializer() {
        StdContainerSerializer<?> ser = _containerSerializer();
        if (ser != null) {
            return ser.getContentSerializer();
        }
        // no idea, alas
        return null;
    }

    @Override
    public boolean hasSingleElement(Object value) {
        if (value instanceof Collection<?> c) {
            return c.size() == 1;
        }
        if (value instanceof Map<?,?> m) {
            return m.size() == 1;
        }
        return false;
    }


    /*
    /**********************************************************************
    /* ValueSerializer, actual serialization
    /**********************************************************************
     */

    @Override
    public void serialize(Object value, JsonGenerator g, SerializationContext provider)
    {
        if (value instanceof PersistentCollection pc) {
            value = findLazyValue(pc);
            if (value == null) {
                provider.defaultSerializeNullValue(g);
                return;
            }
        }
        if (_serializer == null) { // sanity check...
            throw DatabindException.from(g, "PersistentCollection does not have serializer set");
        }

        // 30-Jul-2016, tatu: wrt [datatype-hibernate#93], should NOT have to do anything here;
        //     only affects polymophic cases
        _serializer.serialize(value, g, provider);
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator g, SerializationContext provider,
            TypeSerializer typeSer)
    {
        if (value instanceof PersistentCollection pc) {
            value = findLazyValue(pc);
            if (value == null) {
                provider.defaultSerializeNullValue(g);
                return;
            }
        }
        if (_serializer == null) { // sanity check...
            throw DatabindException.from(g, "PersistentCollection does not have serializer set");
        }

        // 30-Jul-2016, tatu: wrt [datatype-hibernate#93], conversion IS needed here (or,
        //    if we could figure out, type id)

        // !!! TODO: figure out how to replace type id without having to replace collection
        if (Hibernate7Module.Feature.REPLACE_PERSISTENT_COLLECTIONS.enabledIn(_features)) {
            value = convertToJavaCollection(value); // Strip PersistentCollection
        }
        _serializer.serializeWithType(value, g, provider, typeSer);
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    protected StdContainerSerializer<?> _containerSerializer() {
        if (_serializer instanceof StdContainerSerializer stdContainerSerializer) {
            return stdContainerSerializer;
        }
        return null;
    }

    protected Object findLazyValue(PersistentCollection coll) {
        // If lazy-loaded, not yet loaded, may serialize as null?
        if (!Hibernate7Module.Feature.FORCE_LAZY_LOADING.enabledIn(_features) && !coll.wasInitialized()) {
            return null;
        }
        // Only open a temporary session when the collection actually needs it:
        // an already loaded collection would otherwise cost a JDBC connection
        // and a transaction for a no-op initialization.
        if (_sessionFactory != null && !coll.wasInitialized()) {
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

        try {
            PersistenceContext persistenceContext = ((SessionImplementor) session).getPersistenceContext();
            persistenceContext.setDefaultReadOnly(true);
            session.setHibernateFlushMode(FlushMode.MANUAL);

            persistenceContext.addUninitializedDetachedCollection(
                    ((SessionFactoryImplementor) _sessionFactory).getMappingMetamodel().getCollectionDescriptor(coll.getRole()),
                    coll
            );

            return session;
        } catch (RuntimeException e) {
            // Setup after openSession() can fail -- an unknown collection role, or a
            // SessionFactory that is not a SessionFactoryImplementor -- and the session
            // is already open by then, so it would leak without this.
            closeQuietly(session, e);
            throw e;
        }
    }

    private void initializeCollection(PersistentCollection coll, Session session) {

//        boolean isJTA = ((SessionImplementor) session).getTransactionCoordinator()
//                .getTransactionContext().getTransactionEnvironment()
//                .getTransactionFactory()
//                .compatibleWithJtaSynchronization();
        //Above is removed after Hibernate 5
        boolean isJTA = false;
        RuntimeException failure = null;

        try {
            isJTA = SessionReader.isJTA(session);

            if (!isJTA) {
                session.beginTransaction();
            }

            coll.setCurrentSession(((SessionImplementor) session));
            Hibernate.initialize(coll);

            if (!isJTA) {
                session.getTransaction().commit();
            }
        } catch (RuntimeException e) {
            failure = e;
            if (!isJTA) {
                rollbackQuietly(session, e);
            }
            throw e;
        } finally {
            // Always close, even when initialization failed: otherwise the temporary
            // session and its JDBC connection leak.
            closeQuietly(session, failure);
        }
    }

    /**
     * Rolls back the temporary transaction if one is still active, without letting a
     * secondary failure hide the one that actually broke initialization.
     */
    private void rollbackQuietly(Session session, RuntimeException failure) {
        try {
            Transaction tx = session.getTransaction();
            if ((tx != null) && tx.isActive()) {
                tx.rollback();
            }
        } catch (RuntimeException e) {
            failure.addSuppressed(e);
        }
    }

    /**
     * Closes the temporary session without letting a close failure hide the exception
     * that actually broke initialization: a {@code close()} that throws from a
     * {@code finally} block would otherwise discard the pending exception entirely.
     */
    private void closeQuietly(Session session, RuntimeException failure) {
        try {
            session.close();
        } catch (RuntimeException e) {
            if (failure == null) {
                throw e;
            }
            failure.addSuppressed(e);
        }
    }

    /**
     * Method called to see whether given property indicates it uses lazy
     * resolution of reference contained.
     */
    protected boolean usesLazyLoading(BeanProperty property) {
        if (property != null) {
            boolean replaceCollection = Hibernate7Module.Feature.REPLACE_PERSISTENT_COLLECTIONS.enabledIn(_features);
            // As per [datatype-hibernate#36]
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
            // As per [datatype-hibernate#53]
            return !Hibernate7Module.Feature.REQUIRE_EXPLICIT_LAZY_LOADING_MARKER.enabledIn(_features);
        }
        return false;
    }

    // since 2.8.2
    private Object convertToJavaCollection(Object value) {
        if (!(value instanceof PersistentCollection)) {
            return value;
        }

        if (value instanceof Set<?> set) {
            return convertToSet(set);
        }

        if (value instanceof List<?> list) {
            return convertToList(list);
        }

        if (value instanceof Map<?, ?> map) {
            return convertToMap(map);
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
