package tools.jackson.datatype.hibernate5.jakarta;

import java.util.Set;

import org.junit.jupiter.api.*;

import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.datatype.hibernate5.jakarta.data.Customer;
import tools.jackson.datatype.hibernate5.jakarta.data.Payment;
import tools.jackson.datatype.hibernate5.jakarta.testutil.NoCheckSubTypeValidator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.hibernate.Hibernate;

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
        final ObjectMapper mapper = hibernateMapper(new Hibernate5JakartaModule()
                .configure(Hibernate5JakartaModule.Feature.FORCE_LAZY_LOADING, true)
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
        final ObjectMapper mapper = hibernateMapper(new Hibernate5JakartaModule()
                .configure(Hibernate5JakartaModule.Feature.FORCE_LAZY_LOADING, true)
                .configure(Hibernate5JakartaModule.Feature.REPLACE_PERSISTENT_COLLECTIONS, true)
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

//		Map<?, ?> stuff = mapper.readValue(json, Map.class);
//
//		assertTrue(stuff.containsKey("payments"));
//		assertTrue(stuff.containsKey("orders"));
//		assertNull(stuff.get("orderes"));
    }

    private ObjectMapper hibernateMapper(Hibernate5JakartaModule module) {
        return JsonMapper.builder()
                .addModule(module)
                .activateDefaultTyping(new NoCheckSubTypeValidator(),
                        DefaultTyping.NON_FINAL)
                .build();
    }
}
