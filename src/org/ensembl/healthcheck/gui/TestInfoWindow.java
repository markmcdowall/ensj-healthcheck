/*
  Copyright (C) 2004 EBI, GRL
 
  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.
 
  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.
 
  You should have received a copy of the GNU Lesser General Public
  License along with this library; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.ensembl.healthcheck.gui;

import java.util.*;
import javax.swing.JFrame;

import org.ensembl.healthcheck.*;

/**
 * Dialog that displays information about a test that is running (or has just run).
 */
public class TestInfoWindow extends javax.swing.JDialog {

	/** Creates new form TestInfoWindow
	 * @param parent The JFrame opening this dialog.
	 * @param title The title of the dialog.
	 * @param modal Whether or not this window is modal.
	 */
	public TestInfoWindow(java.awt.Frame parent, String title, boolean modal) {
		super(parent, modal);
		this.setTitle(title);
		initComponents();
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	private void initComponents() { //GEN-BEGIN:initComponents
		basePanel = new javax.swing.JPanel();
		infoScrollPane = new javax.swing.JScrollPane();
		infoTextArea = new javax.swing.JTextArea();

		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent evt) {
				closeDialog(evt);
			}
		});

		infoTextArea.setColumns(80);
		infoTextArea.setRows(20);
		infoTextArea.setEditable(false);
		
		infoScrollPane.setViewportView(infoTextArea);
		basePanel.add(infoScrollPane);

		getContentPane().add(basePanel, java.awt.BorderLayout.CENTER);

		pack();
	} //GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt) { //GEN-FIRST:event_closeDialog
		setVisible(false);
		dispose();
	} //GEN-LAST:event_closeDialog

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JPanel basePanel;
	private javax.swing.JScrollPane infoScrollPane;
	private javax.swing.JTextArea infoTextArea;
	// End of variables declaration//GEN-END:variables

	// -------------------------------------------------------------------------
	/**
	 * Append text to the information TextArea.
	 * @param s The text to append.
	 */
	public void append(String s) {

		infoTextArea.append(s);

	} // append

	// -------------------------------------------------------------------------
	/** 
	 * Set the text of the information TextArea to a particular String.
	 * @param s The text to set.
	 */
	public void setText(String s) {

		infoTextArea.setText(s);

	} // setText

	// -------------------------------------------------------------------------
	/**
	 * Set the information TextArea to the contents of a list of Strings.
	 * @param list The List of Strings to display.
	 */
	public void setText(List list) {

		Iterator it = list.iterator();
		while (it.hasNext()) {

			ReportLine line = (ReportLine)it.next();
			infoTextArea.append(line.getMessage());

		}

	} // setText

	// -------------------------------------------------------------------------
	/**
	 * Clear the information TextArea
	 */
	public void clear() {

		infoTextArea.setText("");

	}

	// -----------------------------------------------------------------

	public static void main(String[] args) {

		TestInfoWindow tiw = new TestInfoWindow(new JFrame(), "TestInfoWindow", false);
		tiw.show();

		String text = "";
		for (int i = 0; i < 20; i++) {
			text += "frghuerhtrhghqsadasdadsathruytqrytuirqeuitritwuyqtuiwyrtyweruityrw8nghjafiodshagf\n";
		}
		tiw.setText(text);
	}

}
