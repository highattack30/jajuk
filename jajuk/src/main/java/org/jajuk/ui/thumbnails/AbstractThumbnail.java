/*
 *  Jajuk
 *  Copyright (C) 2007 The Jajuk Team
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
 *  $Revision$
 */

package org.jajuk.ui.thumbnails;

import com.vlsolutions.swing.docking.ShadowBorder;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.Timer;
import javax.swing.TransferHandler;

import org.jajuk.base.Item;
import org.jajuk.base.Track;
import org.jajuk.base.TrackManager;
import org.jajuk.ui.actions.ActionManager;
import org.jajuk.ui.actions.JajukAction;
import org.jajuk.ui.actions.JajukActions;
import org.jajuk.ui.views.CoverView;
import org.jajuk.ui.windows.JajukMainWindow;
import org.jajuk.util.Conf;
import org.jajuk.util.Const;
import org.jajuk.util.IconLoader;
import org.jajuk.util.JajukIcons;
import org.jajuk.util.Messages;
import org.jajuk.util.UtilGUI;
import org.jajuk.util.log.Log;

/**
 * Album thumb represented as album cover + (optionally) others text information
 * and some features like dnd, menu item to play, search cover, album popup
 * display...
 */
public abstract class AbstractThumbnail extends JPanel implements ActionListener, Transferable {

  private static final long serialVersionUID = -6396225563540281695L;

  /** Size */
  int size;

  protected JLabel jlIcon;

  private static long lDateLastMove;

  private static Point lastPosition;

  JPopupMenu jmenu;

  JMenuItem jmiPlay;

  JMenuItem jmiPush;

  JMenuItem jmiFrontPush;

  JMenuItem jmiDelete;

  JMenuItem jmiPlayShuffle;

  JMenuItem jmiPlayRepeat;

  JMenuItem jmiGetCovers;

  JMenuItem jmiShowPopup;

  JMenuItem jmiCDDBWizard;

  JMenuItem jmiProperties;

  JMenuItem jmiOpenLastFMSite;

  /** Dragging flag used to disable simple click behavior */
  private static boolean bDragging = false;

  /** Current details dialog */
  private static ThumbnailPopup details;

  private static AbstractThumbnail last;

  private static AbstractThumbnail mouseOverItem = null;

  /** Whether this thumb is used in artist view * */
  private boolean artistView;

  /** Associated file */
  File fCover;

  /** Timer used to launch popup */
  static {
    Timer timerPopup = new Timer(200, new ActionListener() {
      public void actionPerformed(ActionEvent arg0) {
        try {
          // Close popup ASAP when over none catalog item
          if (mouseOverItem == null) {
            if (details != null) {
              details.dispose();
              details = null;
            }
            last = null;
            // display a popup after n seconds only if item changed
          } else if ((System.currentTimeMillis() - lDateLastMove >= 700) && mouseOverItem != last
              && !bDragging) {
            // Store current item
            last = mouseOverItem;
            // Finally display the popup (Leave if user unselected
            // the option "Show catalog popups"
            if (Conf.getBoolean(Const.CONF_SHOW_POPUPS)) {
              mouseOverItem.displayPopup();
            }
          }
          bDragging = false;
        } catch (Exception e) {
          // Make sure not to exit
          Log.error(e);
        }
      }
    });
    timerPopup.start();
  }

  /**
   * Constructor
   * 
   * @param size :
   *          size of the thumbnail
   */
  public AbstractThumbnail(int size) {
    this.size = size;
    setSelected(false);
  }

  protected boolean isArtistView() {
    return this.artistView;
  }

  public void setArtistView(boolean artistBioThumb) {
    this.artistView = artistBioThumb;
  }

