/*
 * Copyright 2002
 * Robert Forsman <thoth@purplefrog.com>
 */
package com.purplefrog.flea2flea;

import com.purplefrog.apachehttpcliches.*;
import com.purplefrog.httpcliches.*;
import org.apache.http.*;
import org.apache.http.protocol.*;
import org.apache.log4j.*;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;

public class RootListing
    implements HttpRequestHandler
{
    private static final Logger logger = Logger.getLogger(RootListing.class);

    protected Map offerings;

    public RootListing(Map offerings)
    {
        this.offerings = offerings;
    }

    public void handle(HttpRequest req, HttpResponse rsp, HttpContext ctx)
        throws HttpException, IOException
    {
        RequestLine rline = req.getRequestLine();

        EntityAndHeaders rval = null;
        try {
            if ("get".equalsIgnoreCase(rline.getMethod())) {
                CGIEnvironment env = ApacheCGI.parseEnv(req, ctx);
                String sortBy = HTMLTools.firstOrNull( env.args.get("sort"));
                String payload = buildIndex(sortBy);
                rval = EntityAndHeaders.plainPayload(200, payload, "text/html");
            } else {
                rval = EntityAndHeaders.plainPayload(501, "Not Implemented", "text/plain");
            }
            } catch (URISyntaxException e) {
            logger.warn("", e);
        }

        rval.apply(rsp);

    }

    public String buildIndex(String sortby)
    {
        StringBuilder buf = new StringBuilder();
        buf.append("<html>\n<head>\n<title>Flea 2 Flea public index</title>\n</head>"
            + "\n");

        buf.append("<body>\n<h1>Flea 2 Flea public index</h1>"
            + "\n");
        buf.append(offerings.size() + " advertised files available for download"
            + "\n");
        if (0 < offerings.size()) {
            buf.append(offeringsTable(sortby)
                + "\n");
        }

        buf.append("\n<hr>"
            + "\n");
        buf.append("<form action=\"" + IncomingHandler.URI + "\" method=\"post\" enctype=\"multipart/form-data\">\n" +
            "<h2>upload</h2>\n" +
            "Tag: <input type=\"text\" name=\"tag\"><br>\n" +
            "local file to upload: <input type=\"file\" name=\"file\"> <br>\n" +
            "<input type=\"submit\" value=\"Send File\">\n" +
            "</form>"
            + "\n");
        buf.append("</body>"
            + "\n");
        return buf.toString();
    }

    StringBuffer offeringsTable(String sortby)
    {
        StringBuffer rval = new StringBuffer();

	DateFormat df = DateFormat.getDateTimeInstance();

        rval.append("<table>\n<tr> <th><a href=\"?sort=" +
		maybeReverse("-bypath", sortby)+"\">File</a></th> <th><a href=\"?sort=" +
		maybeReverse("bysize", sortby)+"\">size</a></th> <th><a href=\"?sort=" +
		maybeReverse("bytime", sortby) + "\">time</a></th> </tr>\n");


	Collection offerings = this.offerings.values();
	if ("bytime".equals(sortby)) {
	    ArrayList x = new ArrayList(offerings);
	    Collections.sort(x, new OfferingTimeComparator());
	    offerings = x;
	} else if ("-bytime".equals(sortby)) {
	    ArrayList x = new ArrayList(offerings);
	    Collections.sort(x, new ReverseOfferingTimeComparator());
	    offerings = x;
	} else if ("bypath".equals(sortby)) {
	    ArrayList x = new ArrayList(offerings);
	    Collections.sort(x);
	    offerings = x;
	} else if ("-bypath".equals(sortby)) {
	    ArrayList x = new ArrayList(offerings);
	    Collections.sort(x);
	    Collections.reverse(x);
	    offerings = x;
	} else if ("bysize".equals(sortby)) {
	    ArrayList x = new ArrayList(offerings);
	    Collections.sort(x, new OfferingSizeComparator());
	    offerings = x;
	} else if ("-bysize".equals(sortby)) {
	    ArrayList x = new ArrayList(offerings);
	    Collections.sort(x, new OfferingSizeComparator());
	    Collections.reverse(x);
	    offerings = x;
	}

	for (Iterator iter = offerings.iterator(); iter.hasNext();) {
            Offering of = (Offering) iter.next();
            rval.append("<tr><td>");
	    String htmlForTag = Main.htmlEncode(of.getTag());
	    String hrefForTag = Main.URLEncode(of.getTag());
	    rval.append("<a href=\""+hrefForTag+"\">"+htmlForTag+"</a>");
            rval.append("</td>\n");

            long length = of.resource.length();
            rval.append("<td align=right>"+length+"</td>");

	    long mtime = of.resource.lastModified();
	    rval.append("<td align=right>");

	    if (0 < mtime) {
		rval.append(substitute(df.format(new Date(mtime)), ' ', "&nbsp;"));
	    }
	    rval.append("</td>\n");

	    rval.append("</tr>\n");
        }
        rval.append("</table>\n" );

        return rval;
    }

    public static String maybeReverse(String targetKey, String current)
    {
	if (targetKey.equals(current)) {
	    if ('-' == targetKey.charAt(0))
		return targetKey.substring(1);
	    else
		return '-'+targetKey;
	} else {
	    return targetKey;
	}
    }

    private static String substitute(CharSequence str, char orig, String replacement)
    {
	StringBuffer rval = new StringBuffer();
	for (int i=0; i<str.length(); i++) {
	    char ch = str.charAt(i);
	    if (ch == orig)
		rval.append(replacement);
	    else
		rval.append(ch);
	}
	return rval.toString();
    }

    public static class ReverseOfferingTimeComparator
    extends OfferingTimeComparator
    {
	public int compare(Object o1, Object o2)
	{
	    return - super.compare(o1, o2);
	}
    }

    public static class OfferingTimeComparator
	    implements Comparator
    {
	public int compare(Object o1, Object o2)
	{
	    Offering a = (Offering) o1;
	    Offering b = (Offering) o2;
	    long mtime1 = a.resource.lastModified();
	    long mtime2 = b.resource.lastModified();
	    if (mtime1<mtime2)
		return 1;
	    else if (mtime1>mtime2)
		return -1;

	    return a.resource.compareTo(b.resource);
	}
    }

    public static class OfferingSizeComparator
	    implements Comparator
    {
	public int compare(Object o1, Object o2)
	{
	    Offering a = (Offering) o1;
	    Offering b = (Offering) o2;
	    long mtime1 = a.resource.length();
	    long mtime2 = b.resource.length();
	    if (mtime1<mtime2)
		return 1;
	    else if (mtime1>mtime2)
		return -1;

	    return a.resource.compareTo(b.resource);
	}
    }

}
