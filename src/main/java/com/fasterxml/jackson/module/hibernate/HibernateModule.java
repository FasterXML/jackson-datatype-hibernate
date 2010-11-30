package com.fasterxml.jackson.module.hibernate;

import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.*;

public class HibernateModule extends Module
{
    private final String NAME = "HibernateModule";
    
    // Should externalize this somehow
    private final static Version VERSION = new Version(0, 1, 0, null); // 0.1.0

    public HibernateModule() { }

    @Override public String getModuleName() { return NAME; }
    @Override public Version version() { return VERSION; }
    
    @Override
    public void setupModule(SetupContext context)
    {
        context.addSerializers(new HibernateSerializers());
    }

}