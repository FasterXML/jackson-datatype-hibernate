package tools.jackson.datatype.hibernate7;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import org.junit.jupiter.api.Test;

import tools.jackson.datatype.hibernate7.data.SimpleChild;
import tools.jackson.datatype.hibernate7.data.SimpleParent;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for List-backed {@code @OneToMany} collections (bags) with
 * {@code REPLACE_PERSISTENT_COLLECTIONS}.
 */
public class BagCollectionTest extends BaseTest
{
    /**
     * Verifies that {@code convertToJavaCollection()} correctly converts a
     * PersistentBag (List-backed @OneToMany) to a plain JSON array when
     * REPLACE_PERSISTENT_COLLECTIONS is enabled.
     */
    @Test
    public void testBagReplacedWithPlainList() throws Exception
    {
        Configuration cfg = new Configuration()
                .addAnnotatedClass(SimpleParent.class)
                .addAnnotatedClass(SimpleChild.class)
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:bagTest;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.hbm2ddl.auto", "create");

        SimpleParent detached;
        try (SessionFactory sf = cfg.buildSessionFactory()) {
            Integer parentId = sf.fromTransaction(session -> {
                SimpleParent p = new SimpleParent("P1");
                SimpleChild c1 = new SimpleChild("C1", p);
                SimpleChild c2 = new SimpleChild("C2", p);
                p.children.add(c1);
                p.children.add(c2);
                session.persist(p);
                session.persist(c1);
                session.persist(c2);
                return p.id;
            });

            detached = sf.fromTransaction(session -> {
                SimpleParent p = session.find(SimpleParent.class, parentId);
                Hibernate.initialize(p.children);
                return p;
            });
        }

        Hibernate7Module mod = new Hibernate7Module();
        mod.configure(Hibernate7Module.Feature.FORCE_LAZY_LOADING, true);
        mod.configure(Hibernate7Module.Feature.REPLACE_PERSISTENT_COLLECTIONS, true);
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(mod)
                .build();

        String json = mapper.writeValueAsString(detached);
        assertNotNull(json);
        // Should not contain PersistentBag type info
        assertFalse(json.contains("org.hibernate.collection"), "JSON should not contain Hibernate collection type");
        assertThat(json).contains("\"P1\"", "\"C1\"", "\"C2\"", "\"children\"");
    }
}
