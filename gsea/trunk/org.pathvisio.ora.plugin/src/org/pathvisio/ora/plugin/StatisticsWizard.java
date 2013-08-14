package org.pathvisio.ora.plugin;

import java.awt.Component;
import java.awt.Dimension;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import org.pathvisio.core.preferences.PreferenceManager;
import org.pathvisio.core.util.ProgressKeeper;
import org.pathvisio.core.util.ProgressKeeper.ProgressEvent;
import org.pathvisio.core.util.ProgressKeeper.ProgressListener;
import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.desktop.util.BrowseButtonActionListener;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.nexes.wizard.Wizard;
import com.nexes.wizard.WizardPanelDescriptor;

public class StatisticsWizard extends Wizard
{
	private GseaGui gsea;
	private File pwDir;
	private File geneSetCollection;
	
	public StatisticsWizard(PvDesktop pvDesktop)
	{
		gsea = new GseaGui(pvDesktop);
		getDialog().setPreferredSize(new Dimension(800,500));
		
		getDialog().setTitle("Over-Representation analysis wizard");
		this.registerWizardPanel(new ChooseSetsPage());
		this.registerWizardPanel(new GseaConfigPage());
		this.registerWizardPanel(new CalcPage());

		setCurrentPanel(ChooseSetsPage.IDENTIFIER);
	}

	private class ChooseSetsPage extends WizardPanelDescriptor
	{
		public static final String IDENTIFIER = "CHOOSE_SETS_PAGE";

		JTextField txtInput;
		JButton btnInput;
		JTextField txtGeneSets;
		JButton btnGeneSets;

		public ChooseSetsPage()
		{
			super(IDENTIFIER);
		}

		@Override
		protected Component createContents()
		{
			DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("right:pref, 3dlu, pref, 3dlu, pref", "15dlu, pref, 10dlu, pref"));
			CellConstraints cc = new CellConstraints();

			txtInput = new JTextField(40);
			txtInput.setText(PreferenceManager.getCurrent().get(StatisticsPreference.STATS_DIR_LAST_USED_PATHWAY));
			btnInput = new JButton("Browse");
			btnInput.addActionListener(new BrowseButtonActionListener(txtInput, null,
					JFileChooser.DIRECTORIES_ONLY));
			
			builder.add(new JLabel("Pathway directory"), cc.xy(1, 2));
			builder.add(txtInput, cc.xy(3, 2));
			builder.add(btnInput, cc.xy(5, 2));

			txtGeneSets = new JTextField(40);
			btnGeneSets = new JButton("Browse");
			btnGeneSets.addActionListener(new BrowseButtonActionListener(txtGeneSets, null, JFileChooser.FILES_ONLY));
			
			builder.add(new JLabel("GeneSet Collection"), cc.xy(1, 4));
			builder.add(txtGeneSets, cc.xy(3, 4));
			builder.add(btnGeneSets, cc.xy(5, 4));
			return builder.getPanel();
		}

		public Object getNextPanelDescriptor()
		{
			return GseaConfigPage.IDENTIFIER;
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
			if(!txtInput.getText().equals("")) {
				pwDir = new File(txtInput.getText());
				PreferenceManager.getCurrent().setFile(StatisticsPreference.STATS_DIR_LAST_USED_PATHWAY, pwDir);
			}
			if(!txtGeneSets.equals("")) geneSetCollection = new File(txtGeneSets.getText());
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
			return CalcPage.IDENTIFIER;
		}

		public Object getBackPanelDescriptor()
		{
			return ChooseSetsPage.IDENTIFIER;
		}

	}
	
	private class CalcPage extends WizardPanelDescriptor implements ProgressListener
	{
	    public static final String IDENTIFIER = "C";

	    public CalcPage()
	    {
	        super(IDENTIFIER);
	    }

	    public Object getNextPanelDescriptor()
	    {
	        return FINISH;
	    }

	    public Object getBackPanelDescriptor()
	    {
	        return GseaConfigPage.IDENTIFIER;
	    }

	    private JProgressBar progressSent;
	    private JTextArea progressText;
	    private ProgressKeeper pk;
	    private JLabel lblTask;

	    @Override
	    public void aboutToCancel()
	    {
	    	// let the progress keeper know that the user pressed cancel.
	    	pk.cancel();
	    }

		protected JPanel createContents()
		{
	    	FormLayout layout = new FormLayout(
	    			"fill:[100dlu,min]:grow",
	    			"pref, pref, fill:pref:grow"
	    	);

	    	DefaultFormBuilder builder = new DefaultFormBuilder(layout);
	    	builder.setDefaultDialogBorder();
	    	
        	pk = new ProgressKeeper(100);
        	pk.addListener(this);
        	
			progressSent = new JProgressBar(0, pk.getTotalWork());
	        builder.append(progressSent);
	        builder.nextLine();
	        lblTask = new JLabel();
	        builder.append(lblTask);

	        progressText = new JTextArea();

			builder.append(new JScrollPane(progressText));
			return builder.getPanel();
		}

	    public void setProgressValue(int i)
	    {
	        progressSent.setValue(i);
	    }

	    public void setProgressText(String msg)
	    {
	        progressText.setText(msg);
	    }

	    public void aboutToDisplayPanel()
	    {
			getWizard().setPageTitle ("GSEA calculation progress");
			
	    	pk = new ProgressKeeper(100);
	    	
	        setProgressValue(0);
	        setProgressText("");

	        getWizard().setNextFinishButtonEnabled(false);
	        getWizard().setBackButtonEnabled(false);
	    }
	    
	    public void displayingPanel()
	    {
	    	SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
				@Override
				protected Void doInBackground() throws Exception {
					try{
						gsea.runCalculation(pwDir, geneSetCollection, pk, progressText);
					} finally {
						pk.setProgress(100);
						pk.finished();
					}
					return null;
				}

				@Override
				public void done() {
					pk.finished();
					pk.setTaskName("Finished");
					getWizard().setNextFinishButtonEnabled(true);
					getWizard().setBackButtonEnabled(true);
				}
			};
			sw.execute();
	    }

		public void progressEvent(ProgressEvent e)
		{
			switch(e.getType())
			{
				case ProgressEvent.FINISHED:
					progressSent.setValue(pk.getTotalWork());
				case ProgressEvent.TASK_NAME_CHANGED:
					lblTask.setText(pk.getTaskName());//TODO fix, doesn't update the label text
					break;
				case ProgressEvent.REPORT:
					progressText.append(e.getProgressKeeper().getReport() + "\n");
					break;
				case ProgressEvent.PROGRESS_CHANGED:
					progressSent.setValue(pk.getProgress());
					break;
			}
		}

	}

	public static void run(PvDesktop desktop)
	{
		StatisticsWizard wizard = new StatisticsWizard(desktop);
		wizard.showModalDialog(desktop.getSwingEngine().getFrame());
		
		if (wizard.getReturnCode() == Wizard.FINISH_RETURN_CODE)
		{
			GseaResultsFrame resultFrame = new GseaResultsFrame(desktop.getFrame(), wizard.gsea.getResults(), desktop);
			resultFrame.setVisible(true);
		}
	}

}
