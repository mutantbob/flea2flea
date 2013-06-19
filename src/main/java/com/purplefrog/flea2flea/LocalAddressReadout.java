/*
 * Copyright 2002
 * Robert Forsman <thoth@purplefrog.com>
 */
package com.purplefrog.flea2flea;

import java.awt.*;
import java.net.*;
import java.util.*;
import java.io.*;
import javax.swing.*;

import com.purplefrog.apachehttpcliches.*;

public class LocalAddressReadout
	extends JPanel
{
    public JTextArea readout;
    public BasicHTTPAcceptLoop httpServer;

    public LocalAddressReadout(BasicHTTPAcceptLoop httpServer)
    {
	super(new BorderLayout());

	this.httpServer = httpServer;

	readout = new JTextArea();
	readout.setEditable(false);
	add(readout);

	updateReadout();

    }

    public final void updateReadout()
    {
	String s = readoutString();

	readout.setText(s);
    }

    private String readoutString()
    {
	StringWriter sw = new StringWriter();
	PrintWriter pw = new PrintWriter(sw);

        int localPort = httpServer.serversocket.getLocalPort();
	try {

	    try {
		InetAddress localHost = InetAddress.getLocalHost();
		pw.print("\"main\" interface :  ");
                pw.print(urlFor(localHost, localPort)+'\n');
	    } catch (Exception e) {
		pw.println("Unable to determine main interface: "+e.getLocalizedMessage());
		e.printStackTrace();
	    }
	    if (!anyStarted()) {
		pw.println("offline.\n" +
"Perhaps you are already running Flea2Flea, or something else is \n" +
"using the port we wanted.  Try Menu/Change Port.");
	    } else {
		pw.println();
		
		Enumeration ifs = NetworkInterface.getNetworkInterfaces();

		while (ifs.hasMoreElements()) {
		    NetworkInterface iface = (NetworkInterface) ifs.nextElement();
		    readoutInterface(pw, iface, localPort);
		}
	    }
	} catch (IOException e) {
	    e.printStackTrace(pw);
	}

	String s = sw.toString();
	String s2 = s.substring(0, s.length()-1);
	return s2;
    }

    private boolean anyStarted()
    {
        return true;
    }

    private static void readoutInterface(PrintWriter pw, NetworkInterface iface, int port)
    {
	pw.println("interface "+iface.getName()+" = "+iface.getDisplayName());
	Enumeration addrs = iface.getInetAddresses();
	while (addrs.hasMoreElements()) {
	    InetAddress addr = (InetAddress) addrs.nextElement();
            pw.println("  " + urlFor(addr, port));
	}
    }

    private static String urlFor(InetAddress addr, int port)
    {
        if (addr instanceof Inet6Address) {
            Inet6Address addr6 = (Inet6Address) addr;
            String gunk = "[" + addr6.getHostAddress()
                // .replaceAll("%", "%25")
                + "]";
            return "http://" + gunk + ":" + port + '/';
        }    else {
            return "http://"+addr.getHostAddress()+':'+port+'/';
        }
    }
}
