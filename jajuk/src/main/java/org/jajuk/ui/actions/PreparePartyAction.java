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
 *  $$Revision: 3156 $$
 */
package org.jajuk.ui.actions;

import java.awt.event.ActionEvent;

import javax.swing.JComponent;

import org.jajuk.base.Playlist;
import org.jajuk.util.Const;
import org.jajuk.util.IconLoader;
import org.jajuk.util.JajukIcons;
import org.jajuk.util.Messages;
import org.jajuk.util.error.JajukException;
import org.jajuk.util.log.Log;

public class PreparePartyAction extends JajukAction {

  private static final long serialVersionUID = 1L;

  PreparePartyAction() {
    super(Messages.getString("AbstractPlaylistEditorView.27"), IconLoader
        .getIcon(JajukIcons.PREPARE_PARTY), true);
    setShortDescription(Messages.getString("AbstractPlaylistEditorView.27"));
  }

  @Override
  public void perform(ActionEvent e) throws JajukException {
    JComponent source = (JComponent) e.getSource();
    Object o = source.getClientProperty(Const.DETAIL_SELECTION);
    try {
      ((Playlist) o).prepareParty();
    } catch (Exception e1) {
      Log.error(e1);
    }
  }
}