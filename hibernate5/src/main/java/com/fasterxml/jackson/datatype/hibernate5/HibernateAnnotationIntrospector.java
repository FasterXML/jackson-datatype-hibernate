package com.fasterxml.jackson.datatype.hibernate5;

import javax.persistence.Transient;

import org.hibernate.bytecode.internal.javassist.FieldHandler;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;

/**
 * Simple {@link com.fasterxml.jackson.databind.AnnotationIntrospector} that adds support for using
 * {@link javax.persistence.Transient} to denote ignorable fields (alongside with Jackson
 * and/or JAXB annotations).
 */
public class HibernateAnnotationIntrospector extends AnnotationIntrospector
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
    
    public HibernateAnnotationIntrospector() { }

    /**
     * Method to call to specify whether @Transient annotation is to be
     * supported; if false, will be ignored, if true, will be used to
     * detect "ignorable" properties.
     */
    public HibernateAnnotationIntrospector setUseTransient(boolean state) {
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
        return ModuleVersion.instance.version();
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
        // ... could we avoid direct class reference?
        if (FieldHandler.class.isAssignableFrom(ac.getAnnotated())) {
            return Boolean.TRUE;
        }
        return null;
    }
}
