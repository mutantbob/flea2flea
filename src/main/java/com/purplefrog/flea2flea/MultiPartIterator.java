/*
 * Copyright 2005
 * Robert Forsman <thoth@purplefrog.com>
 *

 * Derived from the MultiPartRequest included with Jetty which was
 * version $Id: MultiPartIterator.java,v 1.1 2005/02/02 23:33:17 hammor Exp $
 * author  Greg Wilkins
 * author  Jim Crossley
 */
package com.purplefrog.flea2flea;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import javax.servlet.http.*;

import org.mortbay.http.*;
import org.mortbay.util.*;
import org.mortbay.servlet.*;
import org.apache.commons.logging.*;

public class MultiPartIterator
{
    private static Log log = LogFactory.getLog(MultiPartRequest.class);

    HttpServletRequest _request;
    String _boundary;
    LineInput _in;
    byte[] _byteBoundary;

    boolean _lastPart=false;
    MultiMap _partMap = new MultiMap(10);
    int _char=-2;

    Part2 partCache = null;
    private static final String CORRECT_CONTENT_TYPE = "multipart/form-data";

    /**
     * @deprecated for debugging only
     */
    public MultiPartIterator(InputStream istr, String boundary)
	    throws IOException
    {
	setInputStream(istr, boundary);
    }

    public MultiPartIterator(HttpServletRequest request)
	    throws IOException
    {
	_request=request;

	if (!correctContentType(request))
	    throw new IOException("Not " + CORRECT_CONTENT_TYPE + " request");
	String content_type = request.getHeader(HttpFields.__ContentType);
	if(log.isDebugEnabled())log.debug("Multipart content type = "+content_type);

	setInputStream(request.getInputStream(),
		value(content_type.substring(content_type.indexOf("boundary="))));

    }

    public static boolean correctContentType(HttpServletRequest request)
    {
	String content_type = request.getHeader(HttpFields.__ContentType);
	boolean b = content_type.startsWith(CORRECT_CONTENT_TYPE);
	return b;
    }

    private void setInputStream(InputStream istr, String boundary)
	    throws IOException
    {
	_in = new LineInput(istr);

	// Extract boundary string
	_boundary="--" + boundary;

	if(log.isDebugEnabled())log.debug("Boundary="+_boundary);
	_byteBoundary= (_boundary+"--").getBytes(StringUtil.__ISO_8859_1);

	// Get first boundary
	String line = _in.readLine();
	if (!line.equals(_boundary))
	{
	    log.warn(line);
	    throw new IOException("Missing initial multi part boundary");
	}
//	loadAllParts();
    }

    public Part2 getNextPart()
	    throws IOException
    {
	if (null != partCache && !partCache.isEOF())
	    throw new IOException("failed to drain the last part you fetched.");

	if (_lastPart)
	    return null;

	return partCache = new Part2(_in);
    }


    private boolean parseContentDisposition(String content_disposition, Part part)
	    throws IOException
    {
	// Extract content-disposition
	boolean form_data=false;
	if (content_disposition==null)
	{
	    throw new IOException("Missing content-disposition");
	}

	StringTokenizer tok =
	    new StringTokenizer(content_disposition,";");
	while (tok.hasMoreTokens())
	{
	    String t = tok.nextToken().trim();
	    String tl = t.toLowerCase();
	    if (t.startsWith("form-data"))
		form_data=true;
	    else if (tl.startsWith("name="))
		part._name=value(t);
	    else if (tl.startsWith("filename="))
		part._filename=value(t);
	}
	return form_data;
    }

    /* ------------------------------------------------------------ */
    private String value(String nameEqualsValue)
    {
        String value =
            nameEqualsValue.substring(nameEqualsValue.indexOf('=')+1).trim();

        int i=value.indexOf(';');
        if (i>0)
            value=value.substring(0,i);
        if (value.startsWith("\""))
        {
            value=value.substring(1,value.indexOf('"',1));
        }

        else
        {
            i=value.indexOf(' ');
            if (i>0)
                value=value.substring(0,i);
        }
        return value;
    }

    private static class Part
    {
        String _name=null;
        String _filename=null;
        Hashtable _headers= new Hashtable(10);
        byte[] _data=null;
    }

    public static final Pattern colon = Pattern.compile("\\s*:\\s*");

