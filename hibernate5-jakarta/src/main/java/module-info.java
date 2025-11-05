// Hibernate 5 (jakarta) Jackson datatype module
module tools.jackson.datatype.hibernate5.jakarta
{
    requires tools.jackson.core;
    requires tools.jackson.databind;
    requires static com.fasterxml.jackson.annotation;

    requires org.hibernate.orm.core;

    requires static jakarta.activation;
    requires static jakarta.persistence;
    requires static java.desktop; // for java.beans

    exports tools.jackson.datatype.hibernate5.jakarta;
    opens tools.jackson.datatype.hibernate5.jakarta;

    provides tools.jackson.databind.JacksonModule with
        tools.jackson.datatype.hibernate5.jakarta.Hibernate5JakartaModule;
}
