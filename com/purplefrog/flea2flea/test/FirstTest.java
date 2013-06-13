/*
 * Copyright 2002
 * Robert Forsman <thoth@purplefrog.com>
 */
package com.purplefrog.flea2flea.test;

import java.util.regex.*;

import com.purplefrog.flea2flea.*;
import junit.framework.*;

public class FirstTest
	extends TestCase
{
    public FirstTest(String s)
    {
	super(s);
    }

    public void testMatch()
    {
	{
	    Matcher m = OutboundHandler.byteRangePattern.matcher("bytes=14-");
	    assertTrue(m.matches());
	    assertEquals(2, m.groupCount());
	    assertEquals("14", m.group(1));
	    String s = m.group(2);
	    assertNull(s);
	}

	{
	    Matcher m = OutboundHandler.byteRangePattern.matcher("bytes=-666");
	    assertTrue(m.matches());
	    assertEquals(2, m.groupCount());
	    assertNull(m.group(1));
	    String s = m.group(2);
	    assertEquals("666", s);
	}
    }

    public void testBasename()
    {
	assertEquals("Hate", IncomingHandler.basename("D:\\Windows\\Desktop\\Hate"));
	assertEquals("boobies", IncomingHandler.basename("/usr/local/pr0n/boobies"));
    }

    public static TestSuite suite()
    {
	return new TestSuite(FirstTest.class);
    }
}
