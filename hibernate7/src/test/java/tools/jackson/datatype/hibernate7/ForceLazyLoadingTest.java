package tools.jackson.datatype.hibernate7;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.collection.spi.PersistentCollection;

import org.junit.jupiter.api.Test;

import tools.jackson.datatype.hibernate7.data.AuditedChild;
import tools.jackson.datatype.hibernate7.data.AuditedParent;
import tools.jackson.datatype.hibernate7.data.Customer;
import tools.jackson.datatype.hibernate7.data.SimpleChild;
import tools.jackson.datatype.hibernate7.data.SimpleParent;
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
     * Wraps a {@link SessionFactory} so that calls to {@code openSession()}
     * can be counted.  A dynamic proxy is used rather than a fake database:
     * {@code jdbc:h2:mem:<name>} silently creates an empty database instead of
     * failing, and {@code openSession()} does not acquire a JDBC connection
     * eagerly, so an unwanted session open would go unnoticed.
     */
    static class OpenSessionCounter implements InvocationHandler
    {
        private final SessionFactory _delegate;
        int openSessionCalls;

        OpenSessionCounter(SessionFactory delegate) {
            _delegate = delegate;
        }

        static SessionFactory wrap(SessionFactory sf, OpenSessionCounter counter) {
            // Must expose every interface the real factory does: the code under
            // test casts the factory to `SessionFactoryImplementor`
            Set<Class<?>> ifaces = new LinkedHashSet<>();
            for (Class<?> c = sf.getClass(); c != null; c = c.getSuperclass()) {
                collectInterfaces(c.getInterfaces(), ifaces);
            }
            return (SessionFactory) Proxy.newProxyInstance(
                    SessionFactory.class.getClassLoader(),
                    ifaces.toArray(new Class<?>[0]),
                    counter);
        }

        private static void collectInterfaces(Class<?>[] from, Set<Class<?>> into) {
            for (Class<?> iface : from) {
                if (into.add(iface)) {
                    collectInterfaces(iface.getInterfaces(), into);
                }
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("openSession".equals(method.getName())) {
                ++openSessionCalls;
            }
            try {
                return method.invoke(_delegate, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }

    private Integer persistParentWithChild(SessionFactory sf, String parentName) {
        return sf.fromTransaction(session -> {
            SimpleParent p = new SimpleParent(parentName);
            SimpleChild c = new SimpleChild("C1", p);
            p.children.add(c);
            session.persist(p);
            session.persist(c);
            return p.id;
        });
    }

    private SessionFactory buildSessionFactory(String dbName) {
        return new Configuration()
                .addAnnotatedClass(SimpleParent.class)
                .addAnnotatedClass(SimpleChild.class)
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.hbm2ddl.auto", "create")
                .buildSessionFactory();
    }

    /**
     * {@code findLazyValue()} must not open a temporary session for a
     * collection that is already initialized -- that would cost a needless
     * JDBC connection and transaction, and would leave the collection pointing
     * at a session that is then closed.
     */
    @Test
    public void testAlreadyInitializedCollectionSkipsSessionOpen() throws Exception
    {
        try (SessionFactory sf = buildSessionFactory("initOptDone")) {
            Integer parentId = persistParentWithChild(sf, "P1");

            // Load and explicitly initialize the lazy children collection
            // while the session is still open
            SimpleParent detached = sf.fromTransaction(session -> {
                SimpleParent p = session.find(SimpleParent.class, parentId);
                Hibernate.initialize(p.children);
                return p;
            });
            assertTrue(detached.children instanceof PersistentCollection,
                    "precondition: children should still be a PersistentCollection");
            assertTrue(((PersistentCollection<?>) detached.children).wasInitialized(),
                    "precondition: children should already be initialized");

            OpenSessionCounter counter = new OpenSessionCounter(sf);
            JsonMapper mapper = JsonMapper.builder()
                    .addModule(hibernateModule(true, false, OpenSessionCounter.wrap(sf, counter)))
                    .build();

            String json = mapper.writeValueAsString(detached);

            assertEquals(0, counter.openSessionCalls,
                    "should not open a session for an already-initialized collection");
            assertNotNull(json);
            assertTrue(json.contains("\"P1\""));
            assertTrue(json.contains("\"children\""));
        }
    }

    /**
     * Negative control for {@link #testAlreadyInitializedCollectionSkipsSessionOpen}:
     * proves the counter actually observes session opens, so that a count of
     * zero there is meaningful rather than an artefact of a broken probe.
     */
    @Test
    public void testUninitializedCollectionDoesOpenSession() throws Exception
    {
        try (SessionFactory sf = buildSessionFactory("initOptPending")) {
            Integer parentId = persistParentWithChild(sf, "P2");

            // Load WITHOUT initializing the children collection
            SimpleParent detached = sf.fromTransaction(session ->
                    session.find(SimpleParent.class, parentId));
            assertFalse(Hibernate.isInitialized(detached.children),
                    "precondition: children should not be initialized");

            OpenSessionCounter counter = new OpenSessionCounter(sf);
            JsonMapper mapper = JsonMapper.builder()
                    .addModule(hibernateModule(true, false, OpenSessionCounter.wrap(sf, counter)))
                    .build();

            String json = mapper.writeValueAsString(detached);

            assertTrue(counter.openSessionCalls > 0,
                    "should open a temporary session to initialize the collection");
            assertNotNull(json);
            assertTrue(json.contains("\"C1\""));
        }
    }
}
