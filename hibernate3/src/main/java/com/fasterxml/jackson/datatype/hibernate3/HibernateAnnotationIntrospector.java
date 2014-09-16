package com.fasterxml.jackson.datatype.hibernate3;

import javax.persistence.Transient;

import com.fasterxml.jackson.core.Version;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.*;

/**
 * Simple {@link AnnotationIntrospector} that adds support for using
 * {@link Transient} to denote ignorable fields (alongside with Jackson
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

    @Override
    public Version version() {
        return ModuleVersion.instance.version();
    }
    
    /*
    /**********************************************************************
    /* AnnotationIntrospector implementation/overrides
    /**********************************************************************
     */

    @Override
    public boolean hasIgnoreMarker(AnnotatedMember m) {
        return _cfgCheckTransient && m.hasAnnotation(Transient.class);
    }
}
