package com.fasterxml.jackson.datatype.hibernate4;

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
public class UninitializedIdentifierTest extends BaseTest
{
    @Test
    public void testUninitializedIdentifierFeature() throws Exception
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

            mapper = mapperWithModule(Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS, true);
            json = mapper.writeValueAsString(employee);
            assertNotNull(json);
            stuff = mapper.readValue(json, Map.class);
            // "office" should be serialized as identifier
            assertTrue(stuff.containsKey("office"));

            Object officeObj = stuff.get("office");
            assertTrue(officeObj instanceof Map);
            Map<?,?> office = (Map<?,?>) officeObj;

            //make sure there's only one property on office
            assertEquals(1, office.size());
            assertTrue(office.containsKey("officeCode"));
            assertEquals("4", office.get("officeCode"));            
        } finally {
            emf.close();
        }
    }
}
