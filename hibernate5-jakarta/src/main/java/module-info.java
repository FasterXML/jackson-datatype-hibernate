module tools.jackson.datatype.hibernate5.jakarta {
    requires transitive tools.jackson.core;
    requires transitive tools.jackson.databind;
    requires transitive org.hibernate.orm.core;

    requires static com.fasterxml.jackson.annotation;

    requires static jakarta.activation;
    requires static jakarta.persistence;
    requires static java.desktop; // for java.beans

    exports tools.jackson.datatype.hibernate5.jakarta;
    opens tools.jackson.datatype.hibernate5.jakarta;

    provides tools.jackson.databind.JacksonModule with
        tools.jackson.datatype.hibernate5.jakarta.Hibernate5JakartaModule;
}