  /**
   * display a popup over the catalog item
   */
  public void displayPopup() {
    // close popup if any visible
    if (details != null) {
      details.dispose();
      details = null;
    }
    // don't show details if the contextual popup menu
    // is visible
    if (jmenu.isVisible()) {
      return;
    }
    UtilGUI.waiting();
    String description = getDescription();
    if (description != null) {
      details = new ThumbnailPopup(description, new Rectangle(jlIcon.getLocationOnScreen(),
          new Dimension(jlIcon.getWidth(), jlIcon.getHeight())), true);
      UtilGUI.stopWaiting();
    }
  }

  public abstract void populate();

  /** Return HTML text to display in the popup */
  public abstract String getDescription();

  /**
   * Performs common UI operations for any kind of thumb
   */
  void postPopulate() {
    // Album menu
    jmenu = new JPopupMenu();
    jmiPlay = new JMenuItem(ActionManager.getAction(JajukActions.PLAY_SELECTION));
    jmiPlay.putClientProperty(Const.DETAIL_SELECTION, getItem());
    jmiPush = new JMenuItem(ActionManager.getAction(JajukActions.PUSH_SELECTION));
    jmiPush.putClientProperty(Const.DETAIL_SELECTION, getItem());
    jmiFrontPush = new JMenuItem(ActionManager.getAction(JajukActions.PUSH_FRONT_SELECTION));
    jmiFrontPush.putClientProperty(Const.DETAIL_SELECTION, getItem());
    jmiDelete = new JMenuItem(ActionManager.getAction(JajukActions.DELETE));
    jmiDelete.putClientProperty(Const.DETAIL_SELECTION, getItem());
    jmiPlayShuffle = new JMenuItem(ActionManager.getAction(JajukActions.PLAY_SHUFFLE_SELECTION));
    jmiPlayShuffle.putClientProperty(Const.DETAIL_SELECTION, getItem());
    jmiPlayRepeat = new JMenuItem(ActionManager.getAction(JajukActions.PLAY_REPEAT_SELECTION));
    jmiPlayRepeat.putClientProperty(Const.DETAIL_SELECTION, getItem());
    jmiGetCovers = new JMenuItem(Messages.getString("CatalogView.7"), IconLoader
        .getIcon(JajukIcons.COVER_16X16));
    jmiGetCovers.addActionListener(this);
    jmiShowPopup = new JMenuItem(Messages.getString("CatalogView.20"), IconLoader
        .getIcon(JajukIcons.POPUP));
    jmiShowPopup.addActionListener(this);
    jmiCDDBWizard = new JMenuItem(ActionManager.getAction(JajukActions.CDDB_SELECTION));
    jmiCDDBWizard.putClientProperty(Const.DETAIL_SELECTION, getItem());
    jmiProperties = new JMenuItem(ActionManager.getAction(JajukActions.SHOW_PROPERTIES));
    jmiProperties.putClientProperty(Const.DETAIL_SELECTION, getItem());
    if (Desktop.isDesktopSupported()) {
      JajukAction actionOpenLastFM = ActionManager.getAction(JajukActions.LAUNCH_IN_BROWSER);
      // Change action label
      jmiOpenLastFMSite = new JMenuItem(actionOpenLastFM);
      jmiOpenLastFMSite.setText(Messages.getString("AbstractThumbnail.0"));
      jmiOpenLastFMSite.setToolTipText(Messages.getString("AbstractThumbnail.0"));
    }
    // We add all menu items, each implementation of this class should hide
    // (setVisible(false)) menu items that are not available in their
    // context
    jmenu.add(jmiPlay);
    jmenu.add(jmiFrontPush);
    jmenu.add(jmiPush);
    jmenu.add(jmiPlayShuffle);
    jmenu.add(jmiPlayRepeat);
    jmenu.addSeparator();
    jmenu.add(jmiDelete);
    jmenu.addSeparator();
    jmenu.add(jmiCDDBWizard);
    jmenu.add(jmiGetCovers);
    jmenu.add(jmiShowPopup);
    if (Desktop.isDesktopSupported()) {
      jmenu.add(jmiOpenLastFMSite);
    }
    jmenu.addSeparator();
    jmenu.add(jmiProperties);

    jlIcon.addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseDragged(MouseEvent e) {
        // Notify the mouse listener that we are dragging
        bDragging = true;
        JComponent c = (JComponent) e.getSource();
        TransferHandler handler = c.getTransferHandler();
        handler.exportAsDrag(c, e, TransferHandler.COPY);
      }

      @Override
      public void mouseMoved(MouseEvent e) {
        super.mouseMoved(e);
        lDateLastMove = System.currentTimeMillis();
        lastPosition = e.getPoint();
      }

    });

    jlIcon.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
          handlePopup(e);
        } else if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) == 0) {
          // Leave if already dragging
          if (bDragging) {
            return;
          }
          // Left click
          if (e.getButton() == MouseEvent.BUTTON1 && e.getSource() == jlIcon
              && e.getClickCount() > 1) {
            launch();
          }
        }
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        mouseOverItem = AbstractThumbnail.this;
      }

      @Override
      public void mouseExited(MouseEvent e) {
        // Consider an exit only if mouse really moved to avoid
        // closing popup when popup appears over the mouse cursor
        // (then, a mouseExit event is thrown)
        if (!e.getPoint().equals(lastPosition) &&
        // Don't close popup if user is still over it
            !(details != null && details.contains(e.getPoint()))) {
          mouseOverItem = null;
        }
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        // Leave if already dragging
        if (bDragging) {
          return;
        }
        if (e.isPopupTrigger()) {
          handlePopup(e);
        }
      }

      public void handlePopup(final MouseEvent e) {
        if (e.getSource() == jlIcon) {
          // Show contextual menu
          jmenu.show(jlIcon, e.getX(), e.getY());
          // Hide any details frame
          if (details != null) {
            details.dispose();
            details = null;
          }
        }
      }

    });
  }

  /**
   * 
   * @param b
   */
  public final void setSelected(boolean b) {
    requestFocusInWindow();
    // Add a shadow for selected items
    if (b) {
      setBorder(new ShadowBorder(false));
    } else {
      // add an empty border of the same size than the border to avoid
      // image moves when setting borders
      setBorder(BorderFactory.createEmptyBorder(1, 1, 5, 5));
    }
  }

  public abstract void launch();

  /**
   * If the thumb represents something (album, author...) known in the
   * collection, the implementation of this method should return the associated
   * item
   * 
   * @return the collection item
   */
  public abstract Item getItem();

  /*
   * (non-Javadoc)
   * 
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == jmiGetCovers) {
      // This item is enabled only for albums
      new Thread("Thumbnail Action Thread") {
        @Override
        public void run() {
          JDialog jd = new JDialog(JajukMainWindow.getInstance(), Messages
              .getString("CatalogView.18"));
          org.jajuk.base.File file = null;
          List<Track> tracks = TrackManager.getInstance().getAssociatedTracks(getItem(), false);
          if (tracks.size() > 0) {
            // Take first track found
            Track track = tracks.iterator().next();
            file = track.getPlayeableFile(false);
          }
          CoverView cv = null;
          if (file != null) {
            cv = new CoverView(file);
            cv.setID("catalog/0");
            cv.initUI();
            jd.add(cv);
            jd.setSize(600, 450);
            jd.setLocationByPlatform(true);
            jd.setVisible(true);
          } else {
            Messages.showErrorMessage(166);
          }
        }
      }.start();
    } else if (e.getSource() == jmiShowPopup) {
      this.displayPopup();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.awt.datatransfer.Transferable#getTransferData(java.awt.datatransfer.DataFlavor)
   */
  public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.awt.datatransfer.Transferable#getTransferDataFlavors()
   */
  public DataFlavor[] getTransferDataFlavors() {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.awt.datatransfer.Transferable#isDataFlavorSupported(java.awt.datatransfer.DataFlavor)
   */
  public boolean isDataFlavorSupported(DataFlavor flavor) {
    return false;
  }

  public JLabel getIcon() {
    return this.jlIcon;
  }

}
