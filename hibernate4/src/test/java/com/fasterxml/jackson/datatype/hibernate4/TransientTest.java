package com.fasterxml.jackson.datatype.hibernate4;

import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

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

     public static interface PublicView {}
     public static interface OtherView {}

     @JsonPropertyOrder({"a", "b"})
     static class WithTransientAndView {
          public int a = 3;

          @JsonView(PublicView.class)
          @Transient
          public int b = 4;
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
          Hibernate4Module mod = hibernateModule(false);
          mod.disable(Hibernate4Module.Feature.USE_TRANSIENT_ANNOTATION);
          mapper = new ObjectMapper().registerModule(mod);
          
          assertEquals(aposToQuotes("{'a':1,'b':2}"), mapper.writeValueAsString(new WithTransient()));
     }

     public void testTransientWithView() throws Exception
     {
          ObjectMapper mapper = mapperWithModule(false);
          assertEquals(aposToQuotes("{'a':3}"),
                  mapper.writerWithView(PublicView.class)
                  .writeValueAsString(new WithTransientAndView()));

          Hibernate4Module mod = hibernateModule(false);
          mod.disable(Hibernate4Module.Feature.USE_TRANSIENT_ANNOTATION);
          mapper = new ObjectMapper().registerModule(mod);
          
          assertEquals(aposToQuotes("{'a':3,'b':4}"),
                  mapper.writerWithView(PublicView.class)
                  .writeValueAsString(new WithTransientAndView()));

          // although not if not within view
          assertEquals(aposToQuotes("{'a':3}"),
                  mapper.writerWithView(OtherView.class)
                  .writeValueAsString(new WithTransientAndView()));
     }
}
