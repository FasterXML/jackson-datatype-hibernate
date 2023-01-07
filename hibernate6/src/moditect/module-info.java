module com.fasterxml.jackson.datatype.hibernate6 {
    requires transitive com.fasterxml.jackson.core;
    requires transitive com.fasterxml.jackson.databind;
    requires transitive org.hibernate.orm.core;

    requires static com.fasterxml.jackson.annotation;
    requires static jakarta.activation;
    requires static jakarta.persistence;

    exports com.fasterxml.jackson.datatype.hibernate6;
    opens com.fasterxml.jackson.datatype.hibernate6;

    provides com.fasterxml.jackson.databind.Module with
        com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
}
