package com.fasterxml.jackson.datatype.hibernate4;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.hibernate4.data.Customer;
import com.fasterxml.jackson.datatype.hibernate4.data.Payment;
import org.hibernate.Hibernate;

import org.junit.jupiter.api.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ReplacePersistentCollectionTest {

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
		final ObjectMapper mapper = hibernateMapper(new Hibernate4Module()
						.configure(Hibernate4Module.Feature.FORCE_LAZY_LOADING, true)
						);

		Customer customer = em.find(Customer.class, 103);
		assertFalse(Hibernate.isInitialized(customer.getPayments()));
		String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(customer);
		assertTrue(json.contains("org.hibernate.collection"));
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

		boolean exceptionThrown = false;
		try {
			/*Map<?, ?> stuff =*/ mapper.readValue(json, Map.class);
		} catch (JsonMappingException e) {
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
	}

	// [Issue#93], backwards compatible case
	@Test
	public void testReplacePersistentCollection() throws Exception {
		final ObjectMapper mapper = hibernateMapper(new Hibernate4Module()
						.configure(Hibernate4Module.Feature.FORCE_LAZY_LOADING, true)
						.configure(Hibernate4Module.Feature.REPLACE_PERSISTENT_COLLECTIONS, true));

		Customer customer = em.find(Customer.class, 103);
		assertFalse(Hibernate.isInitialized(customer.getPayments()));
		String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(customer);
		assertFalse(json.contains("org.hibernate.collection"));
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

        /*
         * Currently this cannot be verified due to Issue#94 default typing fails on 2.7.0 - 2.8.2-SNAPSHOT,
         * commented out until that is fixed.
         */

		boolean issue94failed = false;
		try {
			/* Map<?, ?> stuff =*/ mapper.readValue(json, Map.class);
		} catch (JsonMappingException e) {
			issue94failed = true;
		}

		assertTrue(issue94failed, "If this fails, means #94 is fixed. Replace to the below commented lines");

//		Map<?, ?> stuff = mapper.readValue(json, Map.class);
//
//		assertTrue(stuff.containsKey("payments"));
//		assertTrue(stuff.containsKey("orders"));
//		assertNull(stuff.get("orderes"));
	}

	private ObjectMapper hibernateMapper(Hibernate4Module module) {
         return JsonMapper.builder()
                 .addModule(module)
                 .build()
                 .enableDefaultTyping();
	}
}
