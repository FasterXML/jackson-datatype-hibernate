package com.fasterxml.jackson.datatype.hibernate5;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestMaps extends BaseTest
{
    @Test
    public void testSimpleMap() throws Exception
    {
        ObjectMapper mapper = mapperWithModule(false);
        Map<String,Integer> map = new HashMap<String,Integer>();
        map.put("a", Integer.valueOf(1));
        String json = mapper.writeValueAsString(map);
        assertEquals("{\"a\":1}", json);
    }
}
