package com.fasterxml.jackson.datatype.hibernate6;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.Serializers;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.proxy.HibernateProxy;

public class Hibernate6Serializers extends Serializers.Base
{
    protected final boolean _forceLoading;
    protected final boolean _serializeIdentifiers;
    protected final boolean _nullMissingEntities;
    protected final boolean _wrappedIdentifier;
    protected final Mapping _mapping;

    public Hibernate6Serializers(int features) {
        this(null, features);
    }

    public Hibernate6Serializers(Mapping mapping, int features)
    {
        _forceLoading = Hibernate6Module.Feature.FORCE_LAZY_LOADING.enabledIn(features);
        _serializeIdentifiers = Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS.enabledIn(features);
        _nullMissingEntities = Hibernate6Module.Feature.WRITE_MISSING_ENTITIES_AS_NULL.enabledIn(features);
        _wrappedIdentifier = Hibernate6Module.Feature.WRAP_IDENTIFIER_IN_OBJECT.enabledIn(features);
        _mapping = mapping;
    }

    @Override
    public JsonSerializer<?> findSerializer(SerializationConfig config,
            JavaType type, BeanDescription beanDesc)
    {
        Class<?> raw = type.getRawClass();
        if (HibernateProxy.class.isAssignableFrom(raw)) {
            return new Hibernate6ProxySerializer(_forceLoading, _serializeIdentifiers,
                    _nullMissingEntities, _wrappedIdentifier, _mapping);
        }
        return null;
    }
}
