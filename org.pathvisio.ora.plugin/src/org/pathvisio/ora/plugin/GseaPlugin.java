package org.pathvisio.ora.plugin;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.desktop.gex.SimpleGex;
import org.pathvisio.desktop.plugin.Plugin;

public class GseaPlugin implements Plugin, BundleActivator
{
	private PvDesktop desktop;

	public void done()
	{
		desktop.unregisterMenuAction("Data", oraWizardAction);
	}

	public void init(PvDesktop desktop)
	{
		this.desktop = desktop;
		oraWizardAction = new OraWizardAction(desktop);		
		desktop.registerMenuAction ("Data", oraWizardAction);	
	}

	private OraWizardAction oraWizardAction;

	public static class OraWizardAction extends AbstractAction
	{
		private final PvDesktop desktop;

		public OraWizardAction(PvDesktop pvDesktop)
		{
			this.desktop = pvDesktop;
<<<<<<< HEAD:org.pathvisio.ora.plugin/src/org/pathvisio/ora/plugin/GseaPlugin.java
			putValue (NAME, "Gene Set Enrichment Analysis");
			putValue (SHORT_DESCRIPTION, "Perform simple over-representation analysis to find pathways changed pathways.");
=======
			putValue (NAME, "Statistics...");
			putValue (SHORT_DESCRIPTION, "Perform simple over-representation analysis to find changed pathways.");
>>>>>>> Moving org.pathvisio.data package to core of PathVisio:src2/src/org/pathvisio/ora/plugin/GseaPlugin.java
		}

		public void actionPerformed (ActionEvent e)
		{
			SimpleGex gex = desktop.getGexManager().getCurrentGex();
			if (gex == null)
			{
				JOptionPane.showMessageDialog(desktop.getFrame(), "Select an expression dataset first");
			}
			else
			{
				StatisticsWizard.run(desktop);
			}
		}
	}

	@Override
	public void start(BundleContext context) throws Exception
	{
		GseaPlugin plugin = new GseaPlugin();
		context.registerService(Plugin.class.getName(), plugin, null);	
	}

	@Override
	public void stop(BundleContext context) throws Exception
	{
	}

}
