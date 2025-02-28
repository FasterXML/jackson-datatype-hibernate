package com.fasterxml.jackson.datatype.hibernate5.jakarta;

import java.util.Arrays;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.fail;

public abstract class BaseTest
{
    protected BaseTest() {
    	try {
    		System.out.println(Hibernate5JakartaVersion.getHibernateVersion());
    		System.out.println(Hibernate5JakartaVersion.isHibernate5_5_Plus());
			Logger.getLogger(this.getClass()).info("Testing using hibernate " + Hibernate5JakartaVersion.getHibernateVersion() +
					", is 5.5+: " + Hibernate5JakartaVersion.isHibernate5_5_Plus());
		} catch (Exception e) {
			// Should not happen
			throw new RuntimeException(e);
		}
    }

    protected ObjectMapper mapperWithModule(boolean forceLazyLoading)
    {
        return new ObjectMapper().registerModule(hibernateModule(forceLazyLoading, false));
    }

    protected ObjectMapper mapperWithModule(boolean forceLazyLoading, boolean nullMissingEntities)
    {
        return new ObjectMapper().registerModule(hibernateModule(forceLazyLoading, nullMissingEntities));
    }
    
    protected Hibernate5JakartaModule hibernateModule(boolean forceLazyLoading)
    {
        return  hibernateModule(forceLazyLoading, false);
    }

    protected Hibernate5JakartaModule hibernateModule(boolean forceLazyLoading, boolean nullMissingEntities)
    {
    	Hibernate5JakartaModule mod = new Hibernate5JakartaModule();
        mod.configure(Hibernate5JakartaModule.Feature.FORCE_LAZY_LOADING, forceLazyLoading);
        mod.configure(Hibernate5JakartaModule.Feature.WRITE_MISSING_ENTITIES_AS_NULL, nullMissingEntities);
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
