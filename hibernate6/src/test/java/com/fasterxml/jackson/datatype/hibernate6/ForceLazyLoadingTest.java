package com.fasterxml.jackson.datatype.hibernate6;

import java.util.Map;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.hibernate.Hibernate;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.datatype.hibernate6.data.Customer;
import com.fasterxml.jackson.datatype.hibernate6.data.Payment;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

public class ForceLazyLoadingTest extends BaseTest
{
    // [Issue#15]
    @Test
    public void testGetCustomerJson() throws Exception
    {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("persistenceUnit");

        try {
            EntityManager em = emf.createEntityManager();
            
            // false -> no forcing of lazy loading
            ObjectMapper mapper = mapperWithModule(true);
            
            Customer customer = em.find(Customer.class, 103);
            assertFalse(Hibernate.isInitialized(customer.getPayments()));
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(customer);
            // should force loading...
            Set<Payment> payments = customer.getPayments();
            /*
            System.out.println("--- JSON ---");
            System.out.println(json);
            System.out.println("--- /JSON ---");
            */

            assertTrue(Hibernate.isInitialized(payments));
            // TODO: verify
            assertNotNull(json);

            Map<?,?> stuff = mapper.readValue(json, Map.class);

            assertTrue(stuff.containsKey("payments"));
            assertTrue(stuff.containsKey("orders"));
            assertNull(stuff.get("orderes"));
            
        } finally {
            emf.close();
        }
    }
}
