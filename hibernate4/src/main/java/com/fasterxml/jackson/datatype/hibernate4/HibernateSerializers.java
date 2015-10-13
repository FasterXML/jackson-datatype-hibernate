package com.fasterxml.jackson.datatype.hibernate4;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.*;
import com.fasterxml.jackson.databind.ser.std.MapSerializer;
import com.fasterxml.jackson.databind.type.*;
import com.fasterxml.jackson.datatype.hibernate4.Hibernate4Module.Feature;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.proxy.HibernateProxy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HibernateSerializers
        extends BeanSerializerFactory implements Serializers
{
    protected final boolean _forceLoading;
    protected final boolean _serializeIdentifiers;
    protected final Mapping _mapping;
    private SerializerProvider _prov;

    public HibernateSerializers(int features) {
        this(null, null, features);
    }

    public HibernateSerializers(SerializerProvider prov, Mapping mapping, int features)
    {
        super(null);
        _forceLoading = Feature.FORCE_LAZY_LOADING.enabledIn(features);
        _serializeIdentifiers = Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS.enabledIn(features);
        _mapping = mapping;
        _prov = prov;
    }

    @Override
    public JsonSerializer<?> findSerializer(SerializationConfig config,
            JavaType type, BeanDescription beanDesc)
    {
        Class<?> raw = type.getRawClass();
        if (HibernateProxy.class.isAssignableFrom(raw)) {
            try {
                BeanSerializerBuilder builder = constructBeanSerializerBuilder(beanDesc);
                // BuilderUtil.setConfig(builder, config);

                // First: any detectable (auto-detect, annotations) properties
                // to serialize?
                List<BeanPropertyWriter> props;
                props = findBeanProperties(_prov, beanDesc, builder);
                if (props == null) {
                    props = new ArrayList<BeanPropertyWriter>();
                }
                // [JACKSON-440] Need to allow modification bean properties to
                // serialize:
                if (_factoryConfig.hasSerializerModifiers()) {
                    for (BeanSerializerModifier mod : _factoryConfig
                            .serializerModifiers()) {
                        props = mod.changeProperties(config, beanDesc, props);
                    }
                }

                // Any properties to suppress?
                props = filterBeanProperties(config, beanDesc, props);

                // remove hibernate internal properties
                Iterator<BeanPropertyWriter> iterator = props.iterator();
                while (iterator.hasNext()) {
                    BeanPropertyWriter beanPropertyWriter = iterator.next();
                    if (beanPropertyWriter.getName().equals("handler")
                            || beanPropertyWriter.getName().equals(
                            "hibernateLazyInitializer")) {
                        iterator.remove();
                    }
                }

                // [JACKSON-440] Need to allow reordering of properties to
                // serialize
                if (_factoryConfig.hasSerializerModifiers()) {
                    for (BeanSerializerModifier mod : _factoryConfig
                            .serializerModifiers()) {
                        props = mod.orderProperties(config, beanDesc, props);
                    }
                }

				/*
				 * And if Object Id is needed, some preparation for that as
				 * well: better do before view handling, mostly for the custom
				 * id case which needs access to a property
				 */
                builder.setObjectIdWriter(constructObjectIdHandler(_prov,
                        beanDesc, props));

                builder.setProperties(props);
                builder.setFilterId(findFilterId(config, beanDesc));

                AnnotatedMember anyGetter = beanDesc.findAnyGetter();
                if (anyGetter != null) {
                    if (config.canOverrideAccessModifiers()) {
                        anyGetter.fixAccess();
                    }
                    JavaType type2 = anyGetter.getType(beanDesc
                            .bindingsForBeanType());
                    // copied from BasicSerializerFactory.buildMapSerializer():
                    boolean staticTyping = config
                            .isEnabled(MapperFeature.USE_STATIC_TYPING);
                    JavaType valueType = type.getContentType();
                    TypeSerializer typeSer = createTypeSerializer(config,
                            valueType);
                    // last 2 nulls; don't know key, value serializers (yet)
                    // TODO: support '@JsonIgnoreProperties' with any setter?
                    MapSerializer mapSer = MapSerializer.construct(/*
																	 * ignored
																	 * props
																	 */null,
                            type2, staticTyping, typeSer, null, null, /* filterId */
                            null);
                    // TODO: can we find full PropertyName?
                    PropertyName name = new PropertyName(anyGetter.getName());
                    BeanProperty.Std anyProp = new BeanProperty.Std(name,
                            valueType, null, beanDesc.getClassAnnotations(),
                            anyGetter, PropertyMetadata.STD_OPTIONAL);
                    builder.setAnyGetter(new AnyGetterWriter(anyProp,
                            anyGetter, mapSer));
                }
                // Next: need to gather view information, if any:
                processViews(config, builder);

                // Finally: let interested parties mess with the result bit
                // more...
                if (_factoryConfig.hasSerializerModifiers()) {
                    for (BeanSerializerModifier mod : _factoryConfig
                            .serializerModifiers()) {
                        builder = mod.updateBuilder(config, beanDesc, builder);
                    }
                }
                return new HibernateProxySerializer(type, builder,
                        props.toArray(new BeanPropertyWriter[] {}), builder.getFilteredProperties(),
                        _forceLoading, _serializeIdentifiers, _mapping);
            } catch (JsonMappingException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return null;
    }

    @Override
    public JsonSerializer<?> findArraySerializer(SerializationConfig config, ArrayType type, BeanDescription beanDesc, TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer) {
        return null;
    }

    @Override
    public JsonSerializer<?> findCollectionSerializer(SerializationConfig config, CollectionType type, BeanDescription beanDesc, TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer) {
        return null;
    }

    @Override
    public JsonSerializer<?> findCollectionLikeSerializer(SerializationConfig config, CollectionLikeType type, BeanDescription beanDesc, TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer) {
        return null;
    }

    @Override
    public JsonSerializer<?> findMapSerializer(SerializationConfig config, MapType type, BeanDescription beanDesc, JsonSerializer<Object> keySerializer, TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer) {
        return null;
    }

    @Override
    public JsonSerializer<?> findMapLikeSerializer(SerializationConfig config, MapLikeType type, BeanDescription beanDesc, JsonSerializer<Object> keySerializer, TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer) {
        return null;
    }
}
