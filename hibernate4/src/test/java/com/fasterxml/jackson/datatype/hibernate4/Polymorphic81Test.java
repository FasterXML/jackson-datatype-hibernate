package com.fasterxml.jackson.datatype.hibernate4;

import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Polymorphic81Test extends BaseTest
{
    static class TestLayout {
        public RecordFilters recordFilters = new RecordFilters();

        public TestLayout addRecordFilter(AbstractRecordFilter recordFilter) {
            recordFilters.filters.add(recordFilter);
            return this;
        }
    }

    static class RecordFilters implements RecordFilter {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
        @JsonSubTypes({
            @JsonSubTypes.Type(value = EmptyRecordFilter.class)
//        ,@JsonSubTypes.Type(value = RangeRecordFilter.class) }
        })
        public List<AbstractRecordFilter> filters = new ArrayList<AbstractRecordFilter>();
    }

    public interface RecordFilter { }

    static abstract class AbstractRecordFilter implements RecordFilter { }

    @JsonTypeName(value="emptyRecordFilter")
    static class EmptyRecordFilter extends AbstractRecordFilter {
        Expression expr;

        EmptyRecordFilter() {
        }

        public EmptyRecordFilter(Expression expr) {
            this.expr = expr;
        }
    }    

    static class Expression {
        private String expression;

        Expression() { }

        public Expression(String expression) {
            this.expression = expression;
        }

        public String getExpression() {
            return expression;
        }

        public static Expression simpleFieldExpression(String field) {
            return new Expression("${" + field + "}");
        }
    }

    public void testPolymorphic81() throws Exception
    {
        final ObjectMapper mapper = mapperWithModule(true);

        final TestLayout layout = new TestLayout()
            .addRecordFilter(new EmptyRecordFilter(Expression.simpleFieldExpression("id")));

        // uncomment the line below to see the problem
        // mapper.registerModule(new Hibernate4Module());

        String json =  mapper.writerWithDefaultPrettyPrinter().writeValueAsString(layout);
//        System.out.println("OUTPUT:\n"+json);

        final TestLayout restoredLayout =
            mapper.readerFor(TestLayout.class).readValue(json);
        assertNotNull(restoredLayout);
    }
}
