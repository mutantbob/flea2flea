/*
 * Copyright 2002
 * Robert Forsman <thoth@purplefrog.com>
 */
package com.purplefrog.flea2flea;

import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.prefs.*;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;

import org.mortbay.http.*;
import org.mortbay.http.handler.*;
import org.mortbay.jetty.servlet.*;
import org.mortbay.util.*;

public class Main
    extends JFrame
{
    private OfferingsTable offeringsTable;
    private List offerings;
    private DonationList donations;
    private DonationsTable donationsTable;
    private HttpServer httpServer;
    private LocalAddressReadout addressReadout;
    private Logger logger;

    public Main(List offerings_, DonationList donations_, Logger logger, HttpServer httpserver)
    {
        super("Flea2Flea");
        offerings = offerings_;
        donations = donations_;
	httpServer = httpserver;
	this.logger = logger;

        final MenuBar mb = new MenuBar();
	fillMenuBar(mb);

	setMenuBar(mb);

        JSplitPane alpha = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        JSplitPane beta = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        alpha.setBottomComponent(beta);

        offeringsTable = new OfferingsTable(offerings);
	JScrollPane sp1 = new JScrollPane(offeringsTable);
	sp1.setBorder(BorderFactory.createTitledBorder("Available to downloaders"));
	alpha.setTopComponent(sp1);

        donationsTable = new DonationsTable(donations);
	JScrollPane sp2 = new JScrollPane(donationsTable);
	sp2.setBorder(BorderFactory.createTitledBorder("Awaiting uploaders"));
	beta.setTopComponent(sp2);

	logger.setBorder(BorderFactory.createTitledBorder("Event log"));
        beta.setBottomComponent(logger);

	alpha.setDividerLocation(250);
	beta.setDividerLocation(250);

	getContentPane().setLayout(new BorderLayout());
        getContentPane().add(alpha);

	addressReadout = new LocalAddressReadout(httpServer);
	addressReadout.setBorder(BorderFactory.createTitledBorder("Local Addresses"));
	getContentPane().add(addressReadout, BorderLayout.NORTH);
    }

    protected final void fillMenuBar(MenuBar mb)
    {
	{
	    final Menu menu = new Menu("Menu");

	    final MenuItem ano = new MenuItem("Add Offerings");
	    ano.addActionListener(new AddMultipleOfferingsCallback());
	    menu.add(ano);

	    final MenuItem wo = new MenuItem("Withdraw Offering");
	    wo.addActionListener(new RemoveOfferingCallback());
	    menu.add(wo);

	    final MenuItem au = new MenuItem("Add Uploadable");
	    au.addActionListener(new AddUploadableCallback());
	    menu.add(au);

	    final MenuItem ru = new MenuItem("Remove Uploadable");
	    ru.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e)
		{
		    int row = donationsTable.getSelectedRow();
		    if (0 > row) return;
		    donations.remove(row);
		  }
	    });
	    menu.add(ru);

	    menu.add(new MenuItem("-"));

	    MenuItem cp = new MenuItem("Change Port");
	    cp.addActionListener(new ChangePortCallback());
	    menu.add(cp);

	    menu.add(new MenuItem("-"));

	    MenuItem omega = new MenuItem("Exit");
	    omega.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e)
		{
		    int rval = JOptionPane.showConfirmDialog(Main.this, "If you have any downloaders or uploaders connected, they will be interrupted.\nAfter exiting, files will no longer be available to peers.", "Confirm exit", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		    if ( JOptionPane.OK_OPTION == rval)
			System.exit(0);
		}
	    });
	    menu.add(omega);

	    mb.add(menu);
	}

	{
	    final Menu menu = new Menu("Help");

	    final MenuItem about = new MenuItem("About");
	    about.addActionListener(new PopupHTMLViewer("docs/about.html"));
	    menu.add(about);

	    final MenuItem howto = new MenuItem("How To ...");
	    howto.addActionListener(new PopupHTMLViewer("docs/howto.html"));
	    menu.add(howto);

	    final MenuItem scenarios = new MenuItem("use scenarios");
	    scenarios.addActionListener(new PopupHTMLViewer("docs/scenarios.html"));
	    menu.add(scenarios);

	    mb.add(menu);
	}
    }

    public Dimension getPreferredSize()
    {
	Dimension d = super.getPreferredSize();
	int h = Math.min(900, Math.min(Toolkit.getDefaultToolkit().getScreenSize().height-20, d.height));

	return new Dimension(d.width, h);
    }

    public static void main(String[] argv_)
    {
	int port = 8080;
	try {
	    port = Integer.parseInt(Preferences.userNodeForPackage(Main.class).get("port", "8080"));
	} catch (NumberFormatException e) {
	    // bury
	}

	LinkedList argv = new LinkedList(Arrays.asList(argv_));
	while (!argv.isEmpty()) {
	    String argv0 = (String) argv.getFirst();
	    if ("-p".equals(argv0)) {
		argv.removeFirst();
		port = Integer.parseInt((String)argv.removeFirst());
	    } else
		break;
	}

        HTMLViewerFrame dlg = new HTMLViewerFrame(Main.class.getResource("docs/about.html"));
        centerWindow(dlg);
        dlg.setVisible(true);
        dlg.selfDestruct(20);

        final List offerings = new ArrayList(argv.size());

	while (!argv.isEmpty()) {
	    String argv0 = (String)argv.removeFirst();
	    File f = new File(argv0);
	    offerings.add(new Offering(f.getName(), f));
	}

	DonationList donations = new DonationList();
//	donations.add(new Donation("ooga", new File("/tmp/x"), 4L<<20));
        Logger logger = new Logger();
        HttpServer x = fireUpWebServer(logger, new WiggyMap(offerings), donations, port);

        Main fr = new Main(offerings, donations, logger, x);
        fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fr.pack();
        centerWindow(fr);
        fr.setVisible(true);

        dlg.requestFocus();
    }

    private static HttpServer fireUpWebServer(Logger logger, Map offerings, DonationList donations, int port)
    {
        HttpServer httpServer = new HttpServer();
        SocketListener listener = new SocketListener();
        listener.setPort(port);
        httpServer.addListener(listener);

        HttpContext context = new HttpContext();
        context.setContextPath("/");
        context.setResourceBase("/");
        context.addHandler(new OutboundHandler(offerings, logger));
        //context.addHandler(new IncomingHandler(logger));
        context.addHandler(new RootListing(offerings));
        final ServletHandler servletHandler = new ServletHandler();
        IncomingHandler.logger = logger;
        IncomingHandler.donations = donations;
        servletHandler.addServlet(IncomingHandler.URI,
            IncomingHandler.class.getName()
            //Dump.class.getName()
        );
        context.addHandler(servletHandler);
        context.addHandler(new NotFoundHandler());

        httpServer.addContext(context);

        try {
            httpServer.start();
        } catch (MultiException e) {
            e.printStackTrace();
        }

	Preferences.userNodeForPackage(Main.class).put("port", ""+listener.getPort());

	return httpServer;
    }

    public static String htmlEncode(CharSequence str)
    {
        StringBuffer rval = new StringBuffer();

        for (int i=0; i<str.length(); i++) {
            final char ch = str.charAt(i);
            switch (ch) {
                case '&': rval.append("&amp;"); break;
                case '<': rval.append("&lt;"); break;
                case '>': rval.append("&gt;"); break;
                case '"': rval.append("&quot;"); break;
                default: rval.append(ch);
            }
        }

        return rval.toString();
    }

    public static void centerWindow(Window w)
    {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension sz = w.getSize();

        w.setLocation((d.width-sz.width)/2, (d.height-sz.height)/2);
    }

    public static String URLEncode(String str)
    {
	   StringBuffer rval = new StringBuffer();

        for (int i=0; i<str.length(); i++) {
            final char ch = str.charAt(i);
            switch (ch) {
                case '&': rval.append("&amp;"); break;
                case '<': rval.append("&lt;"); break;
                case '>': rval.append("&gt;"); break;
                case '"': rval.append("&quot;"); break;
		case '?': rval.append("%3f"); break;
                default: rval.append(ch);
            }
        }

        return rval.toString();
    }

    public static class WiggyMap
            extends AbstractMap
    {
        private final List offerings;

        public WiggyMap(List offerings)
        {
            this.offerings = offerings;
        }

	public Object get(Object key)
	{
	    for (Iterator iter = offerings.iterator(); iter.hasNext();) {
		Entry x = (Entry) iter.next();
		if (key.equals(x.getKey()))
		    return x;
	    }
	    return null;
	}

	public int size()
	{
	    return offerings.size();
	}

	public Collection values()
	{
	    return Collections.unmodifiableCollection(offerings);
	}

        public Set entrySet()
        {
	    if(true) {
		throw new UnsupportedOperationException("que?");
	    } else {
		Set rval = new TreeSet();
		for (Iterator iter = offerings.iterator(); iter.hasNext();) {
		    Map.Entry of = (Map.Entry) iter.next();
		    rval.add(of);
		}
		return rval;
	    }
        }
    }

    private class RemoveOfferingCallback
	    implements ActionListener
    {
        public void actionPerformed(ActionEvent e) {
	    int[] sel = offeringsTable.getSelectedRows();

	    for (int i=sel.length-1; 0 <= i; i--) {
		int row = sel[i];
		offerings.remove(row);
		offeringsTable.tableChanged(new TableModelEvent(offeringsTable.getModel(), row, row,
			TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE));
	    }
        }
    }

    private class AddUploadableCallback
	    implements ActionListener
    {
	public void actionPerformed(ActionEvent e)
	{
	    AddUploadableDialog dlg = new AddUploadableDialog(Main.this);

	    dlg.pack();
	    centerWindow(dlg);
	    dlg.setVisible(true);

	    if (null != dlg.donation)
		donations.add(dlg.donation);
	}
    }

    private class ChangePortCallback
	    implements ActionListener
    {
	public void actionPerformed(ActionEvent evt)
	{
	    HttpListener[] socks = httpServer.getListeners();
	    String initialText = 0 < socks.length ? socks[0].getPort() + "" : null;

	    int np = fireDialog(initialText);

	    if (0 >= np)
		return;

	    try {
		rigNewSocket(np);
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	    logger.logListenPortChanged(initialText, np);
	    addressReadout.updateReadout();
	}

	private void rigNewSocket(int np)
		throws Exception
	{
	    HttpListener[] socks;
	    socks = httpServer.getListeners();
	    SocketListener newListener = new SocketListener();
	    newListener.setPort(np);
	    httpServer.addListener(newListener);
	    newListener.start();
	    for (int i = 0; i < socks.length; i++) {
		httpServer.removeListener(socks[i]);
	    }

	    Preferences.userNodeForPackage(Main.class).put("port", ""+newListener.getPort());
	}

	private int fireDialog(String initialText)
	{
	    final int[] newport = new int[] { -1 };
	    final JDialog dlg = new JDialog(Main.this, "new port", true);

	    Container c = dlg.getContentPane();
	    c.setLayout(new BorderLayout());
	    c.add(new JLabel("New HTTP server port: "), BorderLayout.WEST);
	    final JTextField textField = new JTextField(10);

	    if (null != initialText) {
		textField.setText(initialText);
	    }
	    c.add(textField);
	    ActionListener goTime = new ActionListener()
	    {
		public void actionPerformed(ActionEvent e)
		{
		    newport[0] = Integer.parseInt(textField.getText());
		    dlg.setVisible(false);
		}
	    };
	    textField.addActionListener(goTime);

	    JButton ok = new JButton("OK");
	    c.add(ok, BorderLayout.SOUTH);
	    ok.addActionListener(goTime);

	    dlg.pack();
	    centerWindow(dlg);

	    dlg.setVisible(true);

	    int np = newport[0];
	    return np;
	}

    }

    private static class PopupHTMLViewer
	    implements ActionListener
    {
	private final URL url;

	private PopupHTMLViewer(String resourcePath)
	{
	    url = Main.class.getResource(resourcePath);
	}

	public void actionPerformed(ActionEvent e)
	{
	    new HTMLViewerFrame(url).setVisible(true);
	}
    }

    protected File cwd = new File(".");

    private class AddMultipleOfferingsCallback
	    implements ActionListener
    {
	public void actionPerformed(ActionEvent e)
	{
	    JFileChooser fc = new JFileChooser();
	    fc.setCurrentDirectory(cwd);
	    fc.setMultiSelectionEnabled(true);
	    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
	    if (JFileChooser.APPROVE_OPTION != fc.showDialog(Main.this, "Share"))
		return;

	    int row = offerings.size();
	    File[] x = fc.getSelectedFiles();
	    List bad = new LinkedList();
	    for (int i = 0; i < x.length; i++) {
		File file = x[i];
		if (!file.isFile()) {
		    bad.add(file);
		    continue;
		}
		cwd = file.getParentFile();
		offerings.add(new Offering(file.getName(), file));
	    }
	    offeringsTable.tableChanged(new TableModelEvent(offeringsTable.getModel(), row, row+x.length-1, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));


	    if (!bad.isEmpty()) {
		StringBuffer buf = new StringBuffer();
		buf.append("Can not share non-files: ");
		boolean first = true;
		for (Iterator iter = bad.iterator(); iter.hasNext();) {
		    File nonFile = (File) iter.next();
		    if (first) {
			first=false;
		    } else {
			buf.append(", ");
		    }
		    buf.append(nonFile);
		}
		buf.append('.');

		JOptionPane.showMessageDialog(Main.this, buf, "doh!", JOptionPane.ERROR_MESSAGE);
	    }
	}
    }
}
