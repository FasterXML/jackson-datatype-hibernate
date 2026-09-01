package tools.jackson.datatype.hibernate7;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.DelegatingSerializer;
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
 * <p>
 * Extends {@link DelegatingSerializer} so that the full {@code ValueSerializer}
 * contract -- {@code resolve()}, {@code createContextual()}, {@code handledType()},
 * {@code getDelegatee()}, {@code properties()}, {@code acceptJsonFormatVisitor()}
 * and the mutant factories -- reaches the wrapped bean serializer.  The modifier
 * wraps <em>every</em> {@code @Entity} bean serializer, so anything not forwarded
 * here is silently dropped for all entity types.
 */
public class CycleDetectingSerializer extends DelegatingSerializer
{
    private static final Object ATTR_KEY = CycleDetectingSerializer.class;

    /**
     * Whether the delegate handles cycles itself via Object Ids
     * (that is, the entity is annotated with {@code @JsonIdentityInfo}).
     */
    private final boolean _delegateUsesObjectId;

    public CycleDetectingSerializer(ValueSerializer<?> delegate) {
        super(delegate);
        _delegateUsesObjectId = delegate.usesObjectId();
    }

    @Override
    protected ValueSerializer<Object> newDelegatingInstance(ValueSerializer<?> newDelegatee) {
        return new CycleDetectingSerializer(newDelegatee);
    }

    /**
     * Returns the delegate's unwrapping serializer undecorated, deliberately
     * overriding {@link DelegatingSerializer}'s re-wrapping behaviour: an
     * unwrapping serializer writes no field name, so it has no place to put a
     * {@code null} cycle placeholder, and wrapping here would only replace a
     * legitimate second occurrence of the value with {@code null}.
     *<p>
     * Skipping tracking for the unwrapped hop is safe as long as the cycle
     * contains at least one regular (non-unwrapped) hop, since the next entity
     * reached as a regular property is tracked by its own wrapper.  A cycle in
     * which <i>every</i> hop is {@code @JsonUnwrapped} is not tracked at all
     * and still recurses without bound; that is a known limitation rather than
     * a regression, as Jackson fails the same way without this module.
     *<p>
     * When the delegate has no unwrapped variant it returns itself, and this
     * wrapper is kept: nothing was unwrapped, so cycle detection still applies.
     */
    @Override
    public ValueSerializer<Object> unwrappingSerializer(NameTransformer unwrapper) {
        ValueSerializer<Object> unwrapping = _delegatee.unwrappingSerializer(unwrapper);
        if (unwrapping == _delegatee) {
            return this;
        }
        return unwrapping;
    }

    @Override
    public void serialize(Object value, JsonGenerator g, SerializationContext ctxt)
    {
        // Entities using @JsonIdentityInfo already handle their own cycles
        if (_delegateUsesObjectId) {
            _delegatee.serialize(value, g, ctxt);
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
            _delegatee.serialize(value, g, ctxt);
        } finally {
            serializing.remove(value);
        }
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator g, SerializationContext ctxt,
            TypeSerializer typeSer)
    {
        if (_delegateUsesObjectId) {
            _delegatee.serializeWithType(value, g, ctxt, typeSer);
            return;
        }
        Set<Object> serializing = _getSerializingSet(ctxt);
        if (serializing.contains(value)) {
            ctxt.defaultSerializeNullValue(g);
            return;
        }
        serializing.add(value);
        try {
            _delegatee.serializeWithType(value, g, ctxt, typeSer);
        } finally {
            serializing.remove(value);
        }
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
