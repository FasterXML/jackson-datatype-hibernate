package tools.jackson.datatype.hibernate4;

import java.util.*;

import javax.persistence.*;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdContainerSerializer;
import tools.jackson.databind.util.NameTransformer;
import tools.jackson.datatype.hibernate4.Hibernate4Module.Feature;

import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.mapping.Bag;

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
    /* ValueSerializer, actual serialization
    /**********************************************************************
     */

    @Override
    public void serialize(Object value, JsonGenerator jgen, SerializationContext provider)
    {
        if (value instanceof PersistentCollection) {
            value = findLazyValue((PersistentCollection) value);
            if (value == null) {
                provider.defaultSerializeNullValue(jgen);
                return;
            }
        }
        if (_serializer == null) { // sanity check...
            throw DatabindException.from(jgen, "PersistentCollection does not have serializer set");
        }

        if (Feature.REPLACE_PERSISTENT_COLLECTIONS.enabledIn(_features)) {
            value = convertToJavaCollection(value); // Strip PersistentCollection
        }

        _serializer.serialize(value, jgen, provider);
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator jgen, SerializationContext provider,
            TypeSerializer typeSer)
    {
        if (value instanceof PersistentCollection) {
            value = findLazyValue((PersistentCollection) value);
            if (value == null) {
                provider.defaultSerializeNullValue(jgen);
                return;
            }
        }
        if (_serializer == null) { // sanity check...
            throw DatabindException.from(jgen, "PersistentCollection does not have serializer set");
        }

        if (Feature.REPLACE_PERSISTENT_COLLECTIONS.enabledIn(_features)) {
            value = convertToJavaCollection(value); // Strip PersistentCollection
        }

        _serializer.serializeWithType(value, jgen, provider, typeSer);
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

    private Object convertToJavaCollection(Object value) {
        if (!(value instanceof PersistentCollection)) {
            return value;
        }

        if (value instanceof Set) {
            return convertToSet((Set<?>) value);
        }

        if (value instanceof List
                || value instanceof Bag
                ) {
            return convertToList((List<?>) value);
        }

        if (value instanceof Map) {
            return convertToMap((Map<?, ?>) value);
        }

        throw new IllegalArgumentException("Unsupported type: " + value.getClass());
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
}
