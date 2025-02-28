package com.fasterxml.jackson.datatype.hibernate5;

import java.util.Set;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.datatype.hibernate5.data.Customer;
import com.fasterxml.jackson.datatype.hibernate5.data.Payment;
import org.hibernate.Hibernate;

import org.junit.jupiter.api.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import static org.junit.jupiter.api.Assertions.*;

public class ReplacePersistentCollectionTest extends BaseTest
{
    private EntityManagerFactory emf;

    private EntityManager em;

    @BeforeEach
    public void setUp() throws Exception {
        emf = Persistence.createEntityManagerFactory("persistenceUnit");
        em = emf.createEntityManager();
    }

    @AfterEach
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
				).enableDefaultTyping(DefaultTyping.NON_FINAL);

        Customer customer = em.find(Customer.class, 103);
        assertFalse(Hibernate.isInitialized(customer.getPayments()));
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(customer);
        assertTrue(json.contains("org.hibernate.collection"));
        // should force loading...
        Set<Payment> payments = customer.getPayments();
        assertTrue(Hibernate.isInitialized(payments));
 
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
				.registerModule(new Hibernate5Module()
						.configure(Hibernate5Module.Feature.FORCE_LAZY_LOADING, true)
						.configure(Hibernate5Module.Feature.REPLACE_PERSISTENT_COLLECTIONS, true)
				        ).enableDefaultTyping(DefaultTyping.NON_FINAL);

		Customer customer = em.find(Customer.class, 103);
		assertFalse(Hibernate.isInitialized(customer.getPayments()));
		String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(customer);
		assertFalse(json.contains("org.hibernate.collection"));
		// should force loading...
		Set<Payment> payments = customer.getPayments();

		assertTrue(Hibernate.isInitialized(payments));
		Customer stuff = mapper.readValue(json, Customer.class);
		assertNotNull(stuff);

//		Map<?, ?> stuff = mapper.readValue(json, Map.class);
//
//		assertTrue(stuff.containsKey("payments"));
//		assertTrue(stuff.containsKey("orders"));
//		assertNull(stuff.get("orderes"));
    }
}
