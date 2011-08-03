package com.fasterxml.jackson.module.hibernate;

import java.lang.annotation.Annotation;
import javax.persistence.Transient;

import org.codehaus.jackson.map.introspect.AnnotatedConstructor;
import org.codehaus.jackson.map.introspect.AnnotatedField;
import org.codehaus.jackson.map.introspect.AnnotatedMethod;
import org.codehaus.jackson.map.introspect.NopAnnotationIntrospector;

/**
 * Simple {@link org.codehaus.jackson.map.AnnotationIntrospector} that adds support for using
 * {@link Transient} to denote ignorable fields (alongside with Jackson
 * and/or JAXB annotations).
 */
public class HibernateAnnotationIntrospector extends NopAnnotationIntrospector
{
    /**
     * Whether we should check for existence of @Transient or not.
     * Default value is 'true'.
     */
    protected boolean _cfgCheckTransient;

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
    
    /*
    /**********************************************************************
    /* AnnotationIntrospector implementation/overrides
    /**********************************************************************
     */
    
    @Override
    public boolean isHandled(Annotation a)
    {
        // We only care for one single type, for now:
        return (a.annotationType() == Transient.class);
    }

    public boolean isIgnorableConstructor(AnnotatedConstructor c)
    {
        return _cfgCheckTransient && c.hasAnnotation(Transient.class);
    }

    public boolean isIgnorableField(AnnotatedField f)
    {
        return _cfgCheckTransient && f.hasAnnotation(Transient.class);
    }

    public boolean isIgnorableMethod(AnnotatedMethod m) 
    {
        return _cfgCheckTransient && m.hasAnnotation(Transient.class);
    }
}
