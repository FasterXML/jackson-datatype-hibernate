package com.fasterxml.jackson.datatype.hibernate4.data;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class EntityHibernateProxy extends JPAEntity implements HibernateProxy {

	private LazyInitializer lazyInitializer;
	/**
	 * 
	 */
	private static final long serialVersionUID = 7245130694573745489L;

	public EntityHibernateProxy() {
		this(true);
	}

	public EntityHibernateProxy(final boolean uninitialized) {
		lazyInitializer = mock(LazyInitializer.class);
		when(lazyInitializer.isUninitialized()).thenReturn(uninitialized);
		final String id = "d0024148-3040-4fee-a78a-e94fd2449ac6";
		when(lazyInitializer.getIdentifier()).thenReturn(id);
		when(lazyInitializer.getEntityName()).thenReturn(JPAEntity.class.getSimpleName());
		
		when(lazyInitializer.getImplementation()).thenAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				JPAEntity entity = new JPAEntity();
				entity.setId(id);
				entity.setOwner(new EntityHibernateProxy(false));
				return entity;
			}});
	}

	@Override
	public Object writeReplace() {
		return null;
	}

	@Override
	public LazyInitializer getHibernateLazyInitializer() {
		return lazyInitializer;
	}

}