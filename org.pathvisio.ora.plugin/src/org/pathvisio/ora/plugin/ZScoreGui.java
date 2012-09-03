package org.pathvisio.ora.plugin;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.gui.SimpleFileFilter;
import org.jdesktop.swingworker.SwingWorker;
import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.preferences.PreferenceManager;
import org.pathvisio.core.util.ProgressKeeper;
import org.pathvisio.data.Criterion;
import org.pathvisio.data.DataException;
import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.desktop.gex.CachedData;
import org.pathvisio.desktop.gex.GexManager;
import org.pathvisio.desktop.util.TextFieldUtils;
import org.pathvisio.gui.ProgressDialog;
import org.pathvisio.gui.SwingEngine;
import org.pathvisio.ora.util.StatisticsTableModel;
import org.pathvisio.ora.zscore.Column;
import org.pathvisio.ora.zscore.StatisticsPathwayResult;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Dialog to let the user set parameters, start calculation and view results.
 */
public class ZScoreGui
{
	/**
	 * the panel for entering an expression, complete
	 * with list boxes for selecting operator and sample.
	 * TODO: figure out if this can be re-used in the color rule panel
	 */
	private static class CriterionPanel extends JPanel
	{
		private JTextField txtExpr;
		private JLabel lblError;
		private Criterion myCriterion = new Criterion();
		private final List<String> sampleNames;

		public Criterion getCriterion()
		{
			return myCriterion;
		}

		private void updateCriterion()
		{
			String error = myCriterion.setExpression(
					txtExpr.getText(), sampleNames);
			if (error != null)
			{
				lblError.setText(error);
			}
			else
			{
				lblError.setText ("OK");
			}
		}

		private CriterionPanel(List<String> aSampleNames)
		{
			super();
			sampleNames = aSampleNames;

			FormLayout layout = new FormLayout (
					"4dlu, min:grow, 4dlu, min:grow, 4dlu",
					"4dlu, pref, 4dlu, pref, 4dlu, [50dlu,min]:grow, 4dlu, pref, 4dlu");
			layout.setColumnGroups(new int[][]{{2,4}});
			setLayout(layout);
			CellConstraints cc = new CellConstraints();
			add (new JLabel ("Expression: "), cc.xy(2,2));
			txtExpr = new JTextField(40);
			txtExpr.getDocument().addDocumentListener(new DocumentListener()
			{
				public void changedUpdate(DocumentEvent e)
				{
					updateCriterion();
				}

				public void insertUpdate(DocumentEvent e)
				{
					updateCriterion();
				}

				public void removeUpdate(DocumentEvent e)
				{
					updateCriterion();
				}
			});

			add (txtExpr, cc.xyw(2,4,3));

			final JList lstOperators = new JList(Criterion.TOKENS);
			add (new JScrollPane (lstOperators), cc.xy (2,6));

			lstOperators.addMouseListener(new MouseAdapter()
			{
				public void mouseClicked(MouseEvent me)
				{
					int selectedIndex = lstOperators.getSelectedIndex();
					if (selectedIndex >= 0)
					{
						String toInsert = Criterion.TOKENS[selectedIndex];
						TextFieldUtils.insertAtCursorWithSpace(txtExpr, toInsert);
					}
					// after clicking on the list, move focus back to text field so
					// user can continue typing
					txtExpr.requestFocusInWindow();
					// on Mac L&F, requesting focus leads to selecting the whole field
					// move caret a bit to work around. Last char is a space anyway.
					txtExpr.setCaretPosition(txtExpr.getDocument().getLength() - 1);
				}
			} );

			final JList lstSamples = new JList(sampleNames.toArray());

			lstSamples.addMouseListener(new MouseAdapter()
			{
				public void mouseClicked(MouseEvent me)
				{
					int selectedIndex = lstSamples.getSelectedIndex();
					if (selectedIndex >= 0)
					{
						String toInsert = "[" + sampleNames.get(selectedIndex) + "]";
						TextFieldUtils.insertAtCursorWithSpace(txtExpr, toInsert);
					}
					// after clicking on the list, move focus back to text field so
					// user can continue typing
					txtExpr.requestFocusInWindow();
					// on Mac L&F, requesting focus leads to selecting the whole field
					// move caret a bit to work around. Last char is a space anyway.
					txtExpr.setCaretPosition(txtExpr.getDocument().getLength() - 1);
				}
			} );

			add (new JScrollPane (lstSamples), cc.xy (4,6));
			lblError = new JLabel("OK");
			add (lblError, cc.xyw (2,8,3));

			txtExpr.requestFocus();
		}
	}

	private ZScoreGui.CriterionPanel critPanel;
	private JButton btnSave;
	private StatisticsResult result = null;
	private final GexManager gm;
	private final SwingEngine se;
	private JDialog dlg;
	private JTable tblResult;
	private JLabel lblResult;

	/**
	 * Save the statistics results to tab delimted text
	 */
	private void doSave()
	{
		JFileChooser jfc = new JFileChooser();
		jfc.setDialogTitle("Save results");
		jfc.setFileFilter(new SimpleFileFilter ("Tab delimited text", "*.txt", true));
		jfc.setDialogType(JFileChooser.SAVE_DIALOG);
		jfc.setCurrentDirectory(PreferenceManager.getCurrent().getFile(StatisticsPreference.STATS_DIR_LAST_USED_RESULTS));
		if (jfc.showDialog(dlg, "Save") == JFileChooser.APPROVE_OPTION)
		{
			File f = jfc.getSelectedFile();
			PreferenceManager.getCurrent().setFile(StatisticsPreference.STATS_DIR_LAST_USED_RESULTS, jfc.getCurrentDirectory());
			if (!f.toString().endsWith (".txt"))
			{
				f = new File (f + ".txt");
			}
			try
			{
				result.save (f);
			}
			catch (IOException e)
			{
				JOptionPane.showMessageDialog(dlg, "Could not save results: " + e.getMessage());
				Logger.log.error ("Could not save results", e);
			}
		}
	}

