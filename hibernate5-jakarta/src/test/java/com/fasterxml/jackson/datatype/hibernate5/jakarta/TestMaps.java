package com.fasterxml.jackson.datatype.hibernate5.jakarta;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TestMaps extends BaseTest
{
    public void testSimpleMap() throws Exception
    {
        ObjectMapper mapper = mapperWithModule(false);
        Map<String,Integer> map = new HashMap<String,Integer>();
        map.put("a", Integer.valueOf(1));
        String json = mapper.writeValueAsString(map);
        assertEquals("{\"a\":1}", json);
    }
}
