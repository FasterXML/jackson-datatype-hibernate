package com.fasterxml.jackson.datatype.hibernate4;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.Hibernate;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate4.data.Customer;

import static org.junit.jupiter.api.Assertions.*;

// [Issue#125]
public class MissingEntitiesAsNullTest extends BaseTest {
    @Test
    public void testMissingProductWhenMissing() throws Exception {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("persistenceUnit");

        try {
            EntityManager em = emf.createEntityManager();

            // false -> no forcing of lazy loading
            ObjectMapper mapper = mapperWithModule(true);

            Customer customer = em.find(Customer.class, 103);
            assertFalse(Hibernate.isInitialized(customer.getPayments()));
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(customer);
            assertNull(customer.getMissingProductCode());
            assertNotNull(json);

            Map<?, ?> stuff = mapper.readValue(json, Map.class);

            assertNull(stuff.get("missingProductCode"));
            assertNull(stuff.get("missingProduct"));

        } finally {
            emf.close();
        }
    }

    @Test
    public void testProductWithValidForeignKey() throws Exception {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("persistenceUnit");

        try {
            EntityManager em = emf.createEntityManager();

            // false -> no forcing of lazy loading
            ObjectMapper mapper = mapperWithModule(true);

            Customer customer = em.find(Customer.class, 500);
            assertFalse(Hibernate.isInitialized(customer.getPayments()));
            assertNotNull(customer.getMissingProductCode());
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(customer);
            assertNotNull(json);

            Map<?, ?> stuff = mapper.readValue(json, Map.class);

            assertNotNull(stuff.get("missingProductCode"));
            assertNotNull(stuff.get("missingProduct"));

        } finally {
            emf.close();
        }
    }

    // caused by javax.persistence.EntityNotFoundException: Unable to find
    // com.fasterxml.jackson.datatype.hibernate4.data.Product with id X10_1678
    @Test
    public void testExceptionWithInvalidForeignKey() throws Exception {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("persistenceUnit");

        try {
            EntityManager em = emf.createEntityManager();

            // false -> no forcing of lazy loading
            ObjectMapper mapper = mapperWithModule(true);

            Customer customer = em.find(Customer.class, 501);
            assertFalse(Hibernate.isInitialized(customer.getPayments()));

            // javax.persistence.EntityNotFoundException thrown here
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(customer);
            // JUnit 3.8
            fail("Expected EntityNotFoundException exception");

        } catch (JsonMappingException e) {
            assertEquals("Unable to find com.fasterxml.jackson.datatype.hibernate4.data.Product with id X10_1678", e.getCause().getMessage());
        } finally {
            emf.close();
        }
    }

    @Test
    public void testWriteAsNullWithInvalidForeignKey() throws Exception {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("persistenceUnit");

        try {
            EntityManager em = emf.createEntityManager();

            // false -> no forcing of lazy loading
            ObjectMapper mapper = mapperWithModule(true, true);

            Customer customer = em.find(Customer.class, 501);
            assertFalse(Hibernate.isInitialized(customer.getPayments()));
            // javax.persistence.EntityNotFoundException thrown here
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(customer);
            assertNotNull(json);

            Map<?, ?> stuff = mapper.readValue(json, Map.class);

            assertNotNull(stuff.get("missingProductCode"));
            assertNull(stuff.get("missingProduct"));

        } finally {
            emf.close();
        }
    }
}
