// Hibernate 5 (jakarta) Jackson datatype module: Test
module tools.jackson.datatype.hibernate5.jakarta
{
    requires tools.jackson.core;
    requires tools.jackson.databind;
    requires static com.fasterxml.jackson.annotation;

    requires org.hibernate.orm.core;

    requires static jakarta.activation;
    requires static jakarta.persistence;

    exports tools.jackson.datatype.hibernate5.jakarta;
    opens tools.jackson.datatype.hibernate5.jakarta;

    // Test additions
    requires org.junit.jupiter.api;

    exports tools.jackson.datatype.hibernate5.jakarta.data;
    opens tools.jackson.datatype.hibernate5.jakarta.data;

    provides tools.jackson.databind.JacksonModule with
        tools.jackson.datatype.hibernate5.jakarta.Hibernate5JakartaModule;
}
