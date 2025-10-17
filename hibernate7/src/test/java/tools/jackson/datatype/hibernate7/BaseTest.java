package tools.jackson.datatype.hibernate7;

import java.util.Arrays;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.apache.log4j.Logger;

import static org.junit.jupiter.api.Assertions.fail;

public abstract class BaseTest
{
    protected BaseTest() {
        try {
            Logger.getLogger(this.getClass()).info("Testing using hibernate " + Hibernate7Version.getHibernateVersion() +
                    ", is 7+: " + Hibernate7Version.isHibernate7_Plus());
        } catch (Exception e) {
            // Should not happen
            throw new RuntimeException(e);
        }
    }

    protected JsonMapper.Builder mapperBuilderWithModule(boolean forceLazyLoading)
    {
        return JsonMapper.builder().addModule(hibernateModule(forceLazyLoading, false));
    }

    protected ObjectMapper mapperWithModule(boolean forceLazyLoading)
    {
        return mapperBuilderWithModule(forceLazyLoading).build();
    }

    protected ObjectMapper mapperWithModule(boolean forceLazyLoading, boolean nullMissingEntities)
    {
        return JsonMapper.builder().addModule(hibernateModule(forceLazyLoading, nullMissingEntities)).build();
    }
    
    protected Hibernate7Module hibernateModule(boolean forceLazyLoading)
    {
        return  hibernateModule(forceLazyLoading, false);
    }

    protected Hibernate7Module hibernateModule(boolean forceLazyLoading, boolean nullMissingEntities)
    {
    	Hibernate7Module mod = new Hibernate7Module();
        mod.configure(Hibernate7Module.Feature.FORCE_LAZY_LOADING, forceLazyLoading);
        mod.configure(Hibernate7Module.Feature.WRITE_MISSING_ENTITIES_AS_NULL, nullMissingEntities);
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
