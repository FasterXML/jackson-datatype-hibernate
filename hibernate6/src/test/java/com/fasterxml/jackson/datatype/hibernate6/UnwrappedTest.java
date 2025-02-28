package com.fasterxml.jackson.datatype.hibernate6;

import com.fasterxml.jackson.datatype.hibernate6.data.Customer;
import com.fasterxml.jackson.datatype.hibernate6.data.Product;
import org.hibernate.Hibernate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    public void testSimpleUnwrapped() throws JsonProcessingException
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
}
