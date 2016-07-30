package com.fasterxml.jackson.datatype.hibernate5;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;
import java.util.Set;

public class ReplacePersistentCollectionTest {

    private EntityManagerFactory emf;

	private EntityManager em;

    @Before
    public void setUp() throws Exception {
        emf = Persistence.createEntityManagerFactory("persistenceUnit");
		em = emf.createEntityManager();
	}

    @After
    public void tearDown() throws Exception {
		em.close();
		emf.close();
    }

    // [Issue#93], backwards compatible case
    @Test
    public void testNoReplacePersistentCollection() throws Exception {
		final ObjectMapper mapper = new ObjectMapper()
				.registerModule(new Hibernate5Module()
						.configure(Hibernate5Module.Feature.FORCE_LAZY_LOADING, true)
				).enableDefaultTyping();

        Customer customer = em.find(Customer.class, 103);
        Assert.assertFalse(Hibernate.isInitialized(customer.getPayments()));
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(customer);
        Assert.assertTrue(json.contains("org.hibernate.collection"));
        // should force loading...
        Set<Payment> payments = customer.getPayments();
                        /*
                        System.out.println("--- JSON ---");
                        System.out.println(json);
                        System.out.println("--- /JSON ---");
                        */

        Assert.assertTrue(Hibernate.isInitialized(payments));
        // TODO: verify
        Assert.assertNotNull(json);

        boolean exceptionThrown = false;
        try {
            Map<?, ?> stuff = mapper.readValue(json, Map.class);
        } catch (JsonMappingException e) {
            exceptionThrown = true;
        }
        Assert.assertTrue(exceptionThrown);
    }

    // [Issue#93], backwards compatible case
    @Test
    public void testReplacePersistentCollection() throws Exception {
		final ObjectMapper mapper = new ObjectMapper()
				.registerModule(new Hibernate5Module()
						.configure(Hibernate5Module.Feature.FORCE_LAZY_LOADING, true)
						.configure(Hibernate5Module.Feature.REPLACE_PERSISTENT_COLLECTIONS, true)
				).enableDefaultTyping();

        Customer customer = em.find(Customer.class, 103);
        Assert.assertFalse(Hibernate.isInitialized(customer.getPayments()));
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(customer);
        Assert.assertFalse(json.contains("org.hibernate.collection"));
        // should force loading...
        Set<Payment> payments = customer.getPayments();
        /*
        System.out.println("--- JSON ---");
        System.out.println(json);
        System.out.println("--- /JSON ---");
        */

        Assert.assertTrue(Hibernate.isInitialized(payments));
        // TODO: verify
        Assert.assertNotNull(json);

        /*
         * Currently this cannot be verified due to Issue#94 default typing fails on 2.7.0 - 2.8.2-SNAPSHOT,
         * commented out until that is fixed.
         */

        boolean issue94failed = false;
        try {
            Map<?, ?> stuff = mapper.readValue(json, Map.class);
        } catch (JsonMappingException e) {
            issue94failed = true;
        }

        Assert.assertTrue("If this fails, means #94 is fixed. Replace to the below commented lines", issue94failed);

//		Map<?, ?> stuff = mapper.readValue(json, Map.class);
//
//		Assert.assertTrue(stuff.containsKey("payments"));
//		Assert.assertTrue(stuff.containsKey("orders"));
//		Assert.assertNull(stuff.get("orderes"));
    }
}
