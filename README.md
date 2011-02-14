Project to build Jackson (http://jackson.codehaus.org) module (jar) to support JSON serialization and deserialization of Hibernate (http://hibernate.org) specific datatypes and properties; especially lazy-loading aspects.

== Usage ==

h2. Using with Spring MVC

(as contributed by Frank Hess)

First step: sub-class ObjectMapper and register the module

   public class HibernateAwareObjectMapper extends ObjectMapper {
     public HibernateAwareObjectMapper() {
       HibernateModule hm = new HibernateModule();
       registerModule(hm);
       configure(Feature.FAIL_ON_EMPTY_BEANS, false);
     }

     public void setPrettyPrint(boolean prettyPrint) {
       configure(Feature.INDENT_OUTPUT, prettyPrint);
     }
   }

Second step register the new ObjectMapper:

  <bean class="org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter">
    <property name="messageConverters">
      <array>
        <bean id="jsonConverter"
      	   class="org.springframework.http.converter.json.MappingJacksonHttpMessageConverter">
          <property name="objectMapper">
            <bean class="campus.authorweb.util.HibernateAwareObjectMapper"/>
          </property>
        </bean>
      </array>
    </property>
  </bean>

This seems to do the trick.
