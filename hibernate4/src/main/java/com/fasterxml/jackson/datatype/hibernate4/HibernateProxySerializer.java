package com.fasterxml.jackson.datatype.hibernate4;


import java.io.IOException;
import java.util.HashMap;

import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializerBuilder;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;

/**
 * Serializer to use for values proxied using {@link HibernateProxy}.
 * <p>
 * TODO: should try to make this work more like Jackson
 * <code>BeanPropertyWriter</code>, possibly sub-classing it -- it handles much
 * of functionality we need, and has access to more information than value
 * serializers (like this one) have.
 */
public class HibernateProxySerializer extends BeanSerializer {
	/**
	 * Property that has proxy value to handle
	 */
	protected final BeanProperty _property;

	protected final boolean _forceLazyLoading;
	protected final boolean _serializeIdentifier;
	protected final Mapping _mapping;

	/**
	 * For efficient serializer lookup, let's use this; most of the time,
	 * there's just one type and one serializer.
	 */
	protected PropertySerializerMap _dynamicSerializers;

	/*
	 * /**********************************************************************
	 * /* Life cycle
	 * /**********************************************************************
	 */

	public HibernateProxySerializer(boolean forceLazyLoading) {
		this(forceLazyLoading, false, null);
	}

	public HibernateProxySerializer(boolean forceLazyLoading,
			boolean serializeIdentifier) {
		this(forceLazyLoading, serializeIdentifier, null);
	}
	
	public HibernateProxySerializer(boolean forceLazyLoading,
			boolean serializeIdentifier, Mapping mapping) {
		this(null, null, null, null, forceLazyLoading, serializeIdentifier, mapping);
	}
	
	

	public HibernateProxySerializer(JavaType type, BeanSerializerBuilder builder,
		    BeanPropertyWriter[] properties, BeanPropertyWriter[] filteredProperties, boolean forceLazyLoading,
			boolean serializeIdentifier, Mapping mapping) {
		super(type, builder,
				properties, filteredProperties);
		_forceLazyLoading = forceLazyLoading;
		_serializeIdentifier = serializeIdentifier;
		_mapping = mapping;
		_dynamicSerializers = PropertySerializerMap.emptyMap();
		_property = null;
	}

	/*
	 * /**********************************************************************
	 * /* JsonSerializer impl
	 * /**********************************************************************
	 */

	// since 2.3
	public boolean isEmpty(HibernateProxy value) {
		return (value == null) || (findProxied(value) == null);
	}

	public void serialize(HibernateProxy value, JsonGenerator jgen,
			SerializerProvider provider) throws IOException,
			JsonProcessingException {
		Object proxiedValue = findProxied(value);
		// TODO: figure out how to suppress nulls, if necessary? (too late for
		// that here)
		if (proxiedValue == null) {
			provider.defaultSerializeNull(jgen);
			return;
		}
		findSerializer(provider, proxiedValue).serialize(proxiedValue, jgen,
				provider);
	}

	public void serializeWithType(HibernateProxy value, JsonGenerator jgen,
			SerializerProvider provider, TypeSerializer typeSer)
			throws IOException, JsonProcessingException {
		
		if (_objectIdWriter != null) {
            _serializeWithObjectId(value, jgen, provider, typeSer);
            return;
        }
		
		Object proxiedValue = findProxied(value);
		if (proxiedValue == null) {
			provider.defaultSerializeNull(jgen);
			return;
		}
		/*
		 * This isn't exactly right, since type serializer really refers to
		 * proxy object, not value. And we really don't either know static type
		 * (necessary to know how to apply additional type info) or other
		 * things; so it's not going to work well. But... we'll do out best.
		 */
		findSerializer(provider, proxiedValue).serializeWithType(proxiedValue,
				jgen, provider, typeSer);
	}

	/*
	 * /**********************************************************************
	 * /* Helper methods
	 * /**********************************************************************
	 */

	protected JsonSerializer<Object> findSerializer(
			SerializerProvider provider, Object value) throws IOException,
			JsonProcessingException {
		/*
		 * TODO: if Hibernate did use generics, or we wanted to allow use of
		 * Jackson annotations to indicate type, should take that into account.
		 */
		Class<?> type = value.getClass();
		/*
		 * we will use a map to contain serializers found so far, keyed by type:
		 * this avoids potentially costly lookup from global caches and/or
		 * construction of new serializers
		 */
		/*
		 * 18-Oct-2013, tatu: Whether this is for the primary property or
		 * secondary is really anyone's guess at this point; proxies can exist
		 * at any level?
		 */
		PropertySerializerMap.SerializerAndMapResult result = _dynamicSerializers
				.findAndAddPrimarySerializer(type, provider, _property);
		if (_dynamicSerializers != result.map) {
			_dynamicSerializers = result.map;
		}
		return result.serializer;
	}

	/**
	 * Helper method for finding value being proxied, if it is available or if
	 * it is to be forced to be loaded.
	 */
	protected Object findProxied(HibernateProxy proxy) {
		LazyInitializer init = proxy.getHibernateLazyInitializer();
		if (!_forceLazyLoading && init.isUninitialized()) {
			if (_serializeIdentifier) {
				final String idName;
				if (_mapping != null) {
					idName = _mapping.getIdentifierPropertyName(init
							.getEntityName());
				} else {
					final SessionImplementor session = init.getSession();
					if (session != null) {
						idName = session
								.getFactory()
								.getIdentifierPropertyName(init.getEntityName());
					} else {
						idName = init.getEntityName();
					}
				}
				final Object idValue = init.getIdentifier();
				HashMap<String, Object> map = new HashMap<String, Object>();
				map.put(idName, idValue);
				return map;
			}
			return null;
		}
		return init.getImplementation();
	}
}
