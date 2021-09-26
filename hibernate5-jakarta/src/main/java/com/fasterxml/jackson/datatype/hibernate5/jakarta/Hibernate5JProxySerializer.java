package com.fasterxml.jackson.datatype.hibernate5.jakarta;

import java.beans.Introspector;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.proxy.pojo.BasicLazyInitializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;
import com.fasterxml.jackson.databind.util.NameTransformer;

import jakarta.persistence.EntityNotFoundException;

/**
 * Serializer to use for values proxied using {@link org.hibernate.proxy.HibernateProxy}.
 *<p>
 * TODO: should try to make this work more like Jackson
 * {@code BeanPropertyWriter}, possibly sub-classing
 * it -- it handles much of functionality we need, and has
 * access to more information than value serializers (like
 * this one) have.
 */
public class Hibernate5JProxySerializer
    extends JsonSerializer<HibernateProxy>
    implements ContextualSerializer
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

    // For datatype-hibernate#97
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

    public Hibernate5JProxySerializer(boolean forceLazyLoading, boolean serializeIdentifier,
            boolean nullMissingEntities, boolean wrappedIdentifier,
            Mapping mapping)
    {
        this(forceLazyLoading, serializeIdentifier, nullMissingEntities, wrappedIdentifier,
                mapping, null, null);
    }

    public Hibernate5JProxySerializer(boolean forceLazyLoading, boolean serializeIdentifier,
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

    protected Hibernate5JProxySerializer(Hibernate5JProxySerializer base,
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
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        return new Hibernate5JProxySerializer(this, property, _unwrapper);
    }

    @Override
    public JsonSerializer<HibernateProxy> unwrappingSerializer(final NameTransformer unwrapper) {
        return new Hibernate5JProxySerializer(this, _property, unwrapper);
    }

    @Override
    public boolean isUnwrappingSerializer()
    {
        return _unwrapper != null;
    }

    /*
    /**********************************************************************
    /* JsonSerializer impl
    /**********************************************************************
     */

    @Override
    public boolean isEmpty(SerializerProvider provider, HibernateProxy value) {
        return (value == null) || (findProxied(value) == null);
    }
    
    @Override
    public void serialize(HibernateProxy value, JsonGenerator g, SerializerProvider provider)
        throws IOException
    {
        Object proxiedValue = findProxied(value);
        // TODO: figure out how to suppress nulls, if necessary? (too late for that here)
        if (proxiedValue == null) {
            provider.defaultSerializeNull(g);
            return;
        }
        findSerializer(provider, proxiedValue).serialize(proxiedValue, g, provider);
    }

    @Override
    public void serializeWithType(HibernateProxy value, JsonGenerator g, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException
    {
        Object proxiedValue = findProxied(value);
        if (proxiedValue == null) {
            provider.defaultSerializeNull(g);
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
        throws JsonMappingException
    {
        SerializerProvider prov = visitor.getProvider();
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

    protected JsonSerializer<Object> findSerializer(SerializerProvider provider, Object value)
        throws IOException
    {
        /* TODO: if Hibernate did use generics, or we wanted to allow use of Jackson
         *  annotations to indicate type, should take that into account.
         */
        Class<?> type = value.getClass();
        /* we will use a map to contain serializers found so far, keyed by type:
         * this avoids potentially costly lookup from global caches and/or construction
         * of new serializers
         */
        /* 18-Oct-2013, tatu: Whether this is for the primary property or secondary is
         *   really anyone's guess at this point; proxies can exist at any level?
         */
        PropertySerializerMap.SerializerAndMapResult result =
                _dynamicSerializers.findAndAddPrimarySerializer(type, provider, _property);
        if (_dynamicSerializers != result.map) {
            _dynamicSerializers = result.map;
        }
        if (_unwrapper != null)
        {
            return result.serializer.unwrappingSerializer(_unwrapper);
        }
        return result.serializer;
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
                    HashMap<String, Object> map = new HashMap<>();
                    map.put(getIdentifierPropertyName(init), idValue);
                    return map;
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
