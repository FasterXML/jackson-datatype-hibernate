package com.fasterxml.jackson.datatype.hibernate4;


import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.Mapping;

public class Hibernate4Module extends Module {
	/**
	 * Enumeration that defines all toggleable features this module
	 */
	public enum Feature {
		/**
		 * Whether lazy-loaded object should be forced to be loaded and then
		 * serialized (true); or serialized as nulls (false).
		 * <p>
		 * Default value is false.
		 */
		FORCE_LAZY_LOADING(false),

		/**
		 * Whether {@link javax.persistence.Transient} annotation should be
		 * checked or not; if true, will consider @Transient to mean that
		 * property is to be ignored; if false annotation will have no effect.
		 * <p>
		 * Default value is true.
		 */
		USE_TRANSIENT_ANNOTATION(true),

		/**
		 * If FORCE_LAZY_LOADING is false lazy-loaded object should be
		 * serialized as map IdentifierName=>IdentifierValue instead of null
		 * (true); or serialized as nulls (false)
		 * <p>
		 * Default value is false.
		 */
		SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS(false),

		/**
		 * This feature determines how
		 * {@link org.hibernate.collection.spi.PersistentCollection}s properties
		 * for which no annotation is found are handled with respect to
		 * lazy-loading: if true, lazy-loading is only assumed if annotation is
		 * used to indicate that; if false, lazy-loading is assumed to be the
		 * default. Note that {@link #FORCE_LAZY_LOADING} has priority over this
		 * Feature; meaning that if it is defined as true, setting of this
		 * Feature has no effect.
		 * <p>
		 * Default value is false, meaning that laziness is considered to be the
		 * default value.
		 * 
		 * @since 2.4
		 */
		REQUIRE_EXPLICIT_LAZY_LOADING_MARKER(false), ;

		final boolean _defaultState;
		final int _mask;

		/**
		 * Method that calculates bit set (flags) of all features that are
		 * enabled by default.
		 */
		public static int collectDefaults() {
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

		public boolean enabledIn(int flags) {
			return (flags & _mask) != 0;
		}

		public boolean enabledByDefault() {
			return _defaultState;
		}

		public int getMask() {
			return _mask;
		}
	}

	protected final static int DEFAULT_FEATURES = Feature.collectDefaults();

	/**
	 * Bit flag composed of bits that indicate which {@link Feature}s are
	 * enabled.
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

    public Hibernate4Module() {
        this(null, null);
    }

    public Hibernate4Module(Mapping mapping) {
        this(mapping, null);
    }

    public Hibernate4Module(SessionFactory sessionFactory) {
        this(null, sessionFactory);
    }

    public Hibernate4Module(Mapping mapping, SessionFactory sessionFactory) {
        _sessionFactory = sessionFactory;
        _mapping = mapping;
    }

    @Override public String getModuleName() { return "jackson-datatype-hibernate"; }
    @Override public Version version() { return ModuleVersion.instance.version(); }


	@Override
	public void setupModule(SetupContext context) {
		/*
		 * First, append annotation introspector (no need to override, esp. as
		 * we just implement couple of methods)
		 */
		// Then add serializers we need
		AnnotationIntrospector ai = annotationIntrospector();
		if (ai != null) {
			context.appendAnnotationIntrospector(ai);
		}
		ObjectMapper objectMapper = ((ObjectMapper) context.getOwner());
		DefaultSerializerProvider defaultSerializerProvider = ((DefaultSerializerProvider) objectMapper
				.getSerializerProvider());
		context.addSerializers(new HibernateSerializers(
				defaultSerializerProvider.createInstance(
						objectMapper.getSerializationConfig(),
						objectMapper.getSerializerFactory()), _mapping,
				_moduleFeatures));
		context.addBeanSerializerModifier(new HibernateSerializerModifier(
				_moduleFeatures, _sessionFactory));
	}

	/**
	 * Method called during {@link #setupModule}, to create
	 * {@link AnnotationIntrospector} to register along with module. If null is
	 * returned, no introspector is added.
	 */
	protected AnnotationIntrospector annotationIntrospector() {
		HibernateAnnotationIntrospector ai = new HibernateAnnotationIntrospector();
		ai.setUseTransient(isEnabled(Feature.USE_TRANSIENT_ANNOTATION));
		return ai;
	}

	/*
	 * /**********************************************************************
	 * /* Extended API, configuration
	 * /**********************************************************************
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