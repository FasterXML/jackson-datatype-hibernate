package com.fasterxml.jackson.datatype.hibernate5.failing;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.mockito.stubbing.Answer;
import org.mockito.invocation.InvocationOnMock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate5.BaseTest;

/**
 * Problem with handling of Object Id, [datatype-hibernate#41]
 */
public class Issue41Test extends BaseTest
{
    @Entity
    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property = "id")
    public static class SomeJPAEntity  {
        @Id
        public String id;

        // @ManyToOne not needed because we mock this
        public SomeJPAEntity owner;
    }

    public static class EntityHibernateProxy extends SomeJPAEntity implements HibernateProxy
    {
        private static final long serialVersionUID = 1L;

        private LazyInitializer lazyInitializer;

        public EntityHibernateProxy() {
            this(true);
        }

        public EntityHibernateProxy(final boolean uninitialized) {
            lazyInitializer = mock(LazyInitializer.class);
            when(lazyInitializer.isUninitialized()).thenReturn(uninitialized);
            final String currId = "d0024148-3040-4fee-a78a-e94fd2449ac6";
            when(lazyInitializer.getIdentifier()).thenReturn(currId);
            when(lazyInitializer.getEntityName()).thenReturn("someJPAEntity");

            when(lazyInitializer.getImplementation()).thenAnswer(new Answer<Object>() {

                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    SomeJPAEntity entity = new SomeJPAEntity();
                    entity.id = id;
                    entity.owner = new EntityHibernateProxy(false);
                    return entity;
                }});
        }

        @Override
        public Object writeReplace() {
            return this;
        }

        @Override
        public LazyInitializer getHibernateLazyInitializer() {
            return lazyInitializer;
        }
    }
    
    public void testIssue41() throws Exception
    {
        EntityHibernateProxy entity = new EntityHibernateProxy(false);
        entity.id = "3cf7a573-f528-440c-83b9-873d7594b373";
        entity.owner = entity;

        ObjectMapper mapper = mapperWithModule(true);
        String json = mapper.writeValueAsString(entity);

        // will throw an exception long before here so this should suffice:
        assertNotNull(json);
    }
}
