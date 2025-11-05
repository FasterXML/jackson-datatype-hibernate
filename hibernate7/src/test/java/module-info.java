module tools.jackson.datatype.hibernate7 {
    requires transitive tools.jackson.core;
    requires transitive tools.jackson.databind;
    requires transitive org.hibernate.orm.core;

    requires static com.fasterxml.jackson.annotation;
    requires static jakarta.activation;
    requires static jakarta.persistence;
    requires static java.desktop; // for java.beans

    // Test dependencies
    requires org.junit.jupiter.api;
    requires jakarta.transaction;

    exports tools.jackson.datatype.hibernate7;
    opens tools.jackson.datatype.hibernate7;

    // Export and open test packages
    exports tools.jackson.datatype.hibernate7.data;
    opens tools.jackson.datatype.hibernate7.data;

    provides tools.jackson.databind.JacksonModule with
        tools.jackson.datatype.hibernate7.Hibernate7Module;

    uses jakarta.persistence.spi.PersistenceProvider;
}
