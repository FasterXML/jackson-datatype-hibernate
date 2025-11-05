module tools.jackson.datatype.hibernate6 {
    requires transitive tools.jackson.core;
    requires transitive tools.jackson.databind;
    requires transitive org.hibernate.orm.core;

    requires static com.fasterxml.jackson.annotation;
    //requires static jakarta.activation;
    requires jakarta.persistence; // Non-static for tests - needed for JPA provider loading
    requires static java.desktop; // for java.beans

    // Test dependencies
    requires org.junit.jupiter.api;
    requires jakarta.transaction;

    exports tools.jackson.datatype.hibernate6;
    opens tools.jackson.datatype.hibernate6;

    // Export and open test packages
    exports tools.jackson.datatype.hibernate6.data;
    opens tools.jackson.datatype.hibernate6.data;

    provides tools.jackson.databind.JacksonModule with
        tools.jackson.datatype.hibernate6.Hibernate6Module;

    uses jakarta.persistence.spi.PersistenceProvider;
}
