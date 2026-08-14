package tools.jackson.datatype.hibernate7;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import org.junit.jupiter.api.Test;

import tools.jackson.datatype.hibernate7.data.AuditedChild;
import tools.jackson.datatype.hibernate7.data.AuditedParent;
import tools.jackson.datatype.hibernate7.data.Customer;
import tools.jackson.datatype.hibernate7.data.Payment;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

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
        // SessionFactory #1: real database — used to create and initialize data
        Configuration cfg = new Configuration()
                .addAnnotatedClass(AuditedParent.class)
                .addAnnotatedClass(AuditedChild.class)
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:initOpt;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.hbm2ddl.auto", "create");

        AuditedParent detached;
        try (SessionFactory sf = cfg.buildSessionFactory()) {
            // Persist a parent with children
            Integer parentId = sf.fromTransaction(session -> {
                AuditedParent p = new AuditedParent("P1");
                AuditedChild c = new AuditedChild("C1", p);
                p.children.add(c);
                session.persist(p);
                session.persist(c);
                return p.id;
            });

            // Load and explicitly initialize the lazy children collection
            // while the session is still open
            detached = sf.fromTransaction(session -> {
                AuditedParent p = session.find(AuditedParent.class, parentId);
                Hibernate.initialize(p.children);
                return p;
            });
        }
        // After session close, `children` is either a plain List (Hibernate
        // replaced the PersistentBag) or an initialized PersistentCollection.
        // Either way, findLazyValue() must NOT try to open a session.

        // SessionFactory #2: non-existent database — any openSession() call
        // would produce a connection failure
        Configuration broken = new Configuration()
                .addAnnotatedClass(AuditedParent.class)
                .addAnnotatedClass(AuditedChild.class)
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:doesNotExist")
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.hbm2ddl.auto", "none");
        try (SessionFactory brokenSf = broken.buildSessionFactory()) {
            JsonMapper mapper = JsonMapper.builder()
                    .addModule(hibernateModule(true, false, brokenSf))
                    .build();

            // Should succeed without attempting to open a session
            String json = mapper.writeValueAsString(detached);
            assertNotNull(json);
            assertTrue(json.contains("\"P1\""));
            assertTrue(json.contains("\"children\""));
        }
    }
}
