package com.fasterxml.jackson.datatype.hibernate5.jakarta;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.OneToMany;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @Test
    public void testMap() throws Exception {
        Y object = new Y();
        object.m.put("A", "A");
    
        assertEquals(EXPECTED_JSON, mapWithoutHibernateModule(object));
        assertEquals(EXPECTED_JSON, mapWithHibernateModule(object));
    }

    @Test
    public void testMapWithOneToMany() throws Exception {
        X object = new X();
        object.m.put("A", "A");
    
        assertEquals(EXPECTED_JSON, mapWithoutHibernateModule(object));
        assertEquals(EXPECTED_JSON, mapWithHibernateModule(object));
    }
    
    private String mapWithHibernateModule(Object object) throws Exception {
        return new ObjectMapper().registerModule(new Hibernate5JakartaModule()).writeValueAsString(object);
    }
    
    private String mapWithoutHibernateModule(Object object) throws Exception {
        return new ObjectMapper().writeValueAsString(object);
    }
}
