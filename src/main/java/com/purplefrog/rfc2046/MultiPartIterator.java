/*
 * Copyright 2005-2013
 * Robert Forsman <thoth@purplefrog.com>
 *

 * Derived from the MultiPartRequest included with Jetty which was
 * version $Id: MultiPartIterator.java,v 1.1 2005/02/02 23:33:17 hammor Exp $
 * author  Greg Wilkins
 * author  Jim Crossley
 */
package com.purplefrog.rfc2046;

import java.io.*;
import java.util.*;

import com.purplefrog.apachehttpcliches.*;
import org.apache.http.*;

/**
 * A rather lightly-tested implementation of RFC2046
 *
 */
public class MultiPartIterator
{
    String _boundary;

    /**
     * includes the leading "\r\n" ; and a trailing "--" (which is only relevant for final boundary)
     */
    byte[] _byteBoundary;

    boolean _lastPart=false;

    Part2 partCache = null;
    private static final String CORRECT_CONTENT_TYPE = "multipart/form-data";
    private PushbackInputStream istr;
    private boolean corrupted=false;

    /**
     * @deprecated for debugging only
     */
    public MultiPartIterator(InputStream istr, String boundary)
	    throws IOException
    {
	setInputStream(istr, boundary);
    }


    public MultiPartIterator(HttpRequest req_)
        throws IOException
    {
        org.apache.http.Header contentType_ = req_.getFirstHeader("Content-Type");
        boolean cct =  null != contentType_ && contentType_.getValue().startsWith(CORRECT_CONTENT_TYPE);

        if (!cct) {
            throw new IOException("Not "+CORRECT_CONTENT_TYPE+" request");
        }

        String contentType = contentType_.getValue();
        String boundary = contentType.substring(contentType.indexOf("boundary="));

        setInputStream(ApacheHTTPCliches.requestBodyAsInputStream(req_), value(boundary));
    }


    private void setInputStream(InputStream istr, String boundary)
	    throws IOException
    {
	this.istr = new PushbackInputStream(istr);

	// Extract boundary string
	_boundary="--" + boundary;


        _byteBoundary= ("\r\n"+_boundary+"--").getBytes();

        // Get first boundary
        String line = readLine(istr);
        if (! line.equals(_boundary)) {
            throw new IOException("Missing initial multi part boundary");
        }

    }

    public static String readLine(InputStream istr)
        throws IOException
    {
        byte[] terminator="\r\n".getBytes();

        StringBuilder rval = new StringBuilder();
        int tcursor=0;
        while(true) {
            int ch= istr.read();

            if (ch<0)
                break;

            rval.append((char) ch);

            byte ch_ = (byte) ch;
            if (ch_==terminator[tcursor]) {
                tcursor++;
                if (tcursor >= terminator.length)
                    break;
            } else {
                // this only works because the terminator doesn't contain any duplicate characters
                tcursor = 0;
            }

        }

        int l2 = rval.length() - ( tcursor==terminator.length ? tcursor :0);
        return rval.substring(0, l2);
    }

    public Part2 getNextPart()
	    throws IOException
    {
	if (null != partCache && !partCache.isEOF())
	    throw new IOException("failed to drain the last part you fetched.");

        if (corrupted)
            throw new IOException("multipart stream was malformed");

	if (_lastPart)
	    return null;

	return partCache = new Part2(istr);
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

    public class Part2
    {
	protected final Map headers = new TreeMap();

	private final BoundaryHunter istr;

	public Part2(InputStream in)
		throws IOException
	{
	    String line;
	    while (null != (line = readLine(in))) {

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

    private class BoundaryHunter
	    extends InputStream
    {
	boolean boundaryPossible=true;
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

            int nRead =0;
            while (nRead<len) {
                int n = read_(b, off+nRead, len-nRead);
//                debugPrint("read_ returns "+n);
                if (n<0) {
                    if (nRead==0)
                        return -1;
                    break;
                }
                nRead+=n;
            }
            return nRead;
        }

        private int read_(byte[] b, int off, int len)
            throws IOException
        {
            if (boundaryPossible) {
                return readWithPossibleBoundary(b, off, len);

            } else {
                if (0 != cacheQty) {
                    return fillFromCache(b, off, len);
//		    throw new IllegalStateException();
                }

                int i;
                for (i=0; i<len && !boundaryPossible; i++) {
                    int ch = istr.read();
//                    debugPrint(ch + " = '" + ((char) ch) + "'");
                    if (0 > ch) {
                        _lastPart = true;
                        break;
                    }
                    if (ch == '\r') {
                        boundaryPossible = true;
                        istr.unread(ch);
                        if (0 == i)
                            return readWithPossibleBoundary(b, off, len);
                        else
                            break;
                    }
                    b[i+off] = (byte) ch;
                }
                return i;
            }
        }

        private int readWithPossibleBoundary(byte[] b, int off, int len)
            throws IOException
        {
            int i;
            for (i=0; i<_byteBoundary.length; i++) {
                int ch = istr.read();

//                debugPrint(ch+" = '"+ ((char) ch) + "' i=" + i+" cacheQty="+cacheQty);
                if (0 > ch) {
// EOF
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
                    if (0 != i) {
//                        istr.unread(ch);
                        break;
                    } else {
                        if (0 == cacheQty) {
                            if ('\r'==ch) {
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
                                boundaryPossible=false;
                                return fillFromCache(b, off, len);
                            }
                        } else {
                            break;
                        }
                    }
                }
            }
            if (i<_byteBoundary.length-2) {
                cacheQty += i+1;
                boundaryPossible = '\r' == (cache[cacheQty-1]);
                return fillFromCache(b, off, len);
            } else {
                eof=true; // uhoh, boundary!
                _lastPart = i>=_byteBoundary.length; // wow, final "--" and everything
                if (!_lastPart) {
                    int pos2 = _byteBoundary.length - 2;
                    for(; i>=pos2; i--) {
                        if (cacheQty +i<cache.length) {
//                            debugPrint("unread("+cache[cacheQty+i]+")");
                            istr.unread(cache[cacheQty + i]);
                        }
                    }
                }
                boundaryPossible = false;

                devourTrailingWhitespace();
                return -1;
            }
        }

        private void devourTrailingWhitespace()
            throws IOException
        {
            byte[] crlf = { 13, 10 };
            int ch;
            int crlfIdx=0;
            while(true) {
                ch = istr.read();

                if (ch<0)
                    return;

                if (ch == crlf[crlfIdx]) {
                    crlfIdx ++;
                    if (crlfIdx>= crlf.length)
                        return;
                } else {
                    crlfIdx=0;

                    if (!Character.isWhitespace(ch)){
                        corrupted = true;
                        throw new IOException("non-whitespace after boundary sequence");
                    }
                }
            }
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
