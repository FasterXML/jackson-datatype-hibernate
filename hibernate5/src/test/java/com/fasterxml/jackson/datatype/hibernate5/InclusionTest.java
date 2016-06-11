package com.fasterxml.jackson.datatype.hibernate5;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

public class InclusionTest extends BaseTest
{
    static class Mock
    {
        public long id = 13;
        public Set<String> mocks = new LinkedHashSet<String>();
    }

    // [hibernate#65]
    public void testInclusion() throws Exception
    {
        final ObjectMapper mapper = mapperWithModule(false);
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        String json = mapper.writeValueAsString(new Mock());
        assertEquals("{\"id\":13}", json);
    }
}
