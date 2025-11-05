// Hibernate 7 Jackson datatype module
module tools.jackson.datatype.hibernate7
{
    requires tools.jackson.core;
    requires tools.jackson.databind;
    requires static com.fasterxml.jackson.annotation;

    requires org.hibernate.orm.core;

    requires static jakarta.activation;
    requires static jakarta.persistence;

    exports tools.jackson.datatype.hibernate7;
    opens tools.jackson.datatype.hibernate7;

    provides tools.jackson.databind.JacksonModule with
        tools.jackson.datatype.hibernate7.Hibernate7Module;
}
