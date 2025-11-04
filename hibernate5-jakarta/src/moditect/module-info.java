module tools.jackson.datatype.hibernate5.jakarta {
    requires transitive tools.jackson.core;
    requires transitive tools.jackson.databind;
    requires transitive org.hibernate.orm.core;

    requires static tools.jackson.annotation;
    requires static jakarta.activation;
    requires static jakarta.persistence;

    exports tools.jackson.datatype.hibernate5.jakarta;
    opens tools.jackson.datatype.hibernate5.jakarta;

    provides tools.jackson.databind.JacksonModule with
        tools.jackson.datatype.hibernate5.jakarta.Hibernate5JakartaModule;
}
