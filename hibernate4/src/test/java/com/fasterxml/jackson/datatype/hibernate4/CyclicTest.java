package com.fasterxml.jackson.datatype.hibernate4;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate4.Hibernate4Module.Feature;
import com.fasterxml.jackson.datatype.hibernate4.data.EntityHibernateProxy;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

public class CyclicTest {
	
	@Test
	public void testCyclicMarshallingHibernateProxy() throws JsonGenerationException,
			JsonMappingException, IOException {
		String s = getJSONStringForEntity(true, false);
		assertEquals("{\"id\":\"3cf7a573-f528-440c-83b9-873d7594b373\",\"owner\":\"26190a70-f2ef-440e-b092-0b904c062c08\"}",s);
	}	
	
	@Test
	public void testCyclicMarshallingHibernateProxyInitialized() throws JsonGenerationException,
			JsonMappingException, IOException {
		String s = getJSONStringForEntity(false, true);
		assertEquals("{\n" + 
				"  \"id\" : \"3cf7a573-f528-440c-83b9-873d7594b373\",\n" + 
				"  \"owner\" : \"26190a70-f2ef-440e-b092-0b904c062c08\"\n" + 
				"}",s);
	}

	private String getJSONStringForEntity(boolean uninitialized, boolean prettyPrint) throws IOException,
			JsonGenerationException, JsonMappingException {
		EntityHibernateProxy entity = new EntityHibernateProxy(uninitialized);
		entity.setId("3cf7a573-f528-440c-83b9-873d7594b373");
		
		EntityHibernateProxy entity2 = new EntityHibernateProxy(uninitialized);
		entity2.setId("26190a70-f2ef-440e-b092-0b904c062c08");
		entity2.setOwner(entity);
		entity.setOwner(entity2);
		
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		Hibernate4Module hibernate4Module = new Hibernate4Module();
		hibernate4Module.enable(Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS);
		objectMapper.registerModule(hibernate4Module);
		if(prettyPrint) {
			objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		}
		objectMapper.registerModule(new JaxbAnnotationModule());
		
		StringWriter stringWriter = new StringWriter();
		objectMapper.writeValue(stringWriter, entity);
		String s = stringWriter.toString();
		return s;
	}

}
