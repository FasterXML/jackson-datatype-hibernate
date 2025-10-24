package tools.jackson.datatype.hibernate5;

import javax.persistence.Transient;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

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
          Hibernate5Module mod = hibernateModule(false);
          mod.disable(Hibernate5Module.Feature.USE_TRANSIENT_ANNOTATION);
          mapper = JsonMapper.builder().addModule(mod).build();
          
          assertEquals(aposToQuotes("{'a':1,'b':2}"), mapper.writeValueAsString(new WithTransient()));
     }

     @Test
     public void testTransientWithView() throws Exception
     {
          ObjectMapper mapper = mapperBuilderWithModule(false)
                  .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                  .build();
          assertEquals(aposToQuotes("{'aaa':'xxx'}"),
                  mapper.writerWithView(PublicView.class)
                  .writeValueAsString(new WithTransientAndView()));
          assertEquals(aposToQuotes("{'aaa':'xxx','ddd':'xxx'}"),
                  mapper.writerWithView(PrivateView.class)
                  .writeValueAsString(new WithTransientAndView()));
     }
}
