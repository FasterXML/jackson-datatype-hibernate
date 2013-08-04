package com.fasterxml.jackson.datatype.hibernate4;

import com.fasterxml.jackson.core.Version;

import com.fasterxml.jackson.databind.*;
import org.hibernate.engine.spi.Mapping;

public class Hibernate4Module extends Module
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
         * Whether {@link javax.persistence.Transient} annotation should be checked or not; if true, will consider
         * @Transient to mean that property is to be ignored; if false annotation will
         * have no effect.
         *<p>
         * Default value is true.
         */
        USE_TRANSIENT_ANNOTATION(true),
        
	    /**
	     * If FORCE_LAZY_LOADING is false lazy-loaded object should be serialized as map IdentifierName=>IdentifierValue
	     * instead of null (true); or serialized as nulls (false)
	     * <p>
	     * Default value is false
	     */
        SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS(false)        
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

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public Hibernate4Module() {
        this(null);
    }

    public Hibernate4Module(Mapping mapping) {
        _mapping = mapping;
    }

    @Override public String getModuleName() { return "jackson-datatype-hibernate"; }
    @Override public Version version() { return ModuleVersion.instance.version(); }
    
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
        boolean forceLoading = isEnabled(Feature.FORCE_LAZY_LOADING);
        context.addSerializers(new HibernateSerializers(forceLoading, isEnabled(Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS), _mapping));
        context.addBeanSerializerModifier(new HibernateSerializerModifier(forceLoading));
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

    public Hibernate4Module enable(Feature f) {
        _moduleFeatures |= f.getMask();
        return this;
    }

    public Hibernate4Module disable(Feature f) {
        _moduleFeatures &= ~f.getMask();
        return this;
    }

    public final boolean isEnabled(Feature f) {
        return (_moduleFeatures & f.getMask()) != 0;
    }

    public Hibernate4Module configure(Feature f, boolean state) {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
    }

}