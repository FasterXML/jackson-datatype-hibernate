package tools.jackson.datatype.hibernate5;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;

import javax.persistence.EntityNotFoundException;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.impl.PropertySerializerMap;
import tools.jackson.databind.util.NameTransformer;

import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.proxy.pojo.BasicLazyInitializer;

/**
 * Serializer to use for values proxied using {@link org.hibernate.proxy.HibernateProxy}.
 *<p>
 * TODO: should try to make this work more like Jackson
 * {@code BeanPropertyWriter}, possibly sub-classing
 * it -- it handles much of functionality we need, and has
 * access to more information than value serializers (like
 * this one) have.
 */
public class HibernateProxySerializer
    extends ValueSerializer<HibernateProxy>
{
    /**
     * Property that has proxy value to handle
     */
    protected final BeanProperty _property;

    protected final boolean _forceLazyLoading;
    protected final boolean _serializeIdentifier;
    protected final boolean _nullMissingEntities;
    protected final boolean _wrappedIdentifier;
    protected final Mapping _mapping;

    // @since 2.11.2 (datatype-hibernate#97)
    protected final NameTransformer _unwrapper;

    /**
     * For efficient serializer lookup, let's use this; most
     * of the time, there's just one type and one serializer.
     */
    protected PropertySerializerMap _dynamicSerializers;
    
    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */

    @Deprecated // since 2.12
    public HibernateProxySerializer(boolean forceLazyLoading)
    {
        this(forceLazyLoading, false, false, true, null, null, null);
    }

    @Deprecated // since 2.12
    public HibernateProxySerializer(boolean forceLazyLoading, boolean serializeIdentifier) {
        this(forceLazyLoading, serializeIdentifier, false, true,
                null, null, null);
    }

    @Deprecated // since 2.12
    public HibernateProxySerializer(boolean forceLazyLoading, boolean serializeIdentifier, Mapping mapping) {
        this(forceLazyLoading, serializeIdentifier, false,  true,
                mapping, null, null);
    }

    @Deprecated // since 2.12
    public HibernateProxySerializer(boolean forceLazyLoading, boolean serializeIdentifier,
            boolean nullMissingEntities, Mapping mapping) {
        this(forceLazyLoading, serializeIdentifier, nullMissingEntities, true,
                mapping, null, null);
    }

    @Deprecated // since 2.12
    public HibernateProxySerializer(boolean forceLazyLoading, boolean serializeIdentifier,
            boolean nullMissingEntities, Mapping mapping, BeanProperty property) {
        this(forceLazyLoading, serializeIdentifier, nullMissingEntities, true,
                mapping, property, null);
    }

    /**
     * @since 2.12
     */
    public HibernateProxySerializer(boolean forceLazyLoading, boolean serializeIdentifier,
            boolean nullMissingEntities, boolean wrappedIdentifier,
            Mapping mapping)
    {
        this(forceLazyLoading, serializeIdentifier, nullMissingEntities, wrappedIdentifier,
                mapping, null, null);
    }

    /**
     * @since 2.12
     */
    public HibernateProxySerializer(boolean forceLazyLoading, boolean serializeIdentifier,
            boolean nullMissingEntities, boolean wrappedIdentifier,
            Mapping mapping, BeanProperty property, NameTransformer unwrapper)
    {
        _forceLazyLoading = forceLazyLoading;
        _serializeIdentifier = serializeIdentifier;
        _nullMissingEntities = nullMissingEntities;
        _wrappedIdentifier = wrappedIdentifier;
        _mapping = mapping;
        _property = property;
        _unwrapper = unwrapper;

        _dynamicSerializers = PropertySerializerMap.emptyForProperties();
    }

    /**
     * @since 2.12
     */
    protected HibernateProxySerializer(HibernateProxySerializer base,
            BeanProperty property, NameTransformer unwrapper)
    {
        _forceLazyLoading = base._forceLazyLoading;
        _serializeIdentifier = base._serializeIdentifier;
        _nullMissingEntities = base._nullMissingEntities;
        _wrappedIdentifier = base._wrappedIdentifier;
        _mapping = base._mapping;
        _property = property;
        _unwrapper = unwrapper;

        _dynamicSerializers = PropertySerializerMap.emptyForProperties();
    }
    
    @Override
    public ValueSerializer<?> createContextual(SerializationContext prov, BeanProperty property) {
        return new HibernateProxySerializer(this, property, _unwrapper);
    }

    @Override
    public ValueSerializer<HibernateProxy> unwrappingSerializer(final NameTransformer unwrapper) {
        return new HibernateProxySerializer(this, _property, unwrapper);
    }

    @Override
    public boolean isUnwrappingSerializer()
    {
        return _unwrapper != null;
    }

    /*
    /**********************************************************************
    /* ValueSerializer impl
    /**********************************************************************
     */

    @Override
    public boolean isEmpty(SerializationContext provider, HibernateProxy value) {
        return (value == null) || (findProxied(value) == null);
    }
    
    @Override
    public void serialize(HibernateProxy value, JsonGenerator g, SerializationContext provider)
    {
        Object proxiedValue = findProxied(value);
        // TODO: figure out how to suppress nulls, if necessary? (too late for that here)
        if (proxiedValue == null) {
            provider.defaultSerializeNullValue(g);
            return;
        }
        findSerializer(provider, proxiedValue).serialize(proxiedValue, g, provider);
    }

    @Override
    public void serializeWithType(HibernateProxy value, JsonGenerator g, SerializationContext provider,
            TypeSerializer typeSer)
    {
        Object proxiedValue = findProxied(value);
        if (proxiedValue == null) {
            provider.defaultSerializeNullValue(g);
            return;
        }
        /* This isn't exactly right, since type serializer really refers to proxy
         * object, not value. And we really don't either know static type (necessary
         * to know how to apply additional type info) or other things;
         * so it's not going to work well. But... we'll do out best.
         */
        findSerializer(provider, proxiedValue).serializeWithType(proxiedValue, g, provider, typeSer);
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        throws DatabindException
    {
        SerializationContext prov = visitor.getContext();
        if ((prov == null) || (_property == null)) {
            super.acceptJsonFormatVisitor(visitor, typeHint);
        } else {
            JavaType type = _property.getType();
            prov.findPrimaryPropertySerializer(type, _property)
                .acceptJsonFormatVisitor(visitor, type);
        }
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    protected ValueSerializer<Object> findSerializer(SerializationContext provider, Object value)
    {
        /* TODO: if Hibernate did use generics, or we wanted to allow use of Jackson
         *  annotations to indicate type, should take that into account.
         */
        Class<?> type = value.getClass();
        /* We will use a map to contain serializers found so far, keyed by type:
         * this avoids potentially costly lookup from global caches and/or construction
         * of new serializers. Check the local cache first before doing full resolution.
         */
        ValueSerializer<Object> ser = _dynamicSerializers.serializerFor(type);
        if (ser != null) {
            return ser;
        }
        /* 18-Oct-2013, tatu: Whether this is for the primary property or secondary is
         *   really anyone's guess at this point; proxies can exist at any level?
         */
        final JavaType valueType = provider.constructType(type);
        if (_unwrapper == null) {
            PropertySerializerMap.SerializerAndMapResult result =
                    _dynamicSerializers.findAndAddPrimarySerializer(valueType, provider, _property);
            _dynamicSerializers = result.map;
            return result.serializer;
        }
        // 31-Aug-2026, [datatypes-hibernate#209]: when unwrapping, resolve and cache by
        //   hand. findAndAddPrimarySerializer() would add the raw serializer under this
        //   same type first, and a lookup returns the first match for a type: later calls
        //   would then get a serializer that writes START_OBJECT even though the
        //   unwrapping property writer has already suppressed the property name.
        ser = provider.findPrimaryPropertySerializer(valueType, _property)
                .unwrappingSerializer(_unwrapper);
        _dynamicSerializers = _dynamicSerializers.addSerializer(type, ser).map;
        return ser;
    }

    /**
     * Helper method for finding value being proxied, if it is available
     * or if it is to be forced to be loaded.
     */
    protected Object findProxied(HibernateProxy proxy)
    {
        LazyInitializer init = proxy.getHibernateLazyInitializer();
        if (!_forceLazyLoading && init.isUninitialized()) {
            if (_serializeIdentifier) {
                final Object idValue = init.getIdentifier();
                if (_wrappedIdentifier) {
                    return Collections.singletonMap(getIdentifierPropertyName(init), idValue);
                } else {
                    return idValue;
                }
            }
            return null;
        }
        try {
            return init.getImplementation();
        } catch (EntityNotFoundException e) {
            if (_nullMissingEntities) {
                return null;
            } else {
                throw e;
            }
        }
    }

    /**
     * Helper method to retrieve the name of the identifier property of the
     * specified lazy initializer.
     * @param init Lazy initializer to obtain identifier property name from.
     * @return Name of the identity property of the specified lazy initializer.
     */
    private String getIdentifierPropertyName(final LazyInitializer init) {
        String idName;
        if (_mapping != null) {
            idName = _mapping.getIdentifierPropertyName(init.getEntityName());
        } else {
            idName = ProxySessionReader.getIdentifierPropertyName(init);
            if (idName == null) {
                idName = ProxyReader.getIdentifierPropertyName(init);
                if (idName == null) {
                    idName = init.getEntityName();
                }
            }
        }
        return idName;
    }
    
    /**
     * Inspects a Hibernate proxy to try and determine the name of the identifier property
     * (Hibernate proxies know the getter of the identifier property because it receives special 
     * treatment in the invocation handler). Alas, the field storing the method reference is 
     * private and has no getter, so we must resort to ugly reflection hacks to read its value ... 
     */
    protected static class ProxyReader {

        // static final so the JVM can inline the lookup
        private static final Field getIdentifierMethodField;

        static {
            try {
                getIdentifierMethodField = BasicLazyInitializer.class.getDeclaredField("getIdentifierMethod");
                getIdentifierMethodField.setAccessible(true);
            } catch (Exception e) {
                // should never happen: the field exists in all versions of hibernate 4 and 5
                throw new RuntimeException(e); 
            }
        }

        /**
         * @return the name of the identifier property, or null if the name could not be determined
         */
        static String getIdentifierPropertyName(LazyInitializer init) {
            try {
                Method idGetter = (Method) getIdentifierMethodField.get(init);
                if (idGetter == null) {
                    return null;
                }
                String name = idGetter.getName();
                if (name.startsWith("get")) {
                    name = Introspector.decapitalize(name.substring(3));
                }
                return name;
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    /**
     * Hibernate 5.2 broke abi compatibility of org.hibernate.proxy.LazyInitializer.getSession()
     * The api contract changed
     * from org.hibernate.proxy.LazyInitializer.getSession()Lorg.hibernate.engine.spi.SessionImplementor;
     * to org.hibernate.proxy.LazyInitializer.getSession()Lorg.hibernate.engine.spi.SharedSessionContractImplementor
     * 
     * On hibernate 5.2 the interface SessionImplementor extends SharedSessionContractImplementor.
     * And an instance of org.hibernate.internal.SessionImpl is returned from getSession().
     */
    protected static class ProxySessionReader {
    	
    	/**
    	 * The getSession method must be executed using reflection for compatibility purpose.
    	 * For efficiency keep the method cached.
    	 */
        protected static final Method lazyInitializerGetSessionMethod;
        
        static {
            try {
                lazyInitializerGetSessionMethod = LazyInitializer.class.getMethod("getSession");
            } catch (Exception e) {
                // should never happen: the class and method exists in all versions of hibernate 5
                throw new RuntimeException(e); 
            }
        }
        
        static String getIdentifierPropertyName(LazyInitializer init) {
            final Object session;
            try{
                session = lazyInitializerGetSessionMethod.invoke(init);
            } catch (Exception e) {
                // Should never happen
                throw new RuntimeException(e);
            }
            if(session instanceof SessionImplementor){
            	SessionFactoryImplementor factory = ((SessionImplementor)session).getFactory();
            	return factory.getIdentifierPropertyName(init.getEntityName());
            }else if (session != null) {
                // Should never happen: session should be an instance of org.hibernate.internal.SessionImpl
                // factory = session.getClass().getMethod("getFactory").invoke(session);
                throw new RuntimeException("Session is not instance of SessionImplementor");
            }
            return null;
        }
    }
}
