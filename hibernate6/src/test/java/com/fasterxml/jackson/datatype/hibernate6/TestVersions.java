package com.fasterxml.jackson.datatype.hibernate6;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.Versioned;

public class TestVersions extends BaseTest
{
    public void testMapperVersions()
    {
        assertVersion(new Hibernate6Module());
    }

    private void assertVersion(Versioned vers)
    {
        Version v = vers.version();
        assertFalse("Should find version information (got "+v+")", v.isUnknownVersion());
//        Version exp = PackageVersion.VERSION;
//        assertEquals(exp.toFullString(), v.toFullString());
//        assertEquals(exp, v);
    }
}

