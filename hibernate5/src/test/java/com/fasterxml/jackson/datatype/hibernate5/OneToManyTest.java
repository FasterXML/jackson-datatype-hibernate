package com.fasterxml.jackson.datatype.hibernate5;

import java.util.*;

import javax.persistence.OneToMany;

import com.fasterxml.jackson.databind.ObjectMapper;

public class OneToManyTest extends BaseTest
{
    static final String EXPECTED_JSON = "{\"m\":{\"A\":\"A\"}}";

    static final class X {
        @OneToMany
        public final Map<String, String> m = new LinkedHashMap<String, String>();
    }
    
    static final class Y {
        public final Map<String, String> m = new LinkedHashMap<String, String>();
    }
    
    public void testMap() throws Exception {
        Y object = new Y();
        object.m.put("A", "A");
    
        assertEquals(EXPECTED_JSON, mapWithoutHibernateModule(object));
        assertEquals(EXPECTED_JSON, mapWithHibernateModule(object));
    }
    
    public void testMapWithOneToMany() throws Exception {
        X object = new X();
        object.m.put("A", "A");
    
        assertEquals(EXPECTED_JSON, mapWithoutHibernateModule(object));
        assertEquals(EXPECTED_JSON, mapWithHibernateModule(object));
    }
    
    private String mapWithHibernateModule(Object object) throws Exception {
        return new ObjectMapper().registerModule(new Hibernate5Module()).writeValueAsString(object);
    }
    
    private String mapWithoutHibernateModule(Object object) throws Exception {
        return new ObjectMapper().writeValueAsString(object);
    }
}
