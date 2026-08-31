package tools.jackson.datatype.hibernate7;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.util.NameTransformer;

/**
 * Wrapper serializer that detects cyclic object references during
 * serialization by tracking which objects are currently being serialized
 * via {@link SerializationContext} attributes.  When the same object
 * instance is encountered again, {@code null} is written instead of
 * recursing.
 * <p>
 * This addresses infinite recursion in bidirectional JPA entity graphs
 * when {@code FORCE_LAZY_LOADING} is enabled (see [datatype-hibernate#204]).
 *
 * @since 3.3
 */
public class CycleDetectingSerializer<T> extends ValueSerializer<T>
{
    private static final Object ATTR_KEY = CycleDetectingSerializer.class;

    private final ValueSerializer<T> _delegate;

    /**
     * Whether the delegate handles cycles itself via Object Ids
     * (that is, the entity is annotated with {@code @JsonIdentityInfo}).
     */
    private final boolean _delegateUsesObjectId;

    public CycleDetectingSerializer(ValueSerializer<T> delegate) {
        _delegate = delegate;
        _delegateUsesObjectId = delegate.usesObjectId();
    }

    /**
     * Must delegate resolution: the cache resolves the outermost (wrapping)
     * serializer, so without this {@code BeanSerializerBase.resolve()} would
     * never run for wrapped entities -- dropping {@code @JsonSerialize(converter=...)},
     * {@code nullsUsing} handling and any-getter contextualization.
     */
    @Override
    public void resolve(SerializationContext ctxt) {
        _delegate.resolve(ctxt);
    }

    /**
     * Must delegate contextualization for the same reason as {@link #resolve};
     * otherwise per-property {@code @JsonIgnoreProperties}, {@code @JsonFilter}
     * and {@code @JsonIdentityInfo} overrides are silently ignored for wrapped
     * entities.
     */
    @SuppressWarnings("unchecked")
    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty property)
    {
        ValueSerializer<?> del = _delegate.createContextual(ctxt, property);
        if (del == _delegate) {
            return this;
        }
        return new CycleDetectingSerializer<T>((ValueSerializer<T>) del);
    }

    @Override
    public void serialize(T value, JsonGenerator g, SerializationContext ctxt)
        throws DatabindException
    {
        if (_delegateUsesObjectId) {
            _delegate.serialize(value, g, ctxt);
            return;
        }
        Set<Object> serializing = _getSerializingSet(ctxt);
        // Identity-based check: same object instance on the current path = cycle
        if (serializing.contains(value)) {
            ctxt.defaultSerializeNullValue(g);
            return;
        }
        serializing.add(value);
        try {
            _delegate.serialize(value, g, ctxt);
        } finally {
            serializing.remove(value);
        }
    }

    @Override
    public void serializeWithType(T value, JsonGenerator g, SerializationContext ctxt,
            TypeSerializer typeSer) throws DatabindException
    {
        if (_delegateUsesObjectId) {
            _delegate.serializeWithType(value, g, ctxt, typeSer);
            return;
        }
        Set<Object> serializing = _getSerializingSet(ctxt);
        if (serializing.contains(value)) {
            ctxt.defaultSerializeNullValue(g);
            return;
        }
        serializing.add(value);
        try {
            _delegate.serializeWithType(value, g, ctxt, typeSer);
        } finally {
            serializing.remove(value);
        }
    }

    @Override
    public ValueSerializer<T> unwrappingSerializer(NameTransformer unwrapper) {
        // Delegate unwrapping to the inner serializer — cycle detection
        // for unwrapped properties is handled by the parent object's wrapper.
        return _delegate.unwrappingSerializer(unwrapper);
    }

    @Override
    public boolean isEmpty(SerializationContext provider, T value) {
        return _delegate.isEmpty(provider, value);
    }

    @Override
    public boolean isUnwrappingSerializer() {
        return _delegate.isUnwrappingSerializer();
    }

    /**
     * Must reflect the delegate: {@code BeanPropertyWriter._handleSelfReference}
     * calls this on the outermost serializer to decide whether a direct
     * self-reference is already handled by Object Id.
     */
    @Override
    public boolean usesObjectId() {
        return _delegateUsesObjectId;
    }

    @SuppressWarnings("unchecked")
    private Set<Object> _getSerializingSet(SerializationContext ctxt) {
        Set<Object> set = (Set<Object>) ctxt.getAttribute(ATTR_KEY);
        if (set == null) {
            // First call: the underlying ContextAttributes._nonShared map is
            // null, so setAttribute creates a new ContextAttributes instance.
            // All subsequent calls mutate the map in-place, so the delegate
            // (which receives the same SerializationContext) sees our updates.
            set = Collections.newSetFromMap(new IdentityHashMap<>());
            ctxt.setAttribute(ATTR_KEY, set);
        }
        return set;
    }
}
