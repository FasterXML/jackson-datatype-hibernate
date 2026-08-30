package tools.jackson.datatype.hibernate5.jakarta;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import java.util.Map;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.datatype.hibernate5.jakarta.data.Customer;
import tools.jackson.datatype.hibernate5.jakarta.data.Payment;
import tools.jackson.datatype.hibernate5.jakarta.data.SimpleChild;
import tools.jackson.datatype.hibernate5.jakarta.data.SimpleParent;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

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
     * Verifies that {@code findLazyValue()} opens a temporary session only for a
     * collection that actually needs initializing: none for an already loaded one,
     * exactly one for a lazy one. Both halves matter -- the second is what shows the
     * counter can see a session being opened, so that the zero in the first is a real
     * assertion and not just a counter that never fires.
     */
    @Test
    public void testTemporarySessionOnlyOpenedWhenNeeded() throws Exception
    {
        SessionFactory sf = buildSimpleSessionFactory();
        try {
            Integer parentId = createParentWithChild(sf);

            // (1) Detached parent whose "children" was explicitly initialized while the
            //     loading session was still open
            SimpleParent initialized = loadParent(sf, parentId, true);
            // Guard the premise: still a PersistentCollection, just a loaded one. Without
            // this the test would silently stop exercising anything if Hibernate ever
            // started swapping in a plain List on detach.
            assertInstanceOf(PersistentCollection.class, initialized.children);
            assertTrue(Hibernate.isInitialized(initialized.children));

            SessionOpenCounter counter = new SessionOpenCounter(sf);
            String json = mapperWith(counter.factory()).writeValueAsString(initialized);

            assertTrue(json.contains("\"P1\""), json);
            assertTrue(json.contains("\"C1\""), json);
            assertEquals(0, counter.openSessionCount(),
                    "Should not open a temporary session for an already initialized collection");

            // (2) Same entity, left uninitialized: here the temporary session is what
            //     makes FORCE_LAZY_LOADING work at all, so it must still be opened
            SimpleParent uninitialized = loadParent(sf, parentId, false);
            assertFalse(Hibernate.isInitialized(uninitialized.children));

            counter = new SessionOpenCounter(sf);
            json = mapperWith(counter.factory()).writeValueAsString(uninitialized);

            assertTrue(json.contains("\"P1\""), json);
            assertTrue(json.contains("\"C1\""), json);
            assertEquals(1, counter.openSessionCount(),
                    "Should open exactly one temporary session to force-load the collection");
        } finally {
            sf.close();
        }
    }

    private Integer createParentWithChild(SessionFactory sf)
    {
        Session session = sf.openSession();
        try {
            Transaction tx = session.beginTransaction();
            SimpleParent p = new SimpleParent("P1");
            SimpleChild c = new SimpleChild("C1", p);
            p.children.add(c);
            session.persist(p);
            session.persist(c);
            tx.commit();
            return p.id;
        } finally {
            session.close();
        }
    }

    private SimpleParent loadParent(SessionFactory sf, Integer id, boolean initializeChildren)
    {
        Session session = sf.openSession();
        try {
            Transaction tx = session.beginTransaction();
            SimpleParent p = (SimpleParent) session.get(SimpleParent.class, id);
            if (initializeChildren) {
                Hibernate.initialize(p.children);
            }
            tx.commit();
            return p;
        } finally {
            session.close();
        }
    }

    private ObjectMapper mapperWith(SessionFactory sessionFactory) {
        return JsonMapper.builder()
                .addModule(hibernateModule(true, false, sessionFactory))
                .build();
    }

    private SessionFactory buildSimpleSessionFactory() {
        return new Configuration()
                .addAnnotatedClass(SimpleParent.class)
                .addAnnotatedClass(SimpleChild.class)
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:lazyInit5j;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
                .setProperty("hibernate.hbm2ddl.auto", "create")
                .buildSessionFactory();
    }

    /**
     * Counts {@code openSession()} calls made by the serializer and delegates everything
     * else to the real factory. A dynamic proxy rather than a hand-written wrapper both
     * to avoid implementing the whole of {@link SessionFactory} and so that the
     * {@link SessionFactoryImplementor} cast done by
     * {@code openTemporarySessionForLoading()} still succeeds.
     */
    static class SessionOpenCounter implements InvocationHandler
    {
        private final SessionFactory _delegate;

        private int _openSessionCount;

        SessionOpenCounter(SessionFactory delegate) {
            _delegate = delegate;
        }

        SessionFactory factory() {
            return (SessionFactory) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[] { SessionFactoryImplementor.class }, this);
        }

        int openSessionCount() {
            return _openSessionCount;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("openSession".equals(method.getName()) && (args == null || args.length == 0)) {
                ++_openSessionCount;
            }
            try {
                return method.invoke(_delegate, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }
}
