package tools.jackson.datatype.hibernate7;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.datatype.hibernate7.data.AuditedChild;
import tools.jackson.datatype.hibernate7.data.AuditedParent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for [datatype-hibernate#204]: serializing Envers-audited entities with
 * bidirectional associations and {@code FORCE_LAZY_LOADING=true} causes
 * unbounded recursion on Hibernate ORM 7.3+/7.4.
 * <p>
 * Root cause: ORM 7.3 changed Envers so that
 * {@code @Audited(targetAuditMode = NOT_AUDITED)} associations materialize
 * current session-attached entities (live {@code PersistentBag}) instead of
 * historic audit-table data.  The module's {@code FORCE_LAZY_LOADING} then
 * force-initializes these collections, traversing the bidirectional cycle
 * until Jackson's {@code StreamWriteConstraints} nesting limit is hit.
 */
public class EnversForceLazyLoadingTest extends BaseTest
{
    /**
     * Serialize an Envers revision of an entity whose graph contains a
     * bidirectional {@code @OneToMany} / {@code @ManyToOne} cycle.
     * <p>
     * With {@code FORCE_LAZY_LOADING=true} and no cycle detection in the
     * module, the serializer follows the cycle indefinitely:
     * <pre>
     *   AuditedParent → children → AuditedChild → parent → AuditedParent → ...
     * </pre>
     * On Hibernate ORM 7.3+ this produces a {@code StreamConstraintsException}
     * (nesting depth &gt; 500).
     */
    @Test
    public void testEnversRevisionWithBidirectionalGraph() throws Exception
    {
        Configuration cfg = new Configuration()
                .addAnnotatedClass(AuditedParent.class)
                .addAnnotatedClass(AuditedChild.class)
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:enversForceLazy;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.hbm2ddl.auto", "create");

        JsonMapper mapper = JsonMapper.builder()
                .addModule(hibernateModule(true))
                .build();

        try (SessionFactory sf = cfg.buildSessionFactory()) {
            // Revision 1: create parent with two children
            Integer parentId = sf.fromTransaction(session -> {
                AuditedParent parent = new AuditedParent("Parent-v1");
                AuditedChild c1 = new AuditedChild("child-1", parent);
                AuditedChild c2 = new AuditedChild("child-2", parent);
                parent.children.add(c1);
                parent.children.add(c2);
                session.persist(parent);
                session.persist(c1);
                session.persist(c2);
                return parent.id;
            });

            // Revision 2: update parent name
            sf.inTransaction(session -> {
                AuditedParent p = session.find(AuditedParent.class, parentId);
                p.name = "Parent-v2";
            });

            // Load revision 1 via Envers AuditReader and serialize.
            // The returned entity is session-attached; on ORM 7.3+ the
            // children collection is a live PersistentBag that
            // FORCE_LAZY_LOADING will try to initialize and traverse.
            String json = sf.fromTransaction(session -> {
                AuditReader reader = AuditReaderFactory.get(session);
                AuditedParent rev1 = reader.find(AuditedParent.class, parentId, 1);
                return mapper.writeValueAsString(rev1);
            });

            // If we get here, the serialization completed without infinite
            // recursion — verify the output is well-formed.
            assertNotNull(json);
            assertThat(json).contains("\"Parent-v1\"", "\"children\"", "\"child-1\"");
        }
    }

    /**
     * Verifies that the cycle detector uses identity ({@code ==}), not
     * {@code equals()}, when tracking objects.  Two distinct
     * {@link AuditedChild} instances with the same label are
     * {@code .equals()} but not {@code ==}; both must appear in the output.
     */
    @Test
    public void testEqualsButDistinctEntitiesBothSerialized() throws Exception
    {
        Configuration cfg = new Configuration()
                .addAnnotatedClass(AuditedParent.class)
                .addAnnotatedClass(AuditedChild.class)
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:enversIdentity;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.hbm2ddl.auto", "create");

        JsonMapper mapper = JsonMapper.builder()
                .addModule(hibernateModule(true))
                .build();

        try (SessionFactory sf = cfg.buildSessionFactory()) {
            Integer parentId = sf.fromTransaction(session -> {
                AuditedParent parent = new AuditedParent("Parent");
                // Two children with the same label — .equals() but not ==
                AuditedChild c1 = new AuditedChild("same-label", parent);
                AuditedChild c2 = new AuditedChild("same-label", parent);
                parent.children.add(c1);
                parent.children.add(c2);
                session.persist(parent);
                session.persist(c1);
                session.persist(c2);
                return parent.id;
            });

            String json = sf.fromTransaction(session -> {
                AuditReader reader = AuditReaderFactory.get(session);
                AuditedParent rev1 = reader.find(AuditedParent.class, parentId, 1);
                return mapper.writeValueAsString(rev1);
            });

            assertNotNull(json);
            // Both children must appear — the cycle detector must use ==,
            // not .equals(), so the second child is NOT falsely skipped.
            int count = countOccurrences(json, "\"same-label\"");
            assertThat(count).as("both .equals() children should be serialized")
                    .isGreaterThanOrEqualTo(2);
        }
    }

    private static int countOccurrences(String s, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = s.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}
