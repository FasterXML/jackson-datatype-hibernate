package com.fasterxml.jackson.datatype.hibernate4;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class IdentifierTypeFactoryTest {

    @Test
    public void testCreateInstanceWithIdValue() throws Exception {
        IdentifierTypeFactory s = new IdentifierTypeFactory();
        Object object = s.createInstanceWithIdValue(TestClassA.class, "id", 1L);
        Assert.assertEquals(TestClassA.class, object.getClass());
        Assert.assertEquals(((SuperClass) object).id.longValue(), 1L);
        @SuppressWarnings("rawtypes")
        Map cache = IdentifierTypeFactory._idFieldCache;
        Assert.assertTrue(cache.size() == 1);
    }

    static class SuperClass {
        private Long id;
    }

    static class TestClassA extends SuperClass {
        String name;
    }

}
