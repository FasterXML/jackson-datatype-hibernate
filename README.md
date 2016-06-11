Project to build [Jackson](../../../jackson) module (jar) to
support JSON serialization and deserialization of Hibernate (http://hibernate.org) specific datatypes
and properties; especially lazy-loading aspects.

[![Build Status](https://travis-ci.org/FasterXML/jackson-datatype-hibernate.svg?branch=master)](https://travis-ci.org/FasterXML/jackson-datatype-hibernate)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.fasterxml.jackson.datatype/jackson-datatype-hibernate5/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.fasterxml.jackson.datatype/jackson-datatype-hibernate5/)
[![Javadoc](https://javadoc-emblem.rhcloud.com/doc/com.fasterxml.jackson.datatype/jackson-datatype-hibernate5/badge.svg)](http://www.javadoc.io/doc/com.fasterxml.jackson.datatype/jackson-datatype-hibernate5)

## Status

As of version 2.0 module is usable and used by non-trivial number of developers and projects.

Note: Hibernate 3.x, 4.x and 5.x are supported (5.x starting with Jackson 2.6),
but they require different jar, and Maven artifact names (and jar names differ).
This document refers to "Hibernate 4" version, but changes with 3.x or 5.x should require
little more than replacing "4" in names with "3" or "5".

## Usage

### Maven dependency

To use module on Maven-based projects, use following dependency:

```xml
<dependency>
  <groupId>com.fasterxml.jackson.datatype</groupId>
  <artifactId>jackson-datatype-hibernate4</artifactId>
  <version>2.5.3</version>
</dependency>    
```

(or whatever version is most up-to-date at the moment;
note that you need to use "jackson-datatype-hibernate3" for Hibernate 3.x,
or "jackson-datatype-hibernate5" for Hibernate 5.x)

### Registering module

Like all standard Jackson modules (libraries that implement Module interface), registration is done as follows:

```java
ObjectMapper mapper = new ObjectMapper();
// for Hibernate 4.x:
mapper.registerModule(new Hibernate4Module());
// or, for Hibernate 5.x
mapper.registerModule(new Hibernate5Module());
// or, for Hibernate 3.6
mapper.registerModule(new Hibernate3Module());
```

after which functionality is available for all normal Jackson operations.

Note that there are actuall 

### Using with Spring MVC

Sub-class ObjectMapper and register the module (Hibernate 3, 4 or 5)

```java
public class HibernateAwareObjectMapper extends ObjectMapper {

    public HibernateAwareObjectMapper() {
        registerModule(new Hibernate4Module());
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
