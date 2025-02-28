package com.fasterxml.jackson.datatype.hibernate4;

import javax.persistence.Transient;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
     public static interface PrivateView {}

     @JsonPropertyOrder({"aaa", "bbb", "ccc", "ddd"})
     static class WithTransientAndView {
         public String aaa = "xxx";
         @Transient
         public String bbb = "xxx";
         @Transient
         @JsonView(PublicView.class)
         public String ccc = "xxx";
         @JsonView(PrivateView.class)
         public String ddd = "xxx";
     }

     /*
     /**********************************************************************
     /* Test methods
     /**********************************************************************
      */

    @Test
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

     @Test
     public void testTransientWithView() throws Exception
     {
          ObjectMapper mapper = mapperWithModule(false);
          assertEquals(aposToQuotes("{'aaa':'xxx'}"),
                  mapper.writerWithView(PublicView.class)
                  .writeValueAsString(new WithTransientAndView()));
          assertEquals(aposToQuotes("{'aaa':'xxx','ddd':'xxx'}"),
                  mapper.writerWithView(PrivateView.class)
                  .writeValueAsString(new WithTransientAndView()));
     }
}
