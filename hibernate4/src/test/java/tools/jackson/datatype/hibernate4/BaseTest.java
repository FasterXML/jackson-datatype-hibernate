package tools.jackson.datatype.hibernate4;

import java.util.Arrays;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.fail;

public abstract class BaseTest
{
    protected BaseTest() { }

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

    protected Hibernate4Module hibernateModule(boolean forceLazyLoading) {
        return hibernateModule(forceLazyLoading, false);
    }
    
    protected Hibernate4Module hibernateModule(boolean forceLazyLoading, boolean nullMissingEntities)
    {
        Hibernate4Module mod = new Hibernate4Module();
        mod.configure(Hibernate4Module.Feature.FORCE_LAZY_LOADING, forceLazyLoading);
        mod.configure(Hibernate4Module.Feature.WRITE_MISSING_ENTITIES_AS_NULL, nullMissingEntities);
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
