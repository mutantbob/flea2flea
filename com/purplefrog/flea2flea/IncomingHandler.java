/*
 * Copyright 2002
 * Robert Forsman <thoth@purplefrog.com>
 */
package com.purplefrog.flea2flea;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

import org.mortbay.util.*;

public class IncomingHandler
    extends HttpServlet
{
    public static Logger logger=null;
    public static DonationList donations=null;
    public static final String URI = "/upload";

    public static final String BACK_LINK = "\n<hr>\n<a href=\"/\">Flea2Flea public index</a>\n";

    public IncomingHandler()
    {
    }

    protected void doPost(HttpServletRequest req_, HttpServletResponse resp)
        throws ServletException, IOException
    {
	MultiPartIterator iter = new MultiPartIterator(req_);

	String tag = null, remoteName=null;

	MultiPartIterator.Part2 part;
	while (null != (part = iter.getNextPart())) {
	    if ("tag".equals(part.getContentName())) {
		tag = new String(part.slurp());
	    } else if ("file".equals(part.getContentName())) {
		remoteName = part.getFilename();
		if (null == tag) {
		    logAndReject(req_.getRemoteAddr(), "*missing*", remoteName, resp, "browser insanity: file before tag in multipart");
		    return;
		}

		break;
	    } else {
		part.drain();
	    }
	}

	if (null == part) {
	    logAndReject(req_.getRemoteAddr(), tag, remoteName, resp, "no file was offered");
	    return;
	}

	Donation don;
	synchronized (donations) {
	    don = donations.lookup(tag);

	    if (null != don)
		donations.deactivate(don);
	}

	if (null == don) {
	    logAndReject(req_.getRemoteAddr(), tag, remoteName, resp, "no matching tag");

	    return;
	}

	if (don.destination.isDirectory()) {
	    donations.activate(don, true);
	    long fullness = bytesInDirectory(don.destination);
	    if (0 >= don.maxSize - fullness) {
		logAndReject(req_.getRemoteAddr(), tag, remoteName, resp, "directory full");
		return;
	    }

	    String x = basename(remoteName);
	    if (1 > x.length()) {
		logAndReject(req_.getRemoteAddr(), tag, remoteName, resp, "bad (short) name provided by peer");
		return;
	    }

	    File f = new File(don.destination, x);
	    if (f.exists()) {
		logAndReject(req_.getRemoteAddr(), tag, remoteName, resp, "file already exists locally");
		return;
	    }
	    receiveToFile(req_, tag, remoteName, part, f, don.maxSize - fullness, resp);

	} else {
	    receiveToFile(req_, tag, remoteName, part, don, resp);
	}
    }

    public static String basename(String remoteName)
    {
	return chopAfter(chopAfter(remoteName, '/'), '\\');
    }

    public static String chopAfter(String str, char separator)
    {
	int idx = str.lastIndexOf(separator);
	return 0 > idx ? str : str.substring(idx + 1);
    }

    public static long bytesInDirectory(File dir)
    {
	File[] files = dir.listFiles();
	if (null == files) {
	    new IOException("called bytesInDirectory() on "+dir+" but listFiles() returned null");
	    return 0;
	}
	long rval = 0;
	for (int i = 0; i < files.length; i++) {
	    File f = files[i];
	    if (f.isDirectory())
		rval += bytesInDirectory(f);
	    else
		rval += f.length();
	}
	return rval;
    }

    private static void logAndReject(String remoteAddr, String tag, String remoteName, HttpServletResponse resp, String reason)
	    throws IOException
    {
	logger.logUploadReject(remoteAddr, tag, remoteName, reason);

	resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
	resp.setContentType("text/html");

	PrintWriter pw = resp.getWriter();
	pw.println("<html><head><title>upload declined</title></head>");
	pw.println("<body>");
	pw.println("Upload of "+remoteName+" to slot "+tag+" <b>declined</b>.");
	pw.println(BACK_LINK);
	pw.println("</body></html>");
	pw.close();
    }

    private static void receiveToFile(HttpServletRequest req_, String tag, String remoteName, MultiPartIterator.Part2 req, Donation don, HttpServletResponse resp)
	    throws IOException
    {
	receiveToFile(req_, tag, remoteName, req, don.destination, don.maxSize, resp);
    }

    private static void receiveToFile(HttpServletRequest req_, String tag, String remoteName, MultiPartIterator.Part2 req, File destination, long maxSize, HttpServletResponse resp)
	    throws IOException
    {
	logger.logUploadStart(req_.getRemoteAddr(), tag, remoteName);
	boolean shortWrite;
	try {
	    InputStream istr = req.getInputStream();
	    FileOutputStream ostr = new FileOutputStream(destination);

	    try {
		IO.bufferSize = 64<<10;
		IO.copy(istr, ostr, maxSize);
		shortWrite = 0 <= istr.read();
	    } finally {
		ostr.close();
	    }
	} catch (IOException e) {
	    logger.logUploadFail(req_.getRemoteAddr(), tag, remoteName);
	    logger.logException(e);

	    resp.setStatus(500);
	    resp.setContentType("text/html");

	    PrintWriter pw = resp.getWriter();
	    pw.println("<html><head><title>upload malfunction</title></head>");
	    pw.println("<body>");
	    pw.println("<b>Malfunction</b> while copying from "+remoteName+" to slot "+tag);
	    pw.println(BACK_LINK);
	    pw.println("</body></html>");
	    pw.close();
	    throw e;
	}

	logger.logUploadFinish(req_.getRemoteAddr(), tag, remoteName, destination.length());

	resp.setStatus(200);
	resp.setContentType("text/html");

	PrintWriter pw = resp.getWriter();
	pw.println("<html><head><title>upload succeeded</title></head>");
	pw.println("<body>");
	long l = destination.length();
	pw.println("<b>Uploaded</b> "+l+" bytes from "+remoteName+" to slot "+tag+".\n");
	if (shortWrite)
	    pw.println("<b>This is not the entirety of the file you offered<blink>!</blink></b>.\n");
	pw.println(BACK_LINK);
	pw.println("</body></html>");
	pw.close();
    }

}
