package tools.jackson.datatype.hibernate4;

import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ser.ValueSerializerModifier;
import tools.jackson.databind.type.CollectionType;
import tools.jackson.databind.type.MapType;
import org.hibernate.SessionFactory;

public class HibernateSerializerModifier
    extends ValueSerializerModifier
{
    protected final int _features;

    protected final SessionFactory _sessionFactory;

    public HibernateSerializerModifier(int features, SessionFactory sessionFactory) {
        _features = features;
        _sessionFactory = sessionFactory;
    }
    
    /*
    @Override
    public ValueSerializer<?> modifySerializer(SerializationConfig config,
            BeanDescription.Supplier beanDesc, ValueSerializer<?> serializer) {
        return serializer;
    }
    */

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
}
