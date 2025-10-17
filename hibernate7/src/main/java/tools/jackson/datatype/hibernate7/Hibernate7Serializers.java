package tools.jackson.datatype.hibernate7;

import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ser.Serializers;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.MappingContext;

public class Hibernate7Serializers extends Serializers.Base
{
    protected final boolean _forceLoading;
    protected final boolean _serializeIdentifiers;
    protected final boolean _nullMissingEntities;
    protected final boolean _wrappedIdentifier;
    protected final MappingContext _mapping;

    public Hibernate7Serializers(int features) {
        this(null, features);
    }

    public Hibernate7Serializers(MappingContext mapping, int features)
    {
        _forceLoading = Hibernate7Module.Feature.FORCE_LAZY_LOADING.enabledIn(features);
        _serializeIdentifiers = Hibernate7Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS.enabledIn(features);
        _nullMissingEntities = Hibernate7Module.Feature.WRITE_MISSING_ENTITIES_AS_NULL.enabledIn(features);
        _wrappedIdentifier = Hibernate7Module.Feature.WRAP_IDENTIFIER_IN_OBJECT.enabledIn(features);
        _mapping = mapping;
    }

    @Override
    public ValueSerializer<?> findSerializer(SerializationConfig config,
            JavaType type, BeanDescription.Supplier beanDesc, JsonFormat.Value formatOverrides)
    {
        Class<?> raw = type.getRawClass();
        if (HibernateProxy.class.isAssignableFrom(raw)) {
            return new Hibernate7ProxySerializer(_forceLoading, _serializeIdentifiers,
                    _nullMissingEntities, _wrappedIdentifier, _mapping);
        }
        return null;
    }
}
