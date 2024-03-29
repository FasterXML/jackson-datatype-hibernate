package com.fasterxml.jackson.datatype.hibernate5.jakarta;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.Serializers;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.proxy.HibernateProxy;

public class Hibernate5JSerializers extends Serializers.Base
{
    protected final boolean _forceLoading;
    protected final boolean _serializeIdentifiers;
    protected final boolean _nullMissingEntities;
    protected final boolean _wrappedIdentifier;
    protected final Mapping _mapping;

    public Hibernate5JSerializers(int features) {
        this(null, features);
    }

    public Hibernate5JSerializers(Mapping mapping, int features)
    {
        _forceLoading = Hibernate5JakartaModule.Feature.FORCE_LAZY_LOADING.enabledIn(features);
        _serializeIdentifiers = Hibernate5JakartaModule.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS.enabledIn(features);
        _nullMissingEntities = Hibernate5JakartaModule.Feature.WRITE_MISSING_ENTITIES_AS_NULL.enabledIn(features);
        _wrappedIdentifier = Hibernate5JakartaModule.Feature.WRAP_IDENTIFIER_IN_OBJECT.enabledIn(features);
        _mapping = mapping;
    }

    @Override
    public JsonSerializer<?> findSerializer(SerializationConfig config,
            JavaType type, BeanDescription beanDesc)
    {
        Class<?> raw = type.getRawClass();
        if (HibernateProxy.class.isAssignableFrom(raw)) {
            return new Hibernate5JProxySerializer(_forceLoading, _serializeIdentifiers,
                    _nullMissingEntities, _wrappedIdentifier, _mapping);
        }
        return null;
    }
}
