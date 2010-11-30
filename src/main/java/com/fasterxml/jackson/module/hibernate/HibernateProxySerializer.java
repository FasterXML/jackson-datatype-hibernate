package com.fasterxml.jackson.module.hibernate;

import java.io.IOException;

import org.hibernate.proxy.HibernateProxy;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

public class HibernateProxySerializer extends JsonSerializer<HibernateProxy>
{

    @Override
    public void serialize(HibernateProxy value, JsonGenerator jgen,
            SerializerProvider provider) throws IOException,
            JsonProcessingException {
        // TODO Auto-generated method stub
        
    }

    public void serializeWithType(HibernateProxy value, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonProcessingException
    {
        // TODO
    }
}
