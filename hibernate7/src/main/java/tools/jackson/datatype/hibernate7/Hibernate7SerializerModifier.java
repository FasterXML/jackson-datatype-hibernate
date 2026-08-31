package tools.jackson.datatype.hibernate7;

import jakarta.persistence.Entity;

import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ser.ValueSerializerModifier;
import tools.jackson.databind.type.CollectionType;
import tools.jackson.databind.type.MapType;
import org.hibernate.SessionFactory;

public class Hibernate7SerializerModifier
    extends ValueSerializerModifier
{
    private static final long serialVersionUID = 3L;

    protected final int _features;

    protected final SessionFactory _sessionFactory;

    public Hibernate7SerializerModifier(int features, SessionFactory sessionFactory) {
        _features = features;
        _sessionFactory = sessionFactory;
    }

    @Override
    public ValueSerializer<?> modifyCollectionSerializer(SerializationConfig config,
            CollectionType valueType, BeanDescription.Supplier beanDesc, ValueSerializer<?> serializer) {
        return new PersistentCollectionSerializer(valueType, serializer, _features, _sessionFactory);
    }

    @Override
    public ValueSerializer<?> modifyMapSerializer(SerializationConfig config,
            MapType valueType, BeanDescription.Supplier beanDesc, ValueSerializer<?> serializer) {
        return new PersistentCollectionSerializer(valueType, serializer, _features, _sessionFactory);
    }

    /**
     * Wraps bean serializers for {@code @Entity}-annotated types with a
     * cycle-detecting wrapper when both {@code REPLACE_CYCLES_WITH_NULL} and
     * {@code FORCE_LAZY_LOADING} are enabled. This prevents infinite recursion
     * in bidirectional entity graphs (see [datatype-hibernate#204]).
     *<p>
     * {@code REPLACE_CYCLES_WITH_NULL} is disabled by default since replacing a
     * back-reference with {@code null} is a behavioral change; and it only has
     * effect together with {@code FORCE_LAZY_LOADING}, which is what causes the
     * lazy back-references to be initialized and followed in the first place.
     */
    @Override
    public ValueSerializer<?> modifySerializer(SerializationConfig config,
            BeanDescription.Supplier beanDesc, ValueSerializer<?> serializer)
    {
        if (!Hibernate7Module.Feature.REPLACE_CYCLES_WITH_NULL.enabledIn(_features)
                || !Hibernate7Module.Feature.FORCE_LAZY_LOADING.enabledIn(_features)) {
            return serializer;
        }
        // Only wrap entity bean serializers — skip collections, maps, arrays,
        // enums, and primitive/wrapper types which cannot participate in
        // entity-level cycles.
        if (beanDesc != null) {
            if (beanDesc.getClassAnnotations().get(Entity.class) != null) {
                return new CycleDetectingSerializer(serializer);
            }
        }
        return serializer;
    }
}
