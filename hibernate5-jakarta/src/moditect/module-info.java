module com.fasterxml.jackson.datatype.hibernate5.jakarta {
    requires transitive com.fasterxml.jackson.core;
    requires transitive com.fasterxml.jackson.databind;
    requires transitive org.hibernate.orm.core;

    requires static com.fasterxml.jackson.annotation;
    requires static jakarta.activation;
    requires static jakarta.persistence;

    exports com.fasterxml.jackson.datatype.hibernate5.jakarta;
    opens com.fasterxml.jackson.datatype.hibernate5.jakarta;

    provides com.fasterxml.jackson.databind.Module with
        com.fasterxml.jackson.datatype.hibernate5.jakarta.Hibernate5JakartaModule;
}
