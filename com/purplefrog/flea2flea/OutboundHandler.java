/*
 * Copyright 2002
 * Robert Forsman <thoth@purplefrog.com>
 */
package com.purplefrog.flea2flea;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.mortbay.http.*;
import org.mortbay.http.handler.*;


public class OutboundHandler
    extends AbstractHttpHandler
{
    protected Map offerings;

    protected Logger logger;
    public static final Pattern byteRangePattern = Pattern.compile("bytes=(\\d+)?-(\\d+)?");


    public OutboundHandler(Map offerings, Logger logger)
    {
        this.logger = logger;
        this.offerings = offerings;
    }

    public void handle(String pathInContext,
                       String pathParams,
                       HttpRequest request,
                       HttpResponse response) throws IOException
    {
	Offering of = (Offering) offerings.get(pathInContext);

	if (null == of)
	    return;

	String remoteAddr = request.getRemoteAddr();
	String method = request.getMethod();

	boolean normalCompletion = false;
	try {

	    if ( ! (isGet(request) || isHead(request)) ) {
		response.setStatus(
			HttpResponse.__405_Method_Not_Allowed);
		response.setAttribute("Allow", "GET, HEAD");
		response.setContentType("text/plain");
		sendText(response, "This URL only supports GET and HEAD requests.\n");
		return;
	    }

	    logger.logOutbound(method, remoteAddr, pathInContext, of.resource);

	    RandomAccessFile raf=null;
	    long length;
	    try {
		try {
		    raf = new RandomAccessFile(of.resource, "r");
		    length = of.resource.length();
		} catch (IOException e) {
		    logger.logOutboundFailure(method, remoteAddr, pathInContext, of.resource);
		    logger.logException(e);
		    response.setStatus(500);
		    response.setContentType("text/plain");
		    sendText(response, "problem getting the file you asked for");
		    throw e;
		}

		Enumeration ranges = request.getFieldValues(HttpFields.__Range);
		if (null != ranges && ranges.hasMoreElements()) {
		    String range = (String) ranges.nextElement();
		    if (!ranges.hasMoreElements()) {
			Matcher m = byteRangePattern.matcher(range);
			if (m.matches()) {
			    long begin = null !=  m.group(1) ? Long.parseLong(m.group(1)) : 0;

                            if (begin >= length) {
                                setHeadersForContentRange(length-1, 0, response, length);
                                response.setStatus(416);
                                response.setReason("Requested Range Not Satisfiable");
                                response.setContentType("text/plain");
                                sendText(response, "You asked for a range starting at "+begin+" but the file is only "+length+" bytes long.\n");
                                normalCompletion = false;
                                return;
                            }

                            long end = null != m.group(2) ? Long.parseLong(m.group(2)) : length - 1;
                            if (end >= length) {
                                end = length-1;
                            }

                            int bytesRequested = setHeadersForContentRange(end, begin, response, length);

			    if (isGet(request)) {
				sendPayloadForContentRange(raf, begin, response, bytesRequested);
                                response.getOutputStream().close();
                            }

                            logger.logOutboundComplete(method, remoteAddr, pathInContext, of.resource, length, begin,end);
			    normalCompletion = true;
			    return;
			}
		    }
		}

		if (2L << 30 < length) {
		    int bytesRequested = setHeadersForContentRange(length-1, 0, response, length);
		    if (isGet(request))
			sendPayloadForContentRange(raf, 0, response, bytesRequested);
		    logger.logOutboundComplete(method, remoteAddr, pathInContext, of.resource, length, 0,bytesRequested-1);
		    normalCompletion = true;
		    return;
		}

		sendCompleteFile(response, raf, (int)length, isGet(request));
		logger.logOutboundComplete(method, remoteAddr, pathInContext, of.resource, (int)length);
		normalCompletion = true;
	    } finally {
		if (null    != raf) raf.close();
	    }
	} finally {
	    if (!normalCompletion)
		logger.logOutboundFailure(method, remoteAddr, pathInContext, of.resource);
	}
    }

    private static boolean isHead(HttpRequest request)
    {
	return "HEAD".equalsIgnoreCase(request.getMethod());
    }

    private static boolean isGet(HttpRequest request)
    {
	return "GET".equalsIgnoreCase(request.getMethod());
    }

    private static void sendPayloadForContentRange(RandomAccessFile raf, long begin, HttpResponse response, int bytesRequested)
	    throws IOException
    {
	raf.seek(begin);

	OutputStream ostr = response.getOutputStream();

	long sent = 0;
	int n;
	byte[] buffer = new byte[64<<10];
	while (sent < bytesRequested) {
	    n=raf.read(buffer, 0, (int)Math.min(buffer.length, bytesRequested - sent));
	    ostr.write(buffer, 0, n);
	    if (1 > n)
		break;
	    sent += n;
	}
    }

    private static int setHeadersForContentRange(long end, long begin, HttpResponse response, long fileLength)
    {
	long bytesRequested_ = end-begin+1;
	int bytesRequested = (int) bytesRequested_;
	if (bytesRequested != bytesRequested_) {
//	    kludge4G(response);
//	    throw new IOException("stream too large : "+bytesRequested_);
	    bytesRequested = -1 + (2<<30);
	    end = begin+bytesRequested - 1;
	}
	response.setStatus(206);
	response.setContentType("application/octet-stream");
	response.setContentLength(bytesRequested);
	response.setField("Content-Range", "bytes "+begin+'-'+end+'/'+fileLength);
        response.setField("Accept-Ranges", "bytes");
	return bytesRequested;
    }

    private static long sendCompleteFile(HttpResponse response, RandomAccessFile raf, int fileLength, boolean isGet)
	    throws IOException
    {
	response.setStatus(200);
	response.setContentType("application/octet-stream");
	response.setContentLength(fileLength);
        response.setField("Accept-Ranges", "bytes");

        OutputStream ostr = response.getOutputStream();
	long totalWritten = 0;

	if (isGet) {
	    byte[] buffer = new byte[64<<10];
	    int n;
	    while ( 0< (n=raf.read(buffer))) {
		ostr.write(buffer, 0, n);
		totalWritten += n;
	    }
	}
	ostr.close();
	return totalWritten;
    }

    private static void kludge4G(HttpResponse response) throws IOException
    {
        response.setStatus(500);
        response.setContentType("text/plain");
        sendText(response, "massive files are not yet supported");
    }

    private static void sendText(HttpResponse response, String msg)
        throws IOException
    {
        byte[] bytes = msg.getBytes();
        response.setContentLength(bytes.length);
        
        final OutputStream ostr = response.getOutputStream();
        ostr.write(bytes);
        ostr.close();
    }
}
