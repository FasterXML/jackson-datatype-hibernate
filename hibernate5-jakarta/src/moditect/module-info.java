module com.fasterxml.jackson.datatype.hibernate5.jakarta {
	exports com.fasterxml.jackson.datatype.hibernate5.jakarta;
	opens com.fasterxml.jackson.datatype.hibernate5.jakarta;

	requires transitive com.fasterxml.jackson.core;
	requires transitive com.fasterxml.jackson.databind;
	requires transitive org.hibernate.orm.core;

	requires static com.fasterxml.jackson.annotation;
	requires static jakarta.activation;
	requires static jakarta.persistence;
} 