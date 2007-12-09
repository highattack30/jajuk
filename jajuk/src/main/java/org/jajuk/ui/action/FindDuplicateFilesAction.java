/*
 *  Jajuk
 *  Copyright (C) 2005 The Jajuk Team
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *  $$Revision$$
 */

package org.jajuk.ui.action;

import org.jajuk.base.File;
import org.jajuk.base.Track;
import org.jajuk.base.TrackManager;
import org.jajuk.util.IconLoader;
import org.jajuk.util.Messages;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JComponent;

import org.jajuk.ui.widgets.DuplicateFilesList;

public class FindDuplicateFilesAction extends ActionBase {

	private static final long serialVersionUID = 1L;

	protected FindDuplicateFilesAction() {
		super(Messages.getString("FindDuplicateFilesAction.2"), IconLoader.ICON_SEARCH, true);
		setShortDescription(Messages.getString("FindDuplicateFilesAction.2"));
	}

	public void perform(ActionEvent evt) throws Exception {
		List<List<File>> duplicateFilesList = new ArrayList<List<File>>();
		for (Track track : TrackManager.getInstance().getTracks()) {
			List<File> trackFileList = track.getFiles();
			if (trackFileList.size() > 1) {
				duplicateFilesList.add(trackFileList);
			}
		}
		if (duplicateFilesList.size() < 1) {
			Messages.showInfoMessage("FindDuplicateFilesAction.0");
		} else {
			JFrame frame = new JFrame("List of Duplicate Files found");
	        
	        //Create and set up the content pane.
	        JComponent newContentPane = new DuplicateFilesList(duplicateFilesList);
	        newContentPane.setOpaque(true); //content panes must be opaque
	        frame.setContentPane(newContentPane);

	        //Display the window.
	        frame.pack();
	        frame.setVisible(true);
							
		}
	}

	private String convertToString(List<File> duplicateFilesList) {
		StringBuilder buffer = new StringBuilder();
		for (File file : duplicateFilesList) {
			buffer.append('\t');
			buffer.append(file.getName());
			buffer.append('\n');
		}
		return buffer.toString();
	}
}
