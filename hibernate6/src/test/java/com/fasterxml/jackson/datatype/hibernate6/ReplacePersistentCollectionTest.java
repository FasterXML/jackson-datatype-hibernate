package com.fasterxml.jackson.datatype.hibernate6;

import java.util.Set;

import com.fasterxml.jackson.datatype.hibernate6.data.Customer;
import com.fasterxml.jackson.datatype.hibernate6.data.Payment;
import org.hibernate.Hibernate;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

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
		final ObjectMapper mapper = new ObjectMapper()
				.registerModule(new Hibernate6Module()
						.configure(Hibernate6Module.Feature.FORCE_LAZY_LOADING, true)
				).enableDefaultTyping(DefaultTyping.NON_FINAL);

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
		final ObjectMapper mapper = new ObjectMapper()
				.registerModule(new Hibernate6Module()
						.configure(Hibernate6Module.Feature.FORCE_LAZY_LOADING, true)
						.configure(Hibernate6Module.Feature.REPLACE_PERSISTENT_COLLECTIONS, true)
				        ).enableDefaultTyping(DefaultTyping.NON_FINAL);

		Customer customer = em.find(Customer.class, 103);
		Assert.assertFalse(Hibernate.isInitialized(customer.getPayments()));
		String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(customer);
		Assert.assertFalse(json.contains("org.hibernate.collection"));
		// should force loading...
		Set<Payment> payments = customer.getPayments();

		Assert.assertTrue(Hibernate.isInitialized(payments));
		Customer stuff = mapper.readValue(json, Customer.class);
		assertNotNull(stuff);

//		Map<?, ?> stuff = mapper.readValue(json, Map.class);
//
//		Assert.assertTrue(stuff.containsKey("payments"));
//		Assert.assertTrue(stuff.containsKey("orders"));
//		Assert.assertNull(stuff.get("orderes"));
    }
}
