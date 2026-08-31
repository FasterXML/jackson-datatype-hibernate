package tools.jackson.datatype.hibernate7;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.util.StdConverter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that {@link CycleDetectingSerializer} forwards the parts of the
 * {@code ValueSerializer} contract that {@code BeanSerializerBase} relies on.
 * The modifier wraps <em>every</em> {@code @Entity} bean serializer when
 * {@code FORCE_LAZY_LOADING} is enabled, so anything the wrapper fails to
 * delegate is silently dropped for all entity types.
 *<p>
 * No Hibernate session is needed here: the wrapping decision is made purely
 * from the {@code @Entity} annotation.
 */
public class CycleDetectingSerializerDelegationTest extends BaseTest
{
    static class UpperCaseConverter extends StdConverter<String, String> {
        @Override
        public String convert(String value) {
            return (value == null) ? null : value.toUpperCase();
        }
    }

    @Entity
    static class ConverterEntity {
        @Id
        public Integer id = 1;

        // Applied only by BeanSerializerBase.resolve(); dropped if the
        // wrapper does not delegate resolve()
        @JsonSerialize(converter = UpperCaseConverter.class)
        public String name = "shouty";
    }

    @Entity
    static class IgnoredPropsEntity {
        @Id
        public Integer id = 2;

        // Applied only by BeanSerializerBase.createContextual(); dropped if
        // the wrapper does not delegate createContextual()
        @JsonIgnoreProperties("secret")
        public Nested nested = new Nested();
    }

    @Entity
    static class Nested {
        @Id
        public Integer id = 3;

        public String visible = "yes";

        public String secret = "no";
    }

    // [datatype-hibernate#204]: resolve() must reach the wrapped bean serializer
    @Test
    public void testConverterAppliedOnWrappedEntity() throws Exception
    {
        ObjectMapper mapper = mapperWithModule(true);
        String json = mapper.writeValueAsString(new ConverterEntity());
        assertThat(json).contains("\"name\":\"SHOUTY\"");
    }

    // [datatype-hibernate#204]: createContextual() must reach the wrapped bean serializer
    @Test
    public void testPerPropertyIgnoralAppliedOnWrappedEntity() throws Exception
    {
        ObjectMapper mapper = mapperWithModule(true);
        String json = mapper.writeValueAsString(new IgnoredPropsEntity());
        assertThat(json).contains("\"visible\":\"yes\"");
        assertThat(json).doesNotContain("secret");
    }
}
