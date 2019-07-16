package com.fasterxml.jackson.datatype.hibernate3;

import com.fasterxml.jackson.core.Version;

import com.fasterxml.jackson.databind.*;
import org.hibernate.SessionFactory;

public class Hibernate3Module extends com.fasterxml.jackson.databind.Module
{
    /**
     * Enumeration that defines all togglable features this module
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
         * Whether {@link javax.persistence.Transient} annotation should be checked or not; if true, will consider
         * {@code @Transient} to mean that property is to be ignored; if false annotation will
         * have no effect.
         *<p>
         * Default value is true.
         */
        USE_TRANSIENT_ANNOTATION(true),

        /**
         * This feature determines how {@link org.hibernate.collection.PersistentCollection}s properties
         * for which no annotation is found are handled with respect to
         * lazy-loading: if true, lazy-loading is only assumed if annotation
         * is used to indicate that; if false, lazy-loading is assumed to be
         * the default.
         * Note that {@link #FORCE_LAZY_LOADING} has priority over this Feature;
         * meaning that if it is defined as true, setting of this Feature has no
         * effect.
         * <p>
         * Default value is false, meaning that laziness is assumed by default,
         * without requiring marker.
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
        REPLACE_PERSISTENT_COLLECTIONS(false)
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

    protected final SessionFactory _sessionFactory;
    
    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */
    
    public Hibernate3Module() {
        this(null);
    }

    public Hibernate3Module(SessionFactory sessionFactory) {
        _sessionFactory = sessionFactory;
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
        context.addSerializers(new HibernateSerializers(_moduleFeatures));
        context.addBeanSerializerModifier(new HibernateSerializerModifier(_moduleFeatures, _sessionFactory));
    }

    /**
     * Method called during {@link #setupModule}, to create {@link AnnotationIntrospector}
     * to register along with module. If null is returned, no introspector is added.
     */
    protected AnnotationIntrospector annotationIntrospector() {
        HibernateAnnotationIntrospector ai = new HibernateAnnotationIntrospector();
        ai.setUseTransient(isEnabled(Feature.USE_TRANSIENT_ANNOTATION));
        return ai;
    }
    
    /*
    /**********************************************************************
    /* Extended API, configuration
    /**********************************************************************
     */

    public Hibernate3Module enable(Feature f) {
        _moduleFeatures |= f.getMask();
        return this;
    }

    public Hibernate3Module disable(Feature f) {
        _moduleFeatures &= ~f.getMask();
        return this;
    }

    public final boolean isEnabled(Feature f) {
        return (_moduleFeatures & f.getMask()) != 0;
    }

    public Hibernate3Module configure(Feature f, boolean state) {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
    }

}