    public static class Header
	    implements Comparable
    {
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

    public class Part2
    {
	protected final Map headers = new TreeMap();

	private final BoundaryHunter istr;

	public Part2(LineInput in)
		throws IOException
	{
	    String line;
	    boolean first = true;
	    while (null != (line = in.readLine())) {
		if (first) {
		    first = false;
		    if (0 == line.length())
			continue;
		}

		if (0 == line.length())
		    break;

		Header hdr = new Header(line);
		if (null != headers.put(hdr.lowerKey(), hdr)) {
		    throw new IOException("duplicate header : "+hdr.lowerKey());
		}
	    }

	    istr = new BoundaryHunter();
	}

	public boolean isEOF()
	{
	    return istr.isEOF();
	}


	public void drain()
		throws IOException
	{
	    istr.drain();
	}

	public InputStream getInputStream()
	{
	    return istr;
	}

	public Iterator headerKeyIterator()
	{
	    return headers.keySet().iterator();
	}

	public String getContentName()
	{

	    return grepSubHeader((Header) headers.get("content-disposition"), "name");
	}

	public String getFilename()
	{
	    return grepSubHeader((Header) headers.get("content-disposition"), "filename");
	}

	private String grepSubHeader(Header hdr, String subKey)
	{
	    if (hdr== null)
		return null;

	    StringTokenizer tok = new StringTokenizer(hdr.value,";");
	    while (tok.hasMoreTokens()) {
		String t = tok.nextToken().trim();
		String tl = t.toLowerCase();
		if (tl.startsWith(subKey + '='))
		    return value(t);
	    }
	    return null;
	}

	/**
	 * returns the contents of this part as a byte[].   If you use this on a REALLY big part, you WILL exceed the heap and get an OutOfMemoryError.
	 * @return a byte[] containing the TOTALITY of the remaining bytes in the InputStream of this part.
	 * @throws IOException
	 */
	public byte[] slurp()
		throws IOException
	{
	    ByteArrayOutputStream ostr = new ByteArrayOutputStream();
	    byte[] buf = new byte[64<<10];
	    int n;
	    while (0< (n=istr.read(buf))) {
		ostr.write(buf, 0, n);
	    }
	    return ostr.toByteArray();
	}

    }
    private  class BoundaryHunter
	    extends InputStream
    {
	boolean boundaryPossible=true;
	boolean cr=false,lf=false;
	byte[] cache = new byte[_byteBoundary.length+2];
	int cacheQty=0;

	boolean eof=false;

	public int read()
		throws IOException
	{
	    byte[] x = new byte[1];
	    if (0>read(x))
		return -1;
	    else
		return x[0];
	}

	public boolean isEOF()
	{
	    return eof;
	}

	/** sweet monkey jesus, what a kludge.  If you even THINK about hacking this, make sure you look at the unit tests. */
	public int read(byte[] b, int off, int len)
		throws IOException
	{
	    if (eof)
		return -1;

	    int i;
	    if (boundaryPossible) {

		for (i=0; i<_byteBoundary.length; i++) {
		    int ch = _in.read();
		    if (0 > ch) {
			_lastPart = true;
			if (1 > i) {
			    eof=true;
			    return -1;
			} else {
			    cacheQty = i;
			    boundaryPossible = false;
			    return fillFromCache(b, off, len);
			}
		    }
		    cache[cacheQty+i] = (byte) ch;
		    if (cache[cacheQty+i] != _byteBoundary[i]) {
			if (0 != i)
			    break;
			else {
			    if (0 == cacheQty) {
				if (crORlf(ch)) {
				    cacheQty++;
				    i--;
				} else {
				    break;
				}
			    } else if (1 == cacheQty) {
				if ('\r' == cache[0]) {
				    if ('\n' == ch) {
					cacheQty++;
					i--;
				    } else {
					break;
				    }
				} else {
				    cacheQty++;
				    return fillFromCache(b, off, 1);
				}
			    } else {
				break;
			    }
			}
		    }
		}
		if (i<_byteBoundary.length-2) {
		    cacheQty += i+1;
		    boundaryPossible = crORlf(cache[cacheQty-1]);
		    return fillFromCache(b, off, len);
		} else {
		    eof=true; // uhoh, boundary!
		    _lastPart = i>=_byteBoundary.length; // wow, final -- and everything
		    boundaryPossible = false;
		    return -1;
		}
	    } else {
		if (0 != cacheQty) {
		    return fillFromCache(b, off, len);
//		    throw new IllegalStateException();
		}

		for (i=0; i<len && !boundaryPossible; i++) {
		    int ch = _in.read();
		    if (0 > ch) {
			_lastPart = true;
			break;
		    }
		    if (crORlf(ch)) {
			boundaryPossible = true;
			cache[0]=(byte) ch;
			cacheQty=1;
			if (0 == i)
			    return read(b, off, len);
			else
			    break;
		    }
		    b[i+off] = (byte) ch;
		}
		return i;
	    }
	}

	private boolean crORlf(int b)
	{
	    return '\r' == b || '\n' == b;
	}

	private int fillFromCache(byte[] dest, int off, int len)
	{
	    int rval = Math.min(cacheQty, len);
	    if (rval<1)
		throw new IllegalStateException("cache empty?");
	    System.arraycopy(cache, 0, dest, off, rval);
	    if (rval<cacheQty) {
		System.arraycopy(cache, rval, cache,0, cacheQty-rval);
		cacheQty-= rval;
	    } else {
		cacheQty = 0;
	    }
	    return rval;
	}

	public void drain()
		throws IOException
	{
	    byte[] buf = new byte[64<<10];
	    while (0<read(buf)) {

	    }
	}
    }
}
