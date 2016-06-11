package com.fasterxml.jackson.datatype.hibernate4;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.Hibernate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate4.data.Contrato;

public class InfiniteRecursion70Test extends BaseTest
{
	// [datatype-hibernate#70]
    public void testInfinite() throws Exception
    {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("persistenceUnit");
        String expected = "{\"id\":1,\"numeroContrato\":\"100001-9\",\"parcelas\":[{\"id\":1,\"numeroParcela\":1}],\"liquidacoes\":[{\"id\":1,\"valorTotal\":10000,\"parcela\":{\"id\":1,\"numeroParcela\":1}}]}";
        final ObjectMapper mapper = mapperWithModule(true);

        try {
             EntityManager em = emf.createEntityManager();
             
             final Contrato contrato1 = em.find(Contrato.class, 1L);
             Hibernate.initialize(contrato1.getParcelas());
             Hibernate.initialize(contrato1.getLiquidacoes());             
             assertEquals(expected, mapper.writeValueAsString(contrato1));

             em.clear();

             final Contrato contrato2 = em.find(Contrato.class, 1L);
             Hibernate.initialize(contrato2.getLiquidacoes());
             Hibernate.initialize(contrato2.getParcelas());
             assertEquals(expected, mapper.writeValueAsString(contrato2)); 
         } finally {
             try {
                 emf.close();
             } catch (Exception e) { }
         }
	}
}
