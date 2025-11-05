// Hibernate 6 Jackson datatype module
module tools.jackson.datatype.hibernate6
{
    requires tools.jackson.core;
    requires tools.jackson.databind;
    requires static com.fasterxml.jackson.annotation;

    requires org.hibernate.orm.core;

    requires static jakarta.activation;
    requires static jakarta.persistence;

    exports tools.jackson.datatype.hibernate6;
    opens tools.jackson.datatype.hibernate6;

    provides tools.jackson.databind.JacksonModule with
        tools.jackson.datatype.hibernate6.Hibernate6Module;
}
