package com.fasterxml.jackson.datatype.hibernate5;

import java.util.Arrays;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class BaseTest extends junit.framework.TestCase
{
    protected BaseTest() {
    	try {
    		System.out.println(Hibernate5Version.getHibernateVersion());
    		System.out.println(Hibernate5Version.isHibernate5_2_Plus());
			Logger.getLogger(this.getClass()).info("Testing using hibernate " + Hibernate5Version.getHibernateVersion() +
					", is 5.2+: " + Hibernate5Version.isHibernate5_2_Plus());
		} catch (Exception e) {
			// Should not happen
			throw new RuntimeException(e);
		}
    }

    protected ObjectMapper mapperWithModule(boolean forceLazyLoading)
    {
        return new ObjectMapper().registerModule(hibernateModule(forceLazyLoading));
    }

    protected Hibernate5Module hibernateModule(boolean forceLazyLoading)
    {
        Hibernate5Module mod = new Hibernate5Module();
        mod.configure(Hibernate5Module.Feature.FORCE_LAZY_LOADING, forceLazyLoading);
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