	private final PvDesktop desktop;
	
	public ZScoreGui(PvDesktop desktop)
	{
		this.desktop = desktop;
		this.se = desktop.getSwingEngine();
		this.gm = desktop.getGexManager();
	}
	
	/**
	 * Pop up the statistics dialog
	 */
	public JPanel getConfigPanel()
	{
		critPanel = new CriterionPanel(gm.getCurrentGex().getSampleNames());
		return critPanel;
	}

	public void doResults(File pwDir)
	{
		dlg = new JDialog (se.getFrame(), "Pathway statistics", false);

		FormLayout layout = new FormLayout (
				"4dlu, pref:grow, 4dlu, pref, 4dlu",
				"4dlu, fill:[pref,250dlu], 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, fill:min:grow");
		dlg.setLayout(layout);

		CellConstraints cc = new CellConstraints();

		JPanel pnlButtons = new JPanel();

		btnSave = new JButton ("Save results");
		pnlButtons.add (btnSave);
		btnSave.setEnabled(false);

		dlg.add (pnlButtons, cc.xyw (2,8,3));

		lblResult = new JLabel(); //Label for adding general results after analysis is done

		dlg.add(lblResult, cc.xyw(2, 10, 3));

		//
		dlg.add (new JScrollPane (tblResult), cc.xyw (2,12,3));

		tblResult = new JTable ();
		tblResult.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent me)
			{
				int row = tblResult.getSelectedRow();
				final StatisticsPathwayResult sr = ((StatisticsTableModel)(tblResult.getModel())).getRow(row);

					//TODO: here I want to use SwingEngine.openPathway, but I need to
					// be able to wait until the process is finished!
				se.openPathway(sr.getFile());
			}
		});

		btnSave.addActionListener(new ActionListener ()
		{
			public void actionPerformed(ActionEvent ae)
			{
				doSave();
			}
		});
		
		dlg.pack();
		dlg.setSize(600, 600); //TODO store preference
		dlg.setLocationRelativeTo(se.getFrame());

		doCalculate (pwDir, critPanel.getCriterion());

		dlg.setVisible(true);

	}
	
	/**
	 * asynchronous statistics calculation function
	 */
	private void doCalculate(final File pwDir, final Criterion crit)
	{
		btnSave.setEnabled (false);

		ProgressKeeper pk = new ProgressKeeper(100);
		final ZScoreWorker worker = new ZScoreWorker(crit, pwDir, gm.getCachedData(), se.getGdbManager().getCurrentGdb(), pk);
		ProgressDialog d = new ProgressDialog(
				JOptionPane.getFrameForComponent(dlg),
				"Calculating Z-scores", pk, true, true
		);
		worker.execute();
		d.setVisible(true);
	}

	private class ZScoreWorker extends SwingWorker <StatisticsResult, Void>
	{
		private final ZScoreCalculator calculator;
		private ProgressKeeper pk;

		// temporary model that will be filled with intermediate results.
		private StatisticsTableModel temp;
		private boolean useMappFinder;

		ZScoreWorker(Criterion crit, File pwDir, CachedData cache, IDMapper gdb, ProgressKeeper pk)
		{
			this.pk = pk;
			calculator = new ZScoreCalculator (crit, pwDir, cache, gdb, pk);
			temp = new StatisticsTableModel(PreferenceManager.getCurrent().getBoolean(
					StatisticsPreference.STATS_RESULT_INCLUDE_FILENAME));
			temp.setColumns(new Column[] {Column.PATHWAY_NAME, Column.R, Column.N, Column.TOTAL, Column.PCT, Column.ZSCORE});
			tblResult.setModel(temp);
			useMappFinder = PreferenceManager.getCurrent().getBoolean(StatisticsPreference.MAPPFINDER_COMPATIBILITY);
		}

		@Override
		protected StatisticsResult doInBackground() throws IDMapperException, DataException
		{
			StatisticsResult result;

			if (useMappFinder)
			{
				result = calculator.calculateMappFinder();
			}
			else
			{
				result = calculator.calculateAlternative();
			}
			return result;
		}

		@Override
		protected void done()
		{
			if (!pk.isCancelled())
			{
				StatisticsResult result;
				try {
					result = get();
					if (result.stm.getRowCount() == 0)
					{
						JOptionPane.showMessageDialog(null,
						"0 results found, did you choose the right directory?");
					}
					else
					{
						// replace temp tableModel with definitive one
						tblResult.setModel(result.stm);
						lblResult.setText(
							"<html>Rows in data (N): " + result.getBigN() +
							"<br>Rows meeting criterion (R): " + result.getBigR()
						);
						//TODO: Important???
//							statisticsPlugin.result = result;
//						dlg.pack();
					}
				}
				catch (InterruptedException e)
				{
					JOptionPane.showMessageDialog(null,
							"Exception while calculating statistics\n" + e.getMessage());
					Logger.log.error ("Statistics calculation exception", e);
				}
				catch (ExecutionException e)
				{
					JOptionPane.showMessageDialog(null,
						"Exception while calculating statistics\n" + e.getMessage());
					Logger.log.error ("Statistics calculation exception", e);
				}
			}
<<<<<<< HEAD:org.pathvisio.ora.plugin/src/org/pathvisio/ora/plugin/ZScoreGui.java
//			btnCalc.setEnabled(true);
=======
>>>>>>> Moving org.pathvisio.data package to core of PathVisio:src2/src/org/pathvisio/ora/plugin/ZScoreGui.java
			btnSave.setEnabled(true);
		}
	}

}