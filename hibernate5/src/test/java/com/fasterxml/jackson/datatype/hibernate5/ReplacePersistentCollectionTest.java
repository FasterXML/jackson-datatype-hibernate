package com.fasterxml.jackson.datatype.hibernate5;

import java.util.*;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.hibernate5.data.Customer;
import com.fasterxml.jackson.datatype.hibernate5.data.Payment;
import org.hibernate.Hibernate;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class ReplacePersistentCollectionTest extends BaseTest
{
    private EntityManagerFactory emf;

    private EntityManager em;

    @Override
    @Before
    public void setUp() throws Exception {
        emf = Persistence.createEntityManagerFactory("persistenceUnit");
        em = emf.createEntityManager();
    }

    @After
    @Override
    public void tearDown() throws Exception {
		em.close();
		emf.close();
    }

    // [Issue#93], backwards compatible case
    @Test
    public void testNoReplacePersistentCollection() throws Exception {
		final ObjectMapper mapper = hibernateMapper(new Hibernate5Module()
						.configure(Hibernate5Module.Feature.FORCE_LAZY_LOADING, true)
				);

        Customer customer = em.find(Customer.class, 103);
        Assert.assertFalse(Hibernate.isInitialized(customer.getPayments()));
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(customer);
        Assert.assertTrue(json.contains("org.hibernate.collection"));
        // should force loading...
        Set<Payment> payments = customer.getPayments();
        Assert.assertTrue(Hibernate.isInitialized(payments));
 
        try {
            /*Customer result =*/ mapper.readValue(json, Customer.class);
            fail("Should throw exception");
        } catch (JsonMappingException e) {
            verifyException(e, "failed to lazily initialize");
        }
    }

    // [Issue#93], backwards compatible case
    @Test
    public void testReplacePersistentCollection() throws Exception {
		final ObjectMapper mapper = hibernateMapper(new Hibernate5Module()
						.configure(Hibernate5Module.Feature.FORCE_LAZY_LOADING, true)
						.configure(Hibernate5Module.Feature.REPLACE_PERSISTENT_COLLECTIONS, true)
						);

		Customer customer = em.find(Customer.class, 103);
		Assert.assertFalse(Hibernate.isInitialized(customer.getPayments()));
		String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(customer);
		Assert.assertFalse(json.contains("org.hibernate.collection"));
		// should force loading...
		Set<Payment> payments = customer.getPayments();

		Assert.assertTrue(Hibernate.isInitialized(payments));
		Customer cust = mapper.readValue(json, Customer.class);
		assertNotNull(cust);

		// For debugging?
		/*
		Map<?, ?> stuff = mapper.readValue(json, Map.class);

		Assert.assertTrue(stuff.containsKey("payments"));
		Assert.assertTrue(stuff.containsKey("orders"));
		Assert.assertNull(stuff.get("orderes"));
		*/
    }

    private ObjectMapper hibernateMapper(Hibernate5Module module) {
        return JsonMapper.builder()
                .addModule(module)
                .build()
                .enableDefaultTyping(DefaultTyping.NON_FINAL);
    }
}
