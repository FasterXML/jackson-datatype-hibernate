package tools.jackson.datatype.hibernate5;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InclusionTest extends BaseTest
{
    static class Mock
    {
        public long id = 13;
        public Set<String> mocks = new LinkedHashSet<String>();
    }

    // [hibernate#65]
    @Test
    public void testInclusion() throws Exception
    {
        final JsonMapper.Builder builder = mapperBuilderWithModule(false);
        builder.changeDefaultPropertyInclusion(inc -> inc.withValueInclusion(Include.NON_EMPTY));
        ObjectMapper mapper = builder.build();
        String json = mapper.writeValueAsString(new Mock());
        assertEquals("{\"id\":13}", json);
    }
}
