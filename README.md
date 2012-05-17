Project to build Jackson (http://jackson.codehaus.org) module (jar) to support JSON serialization and deserialization of Hibernate (http://hibernate.org) specific datatypes and properties; especially lazy-loading aspects.

## Status

As of version 2.0.2 module is usable and used by non-trivial number of developers and projects.
Rough edges may still exist; please report any bugs you find.

Note: both Hibernate 3 and 4 are supported, but they require different jar, and Maven artifact names (and jar names differ).
This document refers to "Hibernate 4" version, but changes with 3 should be little more than replacing "4" in names with "3".

## Usage

### Maven dependency

To use module on Maven-based projects, use following dependency:

    <dependency>
      <groupId>com.fasterxml.jackson.datatype</groupId>
      <artifactId>jackson-datatype-hibernate4</artifactId>
      <version>2.0.0</version>
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
