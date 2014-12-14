package com.fasterxml.jackson.datatype.hibernate4;

import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate4.Hibernate4Module;
import com.fasterxml.jackson.datatype.hibernate4.Hibernate4Module.Feature;

public abstract class BaseTest extends junit.framework.TestCase
{
    protected BaseTest() { }

    protected ObjectMapper mapperWithModule()
    {
        return new ObjectMapper().registerModule(hibernateModule());
    }

    protected ObjectMapper mapperWithModule(Feature f, boolean state)
    {
        return new ObjectMapper().registerModule(hibernateModule(f, state));
    }

    protected Hibernate4Module hibernateModule()
    {
        return hibernateModule(null, false);
    }

    protected Hibernate4Module hibernateModule(Feature f, boolean state)
    {
        Hibernate4Module mod = new Hibernate4Module();
        if (f != null) {
            mod.configure(f, state);
        }
        return mod;
    }
    
    protected void verifyException(Throwable e, String... matches)
    {
        String msg = e.getMessage();
        String lmsg = (msg == null) ? "" : msg.toLowerCase();
        for (String match : matches) {
            String lmatch = match.toLowerCase();
            if (lmsg.indexOf(lmatch) >= 0) {
                return;
            }
        }
        fail("Expected an exception with one of substrings ("+Arrays.asList(matches)+"): got one with message \""+msg+"\"");
    }

    protected String aposToQuotes(String json) {
        return json.replace("'", "\"");
    }
}
