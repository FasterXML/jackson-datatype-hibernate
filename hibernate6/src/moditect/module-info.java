module tools.jackson.datatype.hibernate6 {
    requires transitive tools.jackson.core;
    requires transitive tools.jackson.databind;
    requires transitive org.hibernate.orm.core;

    requires static com.fasterxml.jackson.annotation;
    requires static jakarta.activation;
    requires static jakarta.persistence;

    exports tools.jackson.datatype.hibernate6;
    opens tools.jackson.datatype.hibernate6;

    provides tools.jackson.databind.JacksonModule with
        tools.jackson.datatype.hibernate6.Hibernate6Module;
}
