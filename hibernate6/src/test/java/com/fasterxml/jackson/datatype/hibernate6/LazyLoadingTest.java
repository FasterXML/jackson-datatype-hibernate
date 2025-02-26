package com.fasterxml.jackson.datatype.hibernate6;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.datatype.hibernate6.data.Customer;
import com.fasterxml.jackson.datatype.hibernate6.data.Payment;
import org.hibernate.Hibernate;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module.Feature;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class LazyLoadingTest extends BaseTest
{
    // For [#15]
    @Test
    public void testGetCustomerJson() throws Exception
    {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("persistenceUnit");

        try {
            EntityManager em = emf.createEntityManager();
            
            // false -> no forcing of lazy loading
            ObjectMapper mapper = mapperWithModule(false);
            
            Customer customer = em.find(Customer.class, 103);
            assertFalse(Hibernate.isInitialized(customer.getPayments()));
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(customer);
            // should not force loading...
            Set<Payment> payments = customer.getPayments();

            //System.out.println("--- JSON ---");
            //System.out.println(json);
            //System.out.println("--- /JSON ---");

            assertFalse(Hibernate.isInitialized(payments));
            // TODO: verify
            assertNotNull(json);
            
            Map<?,?> stuff = mapper.readValue(json, Map.class);

            // "payments" is marked as lazily loaded AND "Include.NON_EMPTY"; should not be serialized
            if (stuff.containsKey("payments")) {
                fail("Should not find serialized property 'payments'; got: "+stuff.get("payments")
                        +" from JSON: "+json);
            }
            // orders, on the other hand, not:
            assertTrue(stuff.containsKey("orders"));
            assertNull(stuff.get("orderes"));
            
        } finally {
            emf.close();
        }
    }
    
    @Test
    public void testSerializeIdentifierFeature() throws JsonProcessingException {
    	Hibernate6Module module = new Hibernate6Module();
		module.enable(Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS);
		ObjectMapper objectMapper = new ObjectMapper().registerModule(module);

		EntityManagerFactory emf = Persistence.createEntityManagerFactory("persistenceUnit");
    	try {
    		EntityManager em = emf.createEntityManager();
    		Customer customerRef = em.getReference(Customer.class, 103);
    		em.close();
    		assertFalse(Hibernate.isInitialized(customerRef));
    		
			String json = objectMapper.writeValueAsString(customerRef);
			assertFalse(Hibernate.isInitialized(customerRef));
			assertEquals("{\"customerNumber\":103}", json);
    	} finally {
    		emf.close();
    	}
    }
}
