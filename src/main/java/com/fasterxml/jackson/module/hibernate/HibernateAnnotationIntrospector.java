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
    @Override
    public boolean isHandled(Annotation a)
    {
        // We only care for one single type, for now:
        return (a.annotationType() == Transient.class);
    }

    public boolean isIgnorableConstructor(AnnotatedConstructor c)
    {
        return c.hasAnnotation(Transient.class);
    }

    public boolean isIgnorableField(AnnotatedField f)
    {
        return f.hasAnnotation(Transient.class);
    }

    public boolean isIgnorableMethod(AnnotatedMethod m) 
    {
        return m.hasAnnotation(Transient.class);
    }
}
