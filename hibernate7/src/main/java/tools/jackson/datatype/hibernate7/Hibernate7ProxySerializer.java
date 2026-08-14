package tools.jackson.datatype.hibernate7;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.proxy.pojo.BasicLazyInitializer;
import org.hibernate.type.MappingContext;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.impl.PropertySerializerMap;
import tools.jackson.databind.util.NameTransformer;

import jakarta.persistence.EntityNotFoundException;

/**
 * Serializer to use for values proxied using {@link HibernateProxy}.
 *<p>
 * TODO: should try to make this work more like Jackson
 * {@code BeanPropertyWriter}, possibly sub-classing
 * it -- it handles much of functionality we need, and has
 * access to more information than value serializers (like
 * this one) have.
 */
public class Hibernate7ProxySerializer
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
    protected final MappingContext _mapping;

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

    public Hibernate7ProxySerializer(boolean forceLazyLoading, boolean serializeIdentifier,
                                     boolean nullMissingEntities, boolean wrappedIdentifier,
                                     MappingContext mapping)
    {
        this(forceLazyLoading, serializeIdentifier, nullMissingEntities, wrappedIdentifier,
                mapping, null, null);
    }

    public Hibernate7ProxySerializer(boolean forceLazyLoading, boolean serializeIdentifier,
                                     boolean nullMissingEntities, boolean wrappedIdentifier,
                                     MappingContext mapping, BeanProperty property, NameTransformer unwrapper)
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

    protected Hibernate7ProxySerializer(Hibernate7ProxySerializer base,
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
        return new Hibernate7ProxySerializer(this, property, _unwrapper);
    }

    @Override
    public ValueSerializer<HibernateProxy> unwrappingSerializer(final NameTransformer unwrapper) {
        return new Hibernate7ProxySerializer(this, _property, unwrapper);
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
        PropertySerializerMap.SerializerAndMapResult result =
                _dynamicSerializers.findAndAddPrimarySerializer(
                        provider.constructType(type),
                        provider,
                        _property);
        _dynamicSerializers = result.map;
        ser = result.serializer;
        // 14-Aug-2026: Apply unwrapping once and cache the result so we
        // don't re-wrap on every call
        if (_unwrapper != null) {
            ser = ser.unwrappingSerializer(_unwrapper);
            _dynamicSerializers = _dynamicSerializers.addSerializer(type, ser).map;
        }
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
            idName = ProxyReader.getIdentifierPropertyName(init);
            if (idName == null) {
                idName = init.getEntityName();
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
                    name = _decapitalize(name.substring(3));
                }
                return name;
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        // Same as `java.beans.Introspector.decapitalize()`:
        private static String _decapitalize(String name) {
            if (name == null || name.length() == 0) {
                return name;
            }
            if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) &&
                    Character.isUpperCase(name.charAt(0))){
                return name;
            }
            char[] chars = name.toCharArray();
            chars[0] = Character.toLowerCase(chars[0]);
            return new String(chars);
        }
    }
}
