package com.fasterxml.jackson.datatype.hibernate5;

import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit test for [#61]
 */
public class TransientTest extends BaseTest
{
     @JsonPropertyOrder({"a", "b"})
     static class WithTransient {
          public int a = 1;

          @Transient
          public int b = 2;
     }

     /*
     /**********************************************************************
     /* Test methods
     /**********************************************************************
      */

     public void testSimpleTransient() throws Exception
     {
          // First, with defaults, which allow use of Transient
          ObjectMapper mapper = mapperWithModule(false);
          assertEquals(aposToQuotes("{'a':1}"), mapper.writeValueAsString(new WithTransient()));

          // and then with Transient disabled
          Hibernate5Module mod = hibernateModule(false);
          mod.disable(Hibernate5Module.Feature.USE_TRANSIENT_ANNOTATION);
          mapper = new ObjectMapper().registerModule(mod);
          
          assertEquals(aposToQuotes("{'a':1,'b':2}"), mapper.writeValueAsString(new WithTransient()));
     }
}
