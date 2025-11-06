package com.fasterxml.jackson.datatype.hibernate4;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.Versioned;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestVersions extends BaseTest
{
    @Test
    public void testMapperVersions()
    {
        assertVersion(new Hibernate4Module());
    }

    private void assertVersion(Versioned vers)
    {
        Version v = vers.version();
        assertFalse(v.isUnknownVersion(), "Should find version information (got "+v+")");
//        Version exp = PackageVersion.VERSION;
//        assertEquals(exp.toFullString(), v.toFullString());
//        assertEquals(exp, v);
    }
}

