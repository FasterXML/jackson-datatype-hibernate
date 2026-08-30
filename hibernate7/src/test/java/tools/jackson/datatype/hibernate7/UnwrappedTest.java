package tools.jackson.datatype.hibernate7;

import tools.jackson.datatype.hibernate7.data.Customer;
import tools.jackson.datatype.hibernate7.data.Product;
import org.hibernate.Hibernate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for [#97]
 */
public class UnwrappedTest extends BaseTest
{
    static class HasUnwrapped<T>
    {
        private final T content;

        @JsonCreator
        public HasUnwrapped(T content)
        {
            this.content = content;
        }

        @JsonUnwrapped
        public T getContent()
        {
            return content;
        }
    }

    @Test
    public void testSimpleUnwrapped() throws JacksonException
    {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("persistenceUnit");
        try {
            EntityManager em = emf.createEntityManager();

            ObjectMapper mapper = mapperWithModule(true);

            Customer customer = em.find(Customer.class, 500);
            Product product = customer.getMissingProduct();
            assertFalse(Hibernate.isInitialized(product));

            String json = mapper.writeValueAsString(new HasUnwrapped<>(product));

            assertTrue(Hibernate.isInitialized(product));
            assertNotNull(json);
            HasUnwrapped<Product> deserialized = mapper.readValue(json, new TypeReference<HasUnwrapped<Product>>(){});
            assertTrue(deserialized != null);
            assertTrue(deserialized.getContent() != null);
            assertTrue(deserialized.getContent().getProductCode() != null);

        } finally {
            emf.close();
        }
    }

    // [datatypes-hibernate#209]: the unwrapping serializer has to survive being cached,
    //   so a second pass through the same mapper must not fall back to the raw one
    @Test
    public void testRepeatedUnwrappedSerialization() throws JacksonException
    {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("persistenceUnit");
        try {
            EntityManager em = emf.createEntityManager();

            ObjectMapper mapper = mapperWithModule(true);

            Customer customer = em.find(Customer.class, 500);
            HasUnwrapped<Product> value = new HasUnwrapped<>(customer.getMissingProduct());

            String first = mapper.writeValueAsString(value);
            // properties are unwrapped, so the wrapper's own property name is not written
            assertFalse(first.contains("\"content\""), first);

            // Second pass finds the serializer cached by the first one
            String second = mapper.writeValueAsString(value);
            assertEquals(first, second);

        } finally {
            emf.close();
        }
    }
}
