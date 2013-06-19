package com.purplefrog.rfc2046;

import java.util.regex.*;

/**
* Created with IntelliJ IDEA.
* User: thoth
* Date: 6/19/13
* Time: 3:22 PM
* To change this template use File | Settings | File Templates.
*/
public class Header
        implements Comparable
{
    public static final Pattern colon = Pattern.compile("\\s*:\\s*");
    public final String raw;
    public final String lowerKey;

    public final String value;

    public Header(String str)
    {
        raw = str;
        Matcher m = colon.matcher(raw);
        if (m.find()) {
            lowerKey = raw.substring(0, m.start()).toLowerCase();
            value = raw.substring(m.end());
        } else {
            lowerKey = raw.toLowerCase();
            value = null;
        }

        if (lowerKey.charAt(0) == '\n')
            new Exception("WTF?").printStackTrace();
    }

    public int compareTo(Object o)
    {
        return lowerKey().compareTo(((Header)o).lowerKey());
    }

    public String lowerKey()
    {
        return lowerKey;
    }
}
