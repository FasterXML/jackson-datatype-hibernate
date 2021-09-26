package com.fasterxml.jackson.datatype.hibernate5.jakarta;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.datatype.hibernate5.jakarta.PackageVersion;

import jakarta.persistence.Transient;

/**
 * Simple {@link com.fasterxml.jackson.databind.AnnotationIntrospector} that adds support for using
 * {@link jakarta.persistence.Transient} to denote ignorable fields (alongside with Jackson
 * and/or JAXB annotations).
 */
public class Hibernate5JAnnotationIntrospector extends AnnotationIntrospector
{
    private static final long serialVersionUID = 1L;
    
    /**
     * Whether we should check for existence of @Transient or not.
     * Default value is 'true'.
     */
    protected boolean _cfgCheckTransient = true;

    /*
    /**********************************************************************
    /* Construction, configuration
    /**********************************************************************
     */
    
    public Hibernate5JAnnotationIntrospector() { }

    /**
     * Method to call to specify whether @Transient annotation is to be
     * supported; if false, will be ignored, if true, will be used to
     * detect "ignorable" properties.
     */
    public Hibernate5JAnnotationIntrospector setUseTransient(boolean state) {
        _cfgCheckTransient = state;
        return this;
    }

    /**
     * @since 2.5
     */
    public boolean doesUseTransient() {
         return _cfgCheckTransient;
    }

    /*
    /**********************************************************************
    /* Standard method impl/overrides
    /**********************************************************************
     */

    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }

    /*
    /**********************************************************************
    /* Annotation introspection methods
    /**********************************************************************
     */

    @Override
    public boolean hasIgnoreMarker(AnnotatedMember m) {
        return _cfgCheckTransient && m.hasAnnotation(Transient.class);
    }

    @Override
    public Boolean isIgnorableType(AnnotatedClass ac)
    {
        /* 26-Dec-2015, tatu: To fix [datatype-hibernate#72], need to suppress handling
         *  of `FieldHandled`. Not sure if it works without test (alas, none provided),
         *  but will try our best -- problem is, if it'
         */
        // 11-Feb-2016, tatu: As per [datatype-hibernate#86] must use indirection. Sigh.
        Class<?> handlerClass = FieldHandlerChecker.instance.getHandlerClass();
        if (handlerClass != null) {
            if (handlerClass.isAssignableFrom(ac.getAnnotated())) {
                return Boolean.TRUE;
            }
        }
        return null;
    }

    /**
     * Helper class used to encapsulate detection of <code>FieldHandler</code>; class
     * that was part of Hibernate from 4.0 until 5.0, but removed from 5.1.
     */
    final static class FieldHandlerChecker {
        private final static String FIELD_HANDLER_INTERFACE = "org.hibernate.bytecode.internal.javassist.FieldHandler";

        private final Class<?> _handlerClass;

        public final static FieldHandlerChecker instance = new FieldHandlerChecker();

        public FieldHandlerChecker() {
            Class<?> cls = null;
            try {
                cls = Class.forName(FIELD_HANDLER_INTERFACE);
            } catch (Throwable t) { }
            _handlerClass = cls;
        }

        public Class<?> getHandlerClass() {
            return _handlerClass;
        }
    }
}
