package tools.jackson.datatype.hibernate5.jakarta;

import org.hibernate.Hibernate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.datatype.hibernate5.jakarta.data.Customer;
import tools.jackson.datatype.hibernate5.jakarta.data.Product;

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
            // Properties are unwrapped: Product's own properties are written directly,
            // without the wrapper's "content" property name around them
            assertTrue(first.contains("\"productCode\""), first);
            assertFalse(first.contains("\"content\""), first);

            // Second pass finds the serializer cached by the first one: must produce
            // the same (non-empty, still unwrapped) output, not fall back to the raw one
            String second = mapper.writeValueAsString(value);
            assertTrue(second.contains("\"productCode\""), second);
            assertEquals(first, second);

        } finally {
            emf.close();
        }
    }
}
