package tools.jackson.datatype.hibernate7;

import java.util.Map;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import org.junit.jupiter.api.Test;

import tools.jackson.datatype.hibernate7.data.Customer;
import tools.jackson.datatype.hibernate7.data.Payment;
import tools.jackson.datatype.hibernate7.data.SimpleChild;
import tools.jackson.datatype.hibernate7.data.SimpleParent;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
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

    /**
     * Verifies that {@code findLazyValue()} does not open a temporary session
     * for collections that are already initialized.  We use a SessionFactory
     * backed by a non-existent database; if the code needlessly calls
     * {@code openSession()}, the JDBC driver will throw.
     */
    @Test
    public void testAlreadyInitializedCollectionSkipsSessionOpen() throws Exception
    {
        // Use simple entities with a single level of lazy loading — no nested
        // lazy chains that would need further initialization.
        Configuration cfg = new Configuration()
                .addAnnotatedClass(SimpleParent.class)
                .addAnnotatedClass(SimpleChild.class)
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:initOpt;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.hbm2ddl.auto", "create");

        SimpleParent detached;
        try (SessionFactory sf = cfg.buildSessionFactory()) {
            Integer parentId = sf.fromTransaction(session -> {
                SimpleParent p = new SimpleParent("P1");
                SimpleChild c = new SimpleChild("C1", p);
                p.children.add(c);
                session.persist(p);
                session.persist(c);
                return p.id;
            });

            // Load and explicitly initialize the lazy children collection
            detached = sf.fromTransaction(session -> {
                SimpleParent p = session.find(SimpleParent.class, parentId);
                Hibernate.initialize(p.children);
                return p;
            });
        }
        // After session close, `children` is either a plain List (Hibernate
        // replaced the PersistentBag) or an initialized PersistentCollection.
        // Either way, findLazyValue() must NOT try to open a session.

        // SessionFactory backed by an empty in-memory database (no tables).
        // If findLazyValue() tries to open a session and initialize a
        // collection, the query will fail with a "table not found" error.
        Configuration broken = new Configuration()
                .addAnnotatedClass(SimpleParent.class)
                .addAnnotatedClass(SimpleChild.class)
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:empty;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.hbm2ddl.auto", "none");
        try (SessionFactory brokenSf = broken.buildSessionFactory()) {
            JsonMapper mapper = JsonMapper.builder()
                    .addModule(hibernateModule(true, false, brokenSf))
                    .build();

            // Should succeed without attempting to open a session
            String json = mapper.writeValueAsString(detached);
            assertNotNull(json);
            assertThat(json).contains("\"P1\"", "\"children\"");
        }
    }
}
