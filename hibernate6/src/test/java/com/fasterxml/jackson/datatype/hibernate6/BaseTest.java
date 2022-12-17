package com.fasterxml.jackson.datatype.hibernate6;

import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.log4j.Logger;

public abstract class BaseTest extends junit.framework.TestCase
{
    protected BaseTest() {
    	try {
    		System.out.println(Hibernate6Version.isHibernate6_Plus());
			Logger.getLogger(this.getClass()).info("Testing using hibernate " + Hibernate6Version.getHibernateVersion() +
					", is 6+: " + Hibernate6Version.isHibernate6_Plus());
		} catch (Exception e) {
			// Should not happen
			throw new RuntimeException(e);
		}
    }

    protected ObjectMapper mapperWithModule(boolean forceLazyLoading)
    {
        return JsonMapper.builder().addModule(hibernateModule(forceLazyLoading, false)).build();
    }

    protected ObjectMapper mapperWithModule(boolean forceLazyLoading, boolean nullMissingEntities)
    {
        return JsonMapper.builder().addModule(hibernateModule(forceLazyLoading, nullMissingEntities)).build();
    }
    
    protected Hibernate6Module hibernateModule(boolean forceLazyLoading)
    {
        return  hibernateModule(forceLazyLoading, false);
    }

    protected Hibernate6Module hibernateModule(boolean forceLazyLoading, boolean nullMissingEntities)
    {
    	Hibernate6Module mod = new Hibernate6Module();
        mod.configure(Hibernate6Module.Feature.FORCE_LAZY_LOADING, forceLazyLoading);
        mod.configure(Hibernate6Module.Feature.WRITE_MISSING_ENTITIES_AS_NULL, nullMissingEntities);
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
