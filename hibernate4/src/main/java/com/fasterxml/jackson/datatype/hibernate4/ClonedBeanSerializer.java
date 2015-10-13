package com.fasterxml.jackson.datatype.hibernate4;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerBuilder;
import com.fasterxml.jackson.databind.ser.impl.BeanAsArraySerializer;
import com.fasterxml.jackson.databind.ser.impl.ObjectIdWriter;
import com.fasterxml.jackson.databind.ser.impl.UnwrappingBeanSerializer;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import com.fasterxml.jackson.databind.util.NameTransformer;

import java.io.IOException;

/**
 * Clone of BeanSerializer, because we need to alter serialize method in HibernateProxySerializer
 */
public class ClonedBeanSerializer
        extends BeanSerializerBase
{
    private static final long serialVersionUID = -4536893235025590367L;

    /*
    /**********************************************************
    /* Life-cycle: constructors
    /**********************************************************
     */

    /**
     * @param builder Builder object that contains collected information
     *   that may be needed for serializer
     * @param properties Property writers used for actual serialization
     */
    public ClonedBeanSerializer(JavaType type, BeanSerializerBuilder builder,
                                BeanPropertyWriter[] properties, BeanPropertyWriter[] filteredProperties)
    {
        super(type, builder, properties, filteredProperties);
    }

    /**
     * Alternate copy constructor that can be used to construct
     * standard {@link ClonedBeanSerializer} passing an instance of
     * "compatible enough" source serializer.
     */
    protected ClonedBeanSerializer(BeanSerializerBase src) {
        super(src);
    }

    protected ClonedBeanSerializer(BeanSerializerBase src,
                                   ObjectIdWriter objectIdWriter) {
        super(src, objectIdWriter);
    }

    protected ClonedBeanSerializer(BeanSerializerBase src,
                                   ObjectIdWriter objectIdWriter, Object filterId) {
        super(src, objectIdWriter, filterId);
    }

    protected ClonedBeanSerializer(BeanSerializerBase src, String[] toIgnore) {
        super(src, toIgnore);
    }

    /*
    /**********************************************************
    /* Life-cycle: factory methods, fluent factories
    /**********************************************************
     */

    /**
     * Method for constructing dummy bean serializer; one that
     * never outputs any properties
     */
    public static ClonedBeanSerializer createDummy(JavaType forType)
    {
        return new ClonedBeanSerializer(forType, null, NO_PROPS, null);
    }

    @Override
    public JsonSerializer<Object> unwrappingSerializer(NameTransformer unwrapper) {
        return new UnwrappingBeanSerializer(this, unwrapper);
    }

    @Override
    public BeanSerializerBase withObjectIdWriter(ObjectIdWriter objectIdWriter) {
        return new ClonedBeanSerializer(this, objectIdWriter, _propertyFilterId);
    }

    @Override
    protected BeanSerializerBase withFilterId(Object filterId) {
        return new ClonedBeanSerializer(this, _objectIdWriter, filterId);
    }

    @Override
    protected BeanSerializerBase withIgnorals(String[] toIgnore) {
        return new ClonedBeanSerializer(this, toIgnore);
    }

    /**
     * Implementation has to check whether as-array serialization
     * is possible reliably; if (and only if) so, will construct
     * a {@link BeanAsArraySerializer}, otherwise will return this
     * serializer as is.
     */
    @Override
    protected BeanSerializerBase asArraySerializer()
    {
        /* Can not:
         *
         * - have Object Id (may be allowed in future)
         * - have "any getter"
         * - have per-property filters
         */
        if ((_objectIdWriter == null)
                && (_anyGetterWriter == null)
                && (_propertyFilterId == null)
                ) {
            return new BeanAsArraySerializer(this);
        }
        // already is one, so:
        return this;
    }

    /*
    /**********************************************************
    /* JsonSerializer implementation that differs between impls
    /**********************************************************
     */

    /**
     * Main serialization method that will delegate actual output to
     * configured
     * {@link BeanPropertyWriter} instances.
     */
    @Override
    public void serialize(Object bean, JsonGenerator gen, SerializerProvider provider)
            throws IOException
    {
        if (_objectIdWriter != null) {
            _serializeWithObjectId(bean, gen, provider, true);
            return;
        }
        gen.writeStartObject();
        // [databind#631]: Assign current value, to be accessible by custom serializers
        gen.setCurrentValue(bean);
        if (_propertyFilterId != null) {
            serializeFieldsFiltered(bean, gen, provider);
        } else {
            serializeFields(bean, gen, provider);
        }
        gen.writeEndObject();
    }

    /*
    /**********************************************************
    /* Standard methods
    /**********************************************************
     */

    @Override public String toString() {
        return "BeanSerializer for "+handledType().getName();
    }
}
