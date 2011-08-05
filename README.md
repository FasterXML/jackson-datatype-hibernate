Project to build Jackson (http://jackson.codehaus.org) module (jar) to support JSON serialization and deserialization of Hibernate (http://hibernate.org) specific datatypes and properties; especially lazy-loading aspects.

## Usage

### Maven dependency

To use module on Maven-based projects, use following dependency:

    <dependency>
      <groupId>com.fasterxml</groupId>
      <artifactId>jackson-module-hibernate</artifactId>
      <version>0.7.0</version>
    </dependency>    

(or whatever version is most up-to-date at the moment)

### Registering module

Like all standard Jackson modules (libraries that implement Module interface), registration is done as follows:

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new HibernateModule());

after which functionality is available for all normal Jackson operations.

### Using with Spring MVC

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
