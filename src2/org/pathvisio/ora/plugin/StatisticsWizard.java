package org.pathvisio.ora.plugin;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.pathvisio.core.preferences.PreferenceManager;
import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.desktop.util.BrowseButtonActionListener;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.nexes.wizard.Wizard;
import com.nexes.wizard.WizardPanelDescriptor;

public class StatisticsWizard extends Wizard
{
	private final PvDesktop pvDesktop;
	private GseaGui gsea;
	private ZScoreGui zscore;
	private File pwDir;
	
	public StatisticsWizard(PvDesktop pvDesktop)
	{
		this.pvDesktop = pvDesktop;
		gsea = new GseaGui(pvDesktop);
		zscore = new ZScoreGui(pvDesktop);
		
		getDialog().setTitle("Over-Representation analysis wizard");

		this.registerWizardPanel(new ChooseSetsPage());
		this.registerWizardPanel(new MethodPage());
		this.registerWizardPanel(new GseaConfigPage());
		this.registerWizardPanel(new ZscoreConfigPage());

		setCurrentPanel(ChooseSetsPage.IDENTIFIER);
	}

	private class ChooseSetsPage extends WizardPanelDescriptor
	{
		public static final String IDENTIFIER = "CHOOSE_SETS_PAGE";

		JTextField txtInput;
		JButton btnInput;

		public ChooseSetsPage()
		{
			super(IDENTIFIER);
		}

		@Override
		protected Component createContents()
		{
			DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout(""));

			builder.setDefaultDialogBorder();
			builder.appendColumn("right:pref");
			builder.appendColumn("pref");
			builder.appendColumn("pref");

			txtInput = new JTextField(40);
			txtInput.setText(PreferenceManager.getCurrent().get(StatisticsPreference.STATS_DIR_LAST_USED_PATHWAY));
			btnInput = new JButton("Browse");
			btnInput.addActionListener(new BrowseButtonActionListener(txtInput, null,
					JFileChooser.DIRECTORIES_ONLY));
			
			builder.append("Input file", txtInput, btnInput);

			return builder.getPanel();
		}

		public Object getNextPanelDescriptor()
		{
			return MethodPage.IDENTIFIER;
		}

		public Object getBackPanelDescriptor()
		{
			return null;
		}

		public void aboutToDisplayPanel()
		{
			getWizard().setPageTitle("Choose directory of pathways");
		}
		
		@Override
		public void aboutToHidePanel()
		{
			pwDir = new File(txtInput.getText());
			PreferenceManager.getCurrent().setFile(StatisticsPreference.STATS_DIR_LAST_USED_PATHWAY, pwDir);
		}
	}

	private static enum Methods
	{
		GSEA, ZSCORE
	};

	private Methods selectedMethod = Methods.ZSCORE; // if false, means zscore
														// is selected.

	private class MethodPage extends WizardPanelDescriptor implements ActionListener
	{
		public static final String IDENTIFIER = "METHOD_PAGE";

		JRadioButton btnGsea;
		JRadioButton btnZscore;

		public MethodPage()
		{
			super(IDENTIFIER);
		}

		@Override
		protected Component createContents()
		{
			btnGsea = new JRadioButton("Gene Set Enrichment Analysis (GSEA)");
			btnZscore = new JRadioButton("Z-score");
			ButtonGroup grpMethods = new ButtonGroup();

			DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout(""));
			builder.setDefaultDialogBorder();
			builder.appendColumn("pref");

			grpMethods.add(btnGsea);
			grpMethods.add(btnZscore);

			builder.append(btnGsea);
			builder.append(btnZscore);

			switch (selectedMethod)
			{
			case GSEA:
				btnGsea.setSelected(true);
				break;
			case ZSCORE:
				btnZscore.setSelected(true);
				break;
			}

			btnGsea.addActionListener(this);
			btnZscore.addActionListener(this);

			return builder.getPanel();
		}

		@Override
		public void actionPerformed(ActionEvent arg0)
		{
			if (arg0.getSource() == btnGsea)
			{
				selectedMethod = Methods.GSEA;

			}
			else if (arg0.getSource() == btnZscore)
			{
				selectedMethod = Methods.ZSCORE;
			}
		}

		public Object getNextPanelDescriptor()
		{
			return selectedMethod == Methods.GSEA ? GseaConfigPage.IDENTIFIER
					: ZscoreConfigPage.IDENTIFIER;
		}

		public Object getBackPanelDescriptor()
		{
			return ChooseSetsPage.IDENTIFIER;
		}

		public void aboutToDisplayPanel()
		{
			getWizard().setPageTitle("Choose statistical method");
		}
	}

	private class GseaConfigPage extends WizardPanelDescriptor
	{
		public static final String IDENTIFIER = "GSEA_CONFIG_PAGE";

		public GseaConfigPage()
		{
			super(IDENTIFIER);
		}

		@Override
		protected Component createContents()
		{
			return gsea.getConfigPanel();
		}

		public Object getNextPanelDescriptor()
		{
			return WizardPanelDescriptor.FINISH;
		}

		public Object getBackPanelDescriptor()
		{
			return MethodPage.IDENTIFIER;
		}

	}

	private class ZscoreConfigPage extends WizardPanelDescriptor
	{
		public static final String IDENTIFIER = "ZSCORE_CONFIG_PAGE";

		public ZscoreConfigPage()
		{
			super(IDENTIFIER);
		}

		@Override
		protected Component createContents()
		{
			return zscore.getConfigPanel();
		}

		public Object getNextPanelDescriptor()
		{
			return WizardPanelDescriptor.FINISH;
		}

		public Object getBackPanelDescriptor()
		{
			return MethodPage.IDENTIFIER;
		}

	}

	public static void run(PvDesktop desktop)
	{
		StatisticsWizard wizard = new StatisticsWizard(desktop);
		wizard.showModalDialog(desktop.getSwingEngine().getFrame());
		
		if (wizard.getReturnCode() == Wizard.FINISH_RETURN_CODE)
		{
			switch (wizard.selectedMethod)
			{
			case GSEA:
				wizard.gsea.finishedPressed(wizard.pwDir);
				break;
			case ZSCORE:
				wizard.zscore.doResults(wizard.pwDir);
				break;
			}
		}
	}

}
