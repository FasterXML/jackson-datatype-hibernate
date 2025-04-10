package com.fasterxml.jackson.datatype.hibernate5.jakarta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate5.jakarta.data.Customer;
import com.fasterxml.jackson.datatype.hibernate5.jakarta.data.Employee;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.Query;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HibernateTest extends BaseTest
{
    protected EntityManagerFactory emf;

    @BeforeEach
    public void setUp() {
        emf = Persistence.createEntityManagerFactory("persistenceUnit");
    }

    @AfterEach
    public void tearDown() {
        if (emf!=null) {
            emf.close();
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */
    
    @Test
    public void testGetEntityManager() {
        EntityManager em = emf.createEntityManager();
        assertNotNull(em);
    }

    @Test
    public void testGetCustomerJson() throws Exception {
        EntityManager em = emf.createEntityManager();
        ObjectMapper mapper = mapperWithModule(false);
        String json = mapper.writeValueAsString(em.find(Customer.class, 103));
        
        // TODO: verify
        assertNotNull(json);
        /*
        System.out.println("--- JSON ---");
        System.out.println(json);
        System.out.println("--- /JSON ---");
        */
    }

    @Test
    public void testAllCustomersJson() throws Exception {
        EntityManager em = emf.createEntityManager();
        assertNotNull(em);
        
        Query query = em.createQuery("select c from Customer c");
        // false -> no forcing of lazy loading
        ObjectMapper mapper = mapperWithModule(false);
        String json = mapper.writeValueAsString(query.getResultList());

        // TODO: verify
        assertNotNull(json);
        /*
        System.out.println("--- JSON ---");
        System.out.println(json);
        System.out.println("--- /JSON ---");
        */
    }
    
    /**
     * JPA objects relationships are bidirectional by default.
     * This test try to load an Employee who has assigned many
     * customers who, at the same time, have a link to the original
     * employee.
     */
    @Test
    public void testCyclesJson() throws Exception {
        EntityManager em = emf.createEntityManager();
        
        Employee salesEmployee = em.find(Employee.class, 1370);
        assertNotNull(salesEmployee);
        assertTrue(salesEmployee.getCustomers().size()>0);
        
        // false -> no forcing of lazy loading
        ObjectMapper mapper = mapperWithModule(false);
        String json = mapper.writeValueAsString(salesEmployee);

        // Ok; let's try reading back
        Employee result = mapper.readValue(json, Employee.class);
        assertNotNull(result);
        assertNotNull(result.getCustomers());
        assertEquals(salesEmployee.getCustomers().size(), result.getCustomers().size());
    }
}
