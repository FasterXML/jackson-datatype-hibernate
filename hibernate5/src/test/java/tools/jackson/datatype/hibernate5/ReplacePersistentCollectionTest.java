package tools.jackson.datatype.hibernate5;

import java.util.*;

import org.junit.jupiter.api.*;

import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import tools.jackson.datatype.hibernate5.data.Customer;
import tools.jackson.datatype.hibernate5.data.Payment;
import tools.jackson.datatype.hibernate5.testutil.NoCheckSubTypeValidator;

import org.hibernate.Hibernate;

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

    // [datatypes-hibernate#93], backwards compatible case
    @Test
    public void testNoReplacePersistentCollection() throws Exception {
        final ObjectMapper mapper = hibernateMapper(new Hibernate5Module()
                .configure(Hibernate5Module.Feature.FORCE_LAZY_LOADING, true)
                );

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
        } catch (DatabindException e) {
            verifyException(e, "failed to lazily initialize");
        }
    }

    // [datatypes-hibernate#93], backwards compatible case
    @Test
    public void testReplacePersistentCollection() throws Exception {
		final ObjectMapper mapper = hibernateMapper(new Hibernate5Module()
						.configure(Hibernate5Module.Feature.FORCE_LAZY_LOADING, true)
						.configure(Hibernate5Module.Feature.REPLACE_PERSISTENT_COLLECTIONS, true)
						);

		Customer customer = em.find(Customer.class, 103);
		assertFalse(Hibernate.isInitialized(customer.getPayments()));
		String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(customer);
		assertFalse(json.contains("org.hibernate.collection"));
		// should force loading...
		Set<Payment> payments = customer.getPayments();

          assertTrue(Hibernate.isInitialized(payments));
          Customer stuff = mapper.readValue(json, Customer.class);
          assertNotNull(stuff);

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
                .activateDefaultTyping(new NoCheckSubTypeValidator(),
                        DefaultTyping.NON_FINAL)
                .build();
    }
}
