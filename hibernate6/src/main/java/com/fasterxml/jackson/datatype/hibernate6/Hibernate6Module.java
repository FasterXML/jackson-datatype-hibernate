package com.fasterxml.jackson.datatype.hibernate6;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.Mapping;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.AnnotationIntrospector;

public class Hibernate6Module extends com.fasterxml.jackson.databind.Module
{
    /**
     * Enumeration that defines all toggleable features this module
     */
    public enum Feature {
        /**
         * Whether lazy-loaded object should be forced to be loaded and then serialized
         * (true); or serialized as nulls (false).
         *<p>
         * Default value is false.
         */
        FORCE_LAZY_LOADING(false),

        /**
         * Whether {@link jakarta.persistence.Transient} annotation should be checked or not;
         * if true, will consider {@code @Transient} to mean that property is to be ignored;
         * if false annotation will have no effect.
         *<p>
         * Default value is true.
         */
        USE_TRANSIENT_ANNOTATION(true),
        
	    /**
	     * If FORCE_LAZY_LOADING is false, this feature serializes uninitialized lazy loading proxies as
	     * <code>{"identifierName":"identifierValue"}</code> rather than <code>null</code>. 
	     * <p>
         * Default value is false.
	     * <p>
	     * Note that the name of the identifier property can only be determined if 
	     * <ul>
	     * <li>the {@link Mapping} is provided to the Hibernate5Module, or </li>
	     * <li>the persistence context that loaded the proxy has not yet been closed, or</li> 
	     * <li>the id property is mapped with property access (for instance because the {@code @Id}
	     * annotation is applied to a method rather than a field)</li>
	     * </ul>
	     * Otherwise, the entity name will be used instead. 
	     */
        SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS(false),

        /**
         * This feature determines how {@link org.hibernate.collection.spi.PersistentCollection}s properties
         * for which no annotation is found are handled with respect to
         * lazy-loading: if true, lazy-loading is only assumed if annotation
         * is used to indicate that; if false, lazy-loading is assumed to be
         * the default.
         * Note that {@link #FORCE_LAZY_LOADING} has priority over this Feature;
         * meaning that if it is defined as true, setting of this Feature has no
         * effect.
          * <p>
          * Default value is false, meaning that laziness is considered to be the
          * default value.
         * 
         * @since 2.4
         */
        REQUIRE_EXPLICIT_LAZY_LOADING_MARKER(false),

        /**
         * Feature that may be enabled to force
         * replacement <code>org.hibernate.collection.spi.PersistentCollection</code>,
         * <code>List</code>, <code>Set</code>, <code>Map</code> subclasses
         * during serialization as standard JDK {@link java.util.List},
         * {@link java.util.Set} and {@link java.util.Map}.
         * This is usually done to prevent issues with polymorphic handling, so
         * that type id is generated for standard containers and NOT for Hibernate
         * variants.
         * <p>
         * Default setting is false, so that no replacement occurs.
         *
         * @since 2.8.2
         */
        REPLACE_PERSISTENT_COLLECTIONS(false),
        
        /**
         * Using {@link #FORCE_LAZY_LOADING} may result in
         * `jakarta.persistence.EntityNotFoundException`. This flag configures Jackson to
         * ignore the error and serialize a `null`.
         *
         * @since 2.10
         */
        WRITE_MISSING_ENTITIES_AS_NULL(false),

        /**
         * Feature that may be disables to unwrap the identifier
         * of the serialized entity, returning a value instead of
         * an object.
         *
         * @since 2.12
         */
        WRAP_IDENTIFIER_IN_OBJECT(true)
        ;

        final boolean _defaultState;
        final int _mask;

        /**
         * Method that calculates bit set (flags) of all features that
         * are enabled by default.
         */
        public static int collectDefaults()
        {
            int flags = 0;
            for (Feature f : values()) {
                if (f.enabledByDefault()) {
                    flags |= f.getMask();
                }
            }
            return flags;
        }
        
        private Feature(boolean defaultState) {
            _defaultState = defaultState;
            _mask = (1 << ordinal());
        }

        public boolean enabledIn(int flags) { return (flags & _mask) != 0; }
        public boolean enabledByDefault() { return _defaultState; }
        public int getMask() { return _mask; }
    }

    protected final static int DEFAULT_FEATURES = Feature.collectDefaults();
    
    /**
     * Bit flag composed of bits that indicate which
     * {@link Feature}s
     * are enabled.
     */
    protected int _moduleFeatures = DEFAULT_FEATURES;

    /**
     * Hibernate mapping.
     */
    protected final Mapping _mapping;

    protected final SessionFactory _sessionFactory;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public Hibernate6Module() {
        this(null, null);
    }

    public Hibernate6Module(Mapping mapping) {
        this(mapping, null);
    }

    public Hibernate6Module(SessionFactory sessionFactory) {
        this(null, sessionFactory);
    }

    public Hibernate6Module(Mapping mapping, SessionFactory sessionFactory) {
        _sessionFactory = sessionFactory;
        _mapping = mapping;
    }

    @Override public String getModuleName() { return "jackson-datatype-hibernate"; }
    @Override public Version version() { return PackageVersion.VERSION; }

    @Override
    public void setupModule(SetupContext context)
    {
        /* First, append annotation introspector (no need to override, esp.
         * as we just implement couple of methods)
         */
        // Then add serializers we need
        AnnotationIntrospector ai = annotationIntrospector();
        if (ai != null) {
            context.appendAnnotationIntrospector(ai);
        }
        context.addSerializers(new Hibernate6Serializers(_mapping, _moduleFeatures));
        context.addBeanSerializerModifier(new Hibernate6SerializerModifier(_moduleFeatures, _sessionFactory));
    }

    /**
     * Method called during {@link #setupModule}, to create {@link AnnotationIntrospector}
     * to register along with module. If null is returned, no introspector is added.
     */
    protected AnnotationIntrospector annotationIntrospector() {
        Hibernate6AnnotationIntrospector ai = new Hibernate6AnnotationIntrospector();
        ai.setUseTransient(isEnabled(Feature.USE_TRANSIENT_ANNOTATION));
        return ai;
    }
    
    /*
    /**********************************************************************
    /* Extended API, configuration
    /**********************************************************************
     */

    public Hibernate6Module enable(Feature f) {
        _moduleFeatures |= f.getMask();
        return this;
    }

    public Hibernate6Module disable(Feature f) {
        _moduleFeatures &= ~f.getMask();
        return this;
    }

    public final boolean isEnabled(Feature f) {
        return (_moduleFeatures & f.getMask()) != 0;
    }

    public Hibernate6Module configure(Feature f, boolean state) {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
    }

}
