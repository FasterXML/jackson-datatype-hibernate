Project to build [Jackson](../../../jackson) module (jar) to
support JSON serialization and deserialization of Hibernate (https://hibernate.org) specific datatypes
and properties; especially lazy-loading aspects.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.fasterxml.jackson.datatype/jackson-datatype-hibernate5/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.fasterxml.jackson.datatype/jackson-datatype-hibernate5/)
[![Javadoc](https://javadoc.io/badge/com.fasterxml.jackson.datatype/jackson-datatype-hibernate5.svg)](https://www.javadoc.io/doc/com.fasterxml.jackson.datatype/jackson-datatype-hibernate5)

## Status

As of version 2.0 module is usable and used by non-trivial number of developers and projects.

Note: Hibernate 4.x and 5.x are supported (5.x starting with Jackson 2.6),
but they require different jar, and Maven artifact names (and jar names differ).
This document refers to "Hibernate 5" version, but changes with 4.x should require
little more than replacing "5" in names with "4".

Hibernate 3.x was supported up to Jackson 2.12 but is no longer supported at and after 2.13

Jackson 2.13 adds Support for "Hibernate 5 Jakarta" variant (for Hibernate 5.5 and beyond);
see below for more information.

Jackson 2.15 adds Support for Hibernate 6.x;
see below for more information.

### JDK requirements

Before Jackson 2.15, baseline JDK needed for building for JDK 8 and all
module variants worked on Java 8.

With Jackson 2.15, JDK 11 will be required to build: all modules run on
Java 8 except for Hibernate 6.x module which requires Java 11 like
Hibernate 6.x itself.

### Javax vs Jakarta

Due to changes related to
[Java EE to Jakarta EE](https://blogs.oracle.com/javamagazine/post/transition-from-java-ee-to-jakarta-ee)
transition (also known as "JAXB to Jakarta" etc etc), there are 2 variants of Hibernate 5 module:

* One that works with "old" JAXB/JavaEE APIs: `jackson-datatype-hibernate5`
* One that works with "new" Jakarta APIs: `jackson-datatype-hibernate5-jakarta`

Note that for Hibernate 4.x only old APIs matter; and for 6.x and later only new (Jakarta)
APIs are used -- so there are no separate modules.

## Usage

### Maven dependency

To use module on Maven-based projects, use following dependency
(with whatever is the latest version available):

```xml
<dependency>
  <groupId>com.fasterxml.jackson.datatype</groupId>
  <artifactId>jackson-datatype-hibernate5</artifactId>
  <version>2.14.1</version>
</dependency>    
```

or whatever version is most up-to-date at the moment;

Note that you need to use "jackson-datatype-hibernate4" for Hibernate 4.x.

```xml
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-hibernate4</artifactId>
    <version>2.14.1</version>
</dependency>
```

if you plan to use Hibernate 5.5 with the Jakarta Persistence API 3.0;
you will need the jakarta suffixed dependency for Hibernate 5.5:

```xml
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-hibernate5-jakarta</artifactId>
    <version>2.14.1</version>
</dependency>
```

you will need to use "jackson-datatype-hibernate6" for Hibernate 6.x (when v2.15.0 is released):

```xml
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-hibernate6</artifactId>
    <version>2.15.0</version>
</dependency>
```

### Registering module

Like all standard Jackson modules (libraries that implement Module interface), registration is done as follows:

```java
ObjectMapper mapper = new ObjectMapper();
// for Hibernate 4.x:
mapper.registerModule(new Hibernate4Module());
// or, for Hibernate 5.x
mapper.registerModule(new Hibernate5Module());
// or, for Hibernate 5.5+ with Jakarta
mapper.registerModule(new Hibernate5JakartaModule());
// or, for Hibernate 6.x
mapper.registerModule(new Hibernate6Module());
```

after which functionality is available for all normal Jackson operations.

### Avoiding infinite loops

* https://www.baeldung.com/jackson-bidirectional-relationships-and-infinite-recursion

### Using with Spring MVC

Although more common way would be to register the module explicitly, it is alternatively
possible to just sub-class ObjectMapper and register the module in constructor.

```java
public class HibernateAwareObjectMapper extends ObjectMapper {
    public HibernateAwareObjectMapper() {
        // This for Hibernate 5; change 5 to 4 if you need to support
        // Hibernate 4 instead
        registerModule(new Hibernate5Module());
    }
}
```    

Then add it as the objectmapper to be used

```xml
    <mvc:annotation-driven>
        <mvc:message-converters>
            <!-- Use the HibernateAware mapper instead of the default -->
            <bean class="org.springframework.http.converter.json.MappingJackson2HttpMessageConverter">
                <property name="objectMapper">
                    <bean class="path.to.your.HibernateAwareObjectMapper" />
                </property>
            </bean>
        </mvc:message-converters>
    </mvc:annotation-driven>
```

If mvc:annotation-driven is not being used, it can be added as a jsonconverter to the messageconverters of RequestMappingHandlerAdapter.

## Other

Project [Wiki](../../wiki) contains links to Javadocs and downloadable jars (from Central Maven repository).
