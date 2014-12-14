package com.fasterxml.jackson.datatype.hibernate4;

import java.io.IOException;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.Hibernate;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate4.Hibernate4Module.Feature;
import com.fasterxml.jackson.datatype.hibernate4.data.Employee;

/**
 * Test Hibernate4Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS
 */
public class AlwaysSerializeIdentifierTest extends BaseTest
{
    @Test
    public void testAlwaysSerializeAsIdentifierFeature() throws Exception
    {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("persistenceUnit");

        try {
            EntityManager em = emf.createEntityManager();
           
            Employee employee = em.find(Employee.class, 1370);
            // "office" is marked as lazily loaded
            assertFalse(Hibernate.isInitialized(employee.getOffice()));

            ObjectMapper mapper = mapperWithModule();            
            String json = mapper.writeValueAsString(employee);
            assertNotNull(json);
            Map<?,?> stuff = mapper.readValue(json, Map.class);
            // "office" should be serialized as null
            assertTrue(stuff.containsKey("office"));
            assertNull(stuff.get("office"));

            //"office" should be serialized as an identifier when uninitialized
            mapper = mapperWithModule(Feature.ALWAYS_SERIALIZE_LAZY_LOADED_OBJECTS_AS_IDENTIFIER, true);
            verifySerializedAsIdentifier(employee, mapper);

            //"office" should be serialized as an identifier even when already initialized
            Hibernate.initialize(employee.getOffice());
            assertTrue(Hibernate.isInitialized(employee.getOffice()));
            verifySerializedAsIdentifier(employee, mapper);
        } finally {
            emf.close();
        }
    }

    private void verifySerializedAsIdentifier(Employee employee, ObjectMapper mapper) throws IOException {
        String json = mapper.writeValueAsString(employee);
        assertNotNull(json);
        Map<?,?> stuff = mapper.readValue(json, Map.class);
        // "office" should be serialized as identifier
        assertTrue(stuff.containsKey("office"));

        Object officeObj = stuff.get("office");
        assertTrue(officeObj instanceof Map);
        Map<?,?> office = (Map<?,?>) officeObj;

        //make sure there's only one property on office
        assertEquals(1, office.size());
        assertTrue(office.containsKey("officeCode"));
        assertEquals("4", office.get("officeCode"));          
    }
}
