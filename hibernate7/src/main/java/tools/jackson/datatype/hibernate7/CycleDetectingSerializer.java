package tools.jackson.datatype.hibernate7;

import java.util.LinkedList;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.util.NameTransformer;

/**
 * Wrapper serializer that detects cyclic object references during
 * serialization by tracking which objects are currently being serialized
 * on the current thread.  When the same object instance is encountered
 * again, {@code null} is written instead of recursing.
 * <p>
 * This addresses infinite recursion in bidirectional JPA entity graphs
 * when {@code FORCE_LAZY_LOADING} is enabled (see [datatype-hibernate#204]).
 */
public class CycleDetectingSerializer<T> extends ValueSerializer<T>
{
    private static final ThreadLocal<LinkedList<Object>> _currentlySerializing =
            ThreadLocal.withInitial(LinkedList::new);

    private final ValueSerializer<T> _delegate;

    public CycleDetectingSerializer(ValueSerializer<T> delegate) {
        _delegate = delegate;
    }

    @Override
    public void serialize(T value, JsonGenerator g, SerializationContext ctxt)
        throws DatabindException
    {
        LinkedList<Object> stack = _currentlySerializing.get();
        // Identity-based check: same object instance on the current path = cycle
        for (Object o : stack) {
            if (o == value) {
                ctxt.defaultSerializeNullValue(g);
                return;
            }
        }
        stack.addLast(value);
        try {
            _delegate.serialize(value, g, ctxt);
        } finally {
            stack.removeLast();
        }
    }

    @Override
    public void serializeWithType(T value, JsonGenerator g, SerializationContext ctxt,
            TypeSerializer typeSer) throws DatabindException
    {
        LinkedList<Object> stack = _currentlySerializing.get();
        for (Object o : stack) {
            if (o == value) {
                ctxt.defaultSerializeNullValue(g);
                return;
            }
        }
        stack.addLast(value);
        try {
            _delegate.serializeWithType(value, g, ctxt, typeSer);
        } finally {
            stack.removeLast();
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
}
