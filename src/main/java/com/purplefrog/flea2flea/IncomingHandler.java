/*
 * Copyright 2002
 * Robert Forsman <thoth@purplefrog.com>
 */
package com.purplefrog.flea2flea;

import java.io.*;

import com.purplefrog.apachehttpcliches.*;
import com.purplefrog.rfc2046.*;
import org.apache.http.*;
import org.apache.http.protocol.*;

public class IncomingHandler
    implements HttpRequestHandler
{
    public static Logger logger=null;
    public static DonationList donations=null;
    public static final String URI = "/upload";

    public static final String BACK_LINK = "\n<hr>\n<a href=\"/\">Flea2Flea public index</a>\n";

    public IncomingHandler()
    {
    }

    public void handle(HttpRequest req_, HttpResponse httpResponse, HttpContext httpContext)
    {

        String remoteAddr = ApacheHTTPCliches.remoteAddress(httpContext).getRemoteAddress().getHostAddress();

        EntityAndHeaders rval;
        try {
            MultiPartIterator iter = new MultiPartIterator(req_);

            rval = computeResponse(remoteAddr, iter);
        } catch (IOException e) {
            e.printStackTrace();
            rval = EntityAndHeaders.plainPayload(500, "I am full of explosions\n"+e.getMessage(), "text/plain");
        }

        rval.apply(httpResponse);
    }

    protected EntityAndHeaders computeResponse(String remoteAddr, MultiPartIterator iter)
        throws IOException
    {
        String tag = null, remoteName = null;
	MultiPartIterator.Part2 part;
	while (null != (part = iter.getNextPart())) {
	    if ("tag".equals(part.getContentName())) {
		tag = new String(part.slurp());
	    } else if ("file".equals(part.getContentName())) {
		remoteName = part.getFilename();
		if (null == tag) {
                    return logAndReject(remoteAddr, "*missing*", remoteName, "browser insanity: file before tag in multipart");
		}

		break;
	    } else {
		part.drain();
	    }
	}

	if (null == part) {
	    return logAndReject(remoteAddr, tag, remoteName, "no file was offered");
	}

	Donation don;
	synchronized (donations) {
	    don = donations.lookup(tag);

	    if (null != don)
		donations.deactivate(don);
	}

	if (null == don) {
	    return logAndReject(remoteAddr, tag, remoteName, "no matching tag");

	}

	if (don.destination.isDirectory()) {
	    donations.activate(don, true);
	    long fullness = bytesInDirectory(don.destination);
	    if (0 >= don.maxSize - fullness) {
		return logAndReject(remoteAddr, tag, remoteName, "directory full");

	    }

	    String x = basename(remoteName);
	    if (1 > x.length()) {
		return logAndReject(remoteAddr, tag, remoteName, "bad (short) name provided by peer");

	    }

	    File f = new File(don.destination, x);
	    if (f.exists()) {
		return logAndReject(remoteAddr, tag, remoteName, "file already exists locally");

	    }
	    return receiveToFile(tag, remoteName, part, f, don.maxSize - fullness, remoteAddr);

	} else {
	    return receiveToFile(tag, remoteName, part, don, remoteAddr);
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
	    new IOException("called bytesInDirectory() on "+dir+" but listFiles() returned null").printStackTrace();
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

    private static EntityAndHeaders logAndReject(String remoteAddr, String tag, String remoteName, String reason)
	    throws IOException
    {
	logger.logUploadReject(remoteAddr, tag, remoteName, reason);

        StringBuilder pw = new StringBuilder();
	pw.append("<html><head><title>upload declined</title></head>");
	pw.append("<body>");
	pw.append("Upload of "+remoteName+" to slot "+tag+" <b>declined</b>.");
	pw.append(BACK_LINK);
	pw.append("</body></html>");

        return EntityAndHeaders.plainPayload(403, pw.toString(), "text/html");
    }

    private static EntityAndHeaders receiveToFile(String tag, String remoteName, MultiPartIterator.Part2 req, Donation don, String remoteAddr)
	    throws IOException
    {
        return receiveToFile(tag, remoteName, req, don.destination, don.maxSize, remoteAddr);
    }

    public static void copy(InputStream istr, FileOutputStream ostr, long nBytes)
        throws IOException
    {
        byte[] buffer = new byte[(int) Math.min(64<<10,nBytes)];
        while (nBytes >0) {
            int n = istr.read(buffer, 0, (int) Math.min(nBytes, buffer.length));
            if (n<0)
                break;

            ostr.write(buffer, 0, n);
        }

    }

    private static EntityAndHeaders receiveToFile(String tag, String remoteName, MultiPartIterator.Part2 req, File destination, long maxSize, String remoteAddr)
	    throws IOException
    {
	logger.logUploadStart(remoteAddr, tag, remoteName);
	boolean shortWrite;
	try {
	    InputStream istr = req.getInputStream();
	    FileOutputStream ostr = new FileOutputStream(destination);

	    try {
		copy(istr, ostr, maxSize);
		shortWrite = 0 <= istr.read();
	    } finally {
		ostr.close();
	    }
	} catch (IOException e) {
	    logger.logUploadFail(remoteAddr, tag, remoteName);
	    logger.logException(e);

            StringBuilder pw = new StringBuilder();
	    pw.append("<html><head><title>upload malfunction</title></head>");
	    pw.append("<body>");
	    pw.append("<b>Malfunction</b> while copying from "+remoteName+" to slot "+tag);
	    pw.append(BACK_LINK);
	    pw.append("</body></html>");

	    return EntityAndHeaders.plainPayload(200, pw.toString(), "text/html");

	}

	logger.logUploadFinish(remoteAddr, tag, remoteName, destination.length());

	StringBuilder pw = new StringBuilder();
	pw.append("<html><head><title>upload succeeded</title></head>");
	pw.append("<body>");
	long l = destination.length();
	pw.append("<b>Uploaded</b> "+l+" bytes from "+remoteName+" to slot "+tag+".\n");
	if (shortWrite)
	    pw.append("<b>This is not the entirety of the file you offered<blink>!</blink></b>.\n");
	pw.append(BACK_LINK);
	pw.append("</body></html>");

        return EntityAndHeaders.plainPayload(200, pw.toString(), "text/html");
    }

}
