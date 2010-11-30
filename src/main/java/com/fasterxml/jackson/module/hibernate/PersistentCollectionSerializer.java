package com.fasterxml.jackson.module.hibernate;

import java.io.IOException;

import org.hibernate.collection.PersistentCollection;
import org.hibernate.collection.PersistentMap;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

public class PersistentCollectionSerializer extends JsonSerializer<PersistentCollection>
{

    @Override
    public void serialize(PersistentCollection value, JsonGenerator jgen,
            SerializerProvider provider) throws IOException,
            JsonProcessingException {
        // TODO Auto-generated method stub
        
    }

    public void serializeWithType(PersistentCollection value, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonProcessingException
    {
        // TODO
    }
    
}
