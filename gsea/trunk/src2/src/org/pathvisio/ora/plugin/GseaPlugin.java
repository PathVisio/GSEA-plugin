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
		// nothing to do.
	}

	public void init(PvDesktop desktop)
	{
		this.desktop = desktop;
		oraWizardAction = new OraWizardAction(desktop);
		
//		// remove the menu item created by the default statistics dialog
//		JMenuBar menuBar = desktop.getSwingEngine().getApplicationPanel().getMenuBar();
//		for (int i = 0; i < menuBar.getMenuCount(); ++i)
//		{
//			JMenu menu = menuBar.getMenu(i);
//			if ("Data".equals(menu.getText()))
//			{
//				for (int j = 0; j < menu.getMenuComponentCount(); ++j)
//				{
//					Component comp = menu.getMenuComponent(j);
					//TODO
//					break;
//				}
//				break;
//			}	
//		}
		
		
		desktop.registerMenuAction ("Data", oraWizardAction);	
	}

	private OraWizardAction oraWizardAction;

	public static class OraWizardAction extends AbstractAction
	{
		private final PvDesktop desktop;

		public OraWizardAction(PvDesktop pvDesktop)
		{
			this.desktop = pvDesktop;
			putValue (NAME, "Statistics...");
			putValue (SHORT_DESCRIPTION, "Perform simple over-representation analysis to find pathways changed pathways.");
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
