package burp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Menu implements IContextMenuFactory {
    private IExtensionHelpers helpers;
    private IBurpExtenderCallbacks callbacks;
    private WSDLParserTab tab;
	private PrintWriter stdout;
	private PrintWriter stderr;
    public static Timer timer;

    public Menu(IBurpExtenderCallbacks callbacks) {
        helpers = callbacks.getHelpers();
		stdout = new PrintWriter(callbacks.getStdout(), true);
		stderr = new PrintWriter(callbacks.getStderr(), true);
        tab = new WSDLParserTab(callbacks);
        this.callbacks = callbacks;
        timer = new Timer();
    }

    public List<JMenuItem> createMenuItems(
            final IContextMenuInvocation invocation) {
        List<JMenuItem> list;
        list = new ArrayList<JMenuItem>();


        JMenuItem item = new JMenuItem("Parse WSDL");

        item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
                WSDLParser parser = new WSDLParser(callbacks,helpers, tab);//新建一个解析器
                try {
                    new Worker(parser,invocation, tab, callbacks,false).execute();//构造新的请求，并创建图形界面显示请求内容
                } catch (Exception e1) {
                    e1.printStackTrace();
                }

			}
		});
        
        list.add(item);

        JMenuItem itemScan = new JMenuItem("Parse WSDL And Do Active Scan");

        itemScan.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
                WSDLParser parser = new WSDLParser(callbacks,helpers, tab);//新建一个解析器
                try {
                    new Worker(parser,invocation, tab, callbacks,true).execute();//构造新的请求，并创建图形界面显示请求内容
                } catch (Exception e1) {
                    e1.printStackTrace();
                }

			}
		});
        list.add(itemScan);

        return list;
    }

}

class Worker extends SwingWorker<Void,Void> {

    private JDialog dialog = new JDialog();
    private WSDLParser parser;
    private IContextMenuInvocation invocation;
    private WSDLParserTab tab;
    private IBurpExtenderCallbacks callbacks;
    private int status;
	private boolean doActiveScan = false;
	private PrintWriter stdout;
	private PrintWriter stderr;

    public Worker(WSDLParser parser, IContextMenuInvocation invocation, WSDLParserTab tab, IBurpExtenderCallbacks callbacks) {
        JProgressBar progressBar = new JProgressBar();
        progressBar.setString("Parsing WSDL");
        progressBar.setStringPainted(true);
        progressBar.setIndeterminate(true);
        dialog.getContentPane().add(progressBar);
        dialog.pack();
        dialog.setLocationRelativeTo(tab.getUiComponent().getParent());
        dialog.setModal(false);
        dialog.setVisible(true);
        this.parser = parser;
        this.invocation = invocation;
        this.tab = tab;
        this.callbacks = callbacks;
		stdout = new PrintWriter(callbacks.getStdout(), true);
		stderr = new PrintWriter(callbacks.getStderr(), true);
        
    }
    
    public Worker(WSDLParser parser, IContextMenuInvocation invocation, WSDLParserTab tab, IBurpExtenderCallbacks callbacks,boolean DoActiveScan) {
    	this(parser,invocation, tab, callbacks);
        this.doActiveScan = DoActiveScan;
    }

    @Override
    protected Void doInBackground() throws Exception {
    	for( IHttpRequestResponse message:invocation.getSelectedMessages()) {
    		try {
    			status = parser.parseWSDL(message, callbacks, doActiveScan);//构造新的接口请求
    			
    		}catch (Exception e) {
    			e.printStackTrace(stderr);
    		}
    	}
    	//stderr.println("background done "+doActiveScan);
        return null;
    }

    @Override
    protected void done() {
        dialog.dispose();
        if (status != -1 && status != -2 && status != -3) {
            {
                final JTabbedPane parent = (JTabbedPane) tab.getUiComponent().getParent();
                final int index = parent.indexOfComponent(tab.getUiComponent());
                parent.setBackgroundAt(index, new Color(229, 137, 1));

                Menu.timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        parent.setBackgroundAt(index, new Color(0, 0, 0));
                    }
                }, 5000);

            }
        }
    }
}