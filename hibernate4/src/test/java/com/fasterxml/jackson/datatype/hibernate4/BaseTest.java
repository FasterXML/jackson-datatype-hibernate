package com.fasterxml.jackson.datatype.hibernate4;

import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate4.Hibernate4Module;

public abstract class BaseTest extends junit.framework.TestCase
{
    protected BaseTest() { }
    
    protected ObjectMapper mapperWithModule(boolean forceLazyLoading)
    {
        ObjectMapper mapper = new ObjectMapper();
        Hibernate4Module mod = new Hibernate4Module();
        mod.configure(Hibernate4Module.Feature.FORCE_LAZY_LOADING, forceLazyLoading);
        mapper.registerModule(mod);
        return mapper;
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
