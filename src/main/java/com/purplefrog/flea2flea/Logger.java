/*
 * Copyright 2002
 * Robert Forsman <thoth@purplefrog.com>
 */
package com.purplefrog.flea2flea;

import java.awt.*;
import java.io.*;
import javax.swing.*;

public class Logger
        extends JPanel
{
    private JTextArea area;

    public Logger()
    {
        super (new BorderLayout());
        add(new JScrollPane(area = new JTextArea()));
	area.setEditable(false);
    }

    public void logException(Exception e)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.println();
        pw.close();
        area.append(sw.toString()+'\n');
    }

    public void logOutboundComplete(String method, String remoteAddr, String pathInContext, File resource, int length)
    {
        area.append(method + " END ["+  remoteAddr+"] "+pathInContext+" = "+resource+"  *"+length+'\n');
    }

    public void logOutboundFailure(String method, String remoteAddr, String pathInContext, File resource)
    {
        area.append(method + " ABORT ["+remoteAddr+"] "+pathInContext+" = "+resource+'\n');
    }

    public void logOutbound(String method, String remoteAddr, String pathInContext, File resource)
    {
        area.append(method + " BEGIN ["+remoteAddr+"] "+pathInContext+" = "+resource+'\n');
    }

    public void logOutboundComplete(String method, String remoteAddr, String pathInContext, File resource, long length, long begin, long end)
    {
	area.append(method+" END ["+  remoteAddr+"] "+pathInContext+" = "+resource+' '+begin+'-'+end+'/'+length+'\n');
    }

    public void logUploadReject(String remoteAddr, String tag, String remoteName, String reason)
    {
        area.append("UPLOAD REJECT ["+remoteAddr+"] "+tag+" = "+remoteName+" ; "+reason+'\n');
    }

    public void logUploadStart(String remoteAddr, String tag, String remoteName)
    {
	area.append("UPLOAD BEGIN ["+remoteAddr+"] "+tag+" = "+remoteName+'\n');
    }

    public void logUploadFinish(String remoteAddr, String tag, String remoteName, long l)
    {
	area.append("UPLOAD END ["+  remoteAddr+"] "+tag+" = "+remoteName+" *"+l+'\n');
    }

    public void logUploadFail(String remoteAddr, String tag, String remoteName)
    {
	area.append("UPLOAD FAIL ["+ remoteAddr+"] "+tag+" = "+remoteName+'\n');
    }

    public void logListenPortChanged(String oldPort, int newPort)
    {
	area.append("PORT CHANGE old="+oldPort+" new="+newPort+'\n');
    }
}
