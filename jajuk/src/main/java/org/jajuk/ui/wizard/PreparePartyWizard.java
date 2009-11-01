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
 *  $$Revision: 5405 $$
 */
package org.jajuk.ui.wizard;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jajuk.base.FileManager;
import org.jajuk.base.Playlist;
import org.jajuk.base.PlaylistManager;
import org.jajuk.base.Type;
import org.jajuk.base.TypeManager;
import org.jajuk.events.JajukEvent;
import org.jajuk.events.JajukEvents;
import org.jajuk.events.ObservationManager;
import org.jajuk.services.dj.Ambience;
import org.jajuk.services.dj.AmbienceManager;
import org.jajuk.services.dj.DigitalDJ;
import org.jajuk.services.dj.DigitalDJManager;
import org.jajuk.ui.helpers.DefaultMouseWheelListener;
import org.jajuk.ui.widgets.JajukFileChooser;
import org.jajuk.ui.windows.JajukMainWindow;
import org.jajuk.util.Const;
import org.jajuk.util.IconLoader;
import org.jajuk.util.JajukFileFilter;
import org.jajuk.util.JajukIcons;
import org.jajuk.util.LocaleManager;
import org.jajuk.util.Messages;
import org.jajuk.util.UtilGUI;
import org.jajuk.util.error.JajukException;
import org.jajuk.util.filters.DirectoryFilter;
import org.jajuk.util.log.Log;
import org.qdwizard.ClearPoint;
import org.qdwizard.Screen;
import org.qdwizard.Wizard;

/**
 * DJ creation wizard
 */
public class PreparePartyWizard extends Wizard {

  /** Which source to use for the tracks */
  private static final String KEY_MODE = "MODE";

  /* Which item was selected in the first page of the wizard */
  private static final String KEY_ITEM = "ITEM";

  /* Where to put the files */
  private static final String KEY_DEST_PATH = "DEST_PATH";

  /** Max number of tracks to use */
  private static final String KEY_MAX_TRACKS_ON = "MAXTRACKS_ENABLED";
  private static final String KEY_MAX_TRACKS = "MAXTRACKS";

  /** Max size to use */
  private static final String KEY_MAX_SIZE_ON = "MAXSIZE_ENABLED";
  private static final String KEY_MAX_SIZE = "MAXSIZE";

  /** Max playing length of tracks to use */
  private static final String KEY_MAX_LENGTH_ON = "MAXLENGTH_ENABLED";
  private static final String KEY_MAX_LENGTH = "MAXLENGTH";

  /** Max number of tracks to queue */
  private static final String KEY_ONE_MEDIA_ON = "ONE_MEDIA_ENABLED";
  private static final String KEY_MEDIA = "ONE_MEDIA";

  /** Ratings level */
  private static final String KEY_RATINGS_LEVEL = "RATING_LEVEL";

  // store a temporary playlist that is provided by the PlaylistView without storing it in the PlaylistManager
  // we keep it here to be able to re-display it in the Playlist view later on
  // We need to keep it outside the ActionSelectionPanel because the panel is re-created during back-forward operations
  private static Playlist tempPlaylist;
  
  private enum Mode {
    DJ, Ambience, Shuffle, Playlist, ProvidedPlaylist
  }

  /*
   * Default constructor that lets the user choose where the tracks are taken
   * from
   */
  public PreparePartyWizard(boolean bProvidedPlaylist) {
    super(Messages.getString("PreparePartyWizard.1"), bProvidedPlaylist ? GeneralOptionsPanel.class : ActionSelectionPanel.class, null,
        JajukMainWindow.getInstance(), LocaleManager.getLocale(), 800, 500);
    super.setHeaderIcon(IconLoader.getIcon(JajukIcons.PREPARE_PARTY));
  }

  public static void setPlaylist(Playlist playlist) {
    // store playlist and the mode that we are now having
    tempPlaylist = playlist;
    data.put(KEY_ITEM, playlist.getName());
    data.put(KEY_MODE, Mode.ProvidedPlaylist);
  }

  /**
   * Return if the specified element is true in the data-map
   * 
   * @return true if the value was stored as boolean true, false otherwise.
   */
  private static final boolean isTrue(final String key) {
    return data.containsKey(key) && Boolean.TRUE.equals(data.get(key));
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.qdwizard.Wizard#finish()
   */
  @Override
  public void finish() {
    // retrieve the full list of files according to the selected mode
    List<org.jajuk.base.File> files;
    if (Mode.DJ.equals(data.get(KEY_MODE))) {
      files = getDJFiles((String) data.get(KEY_ITEM));
    } else if (Mode.Ambience.equals(data.get(KEY_MODE))) {
      files = getAmbienceFiles((String) data.get(KEY_ITEM));
    } else if (Mode.Playlist.equals(data.get(KEY_MODE))
        || Mode.ProvidedPlaylist.equals(data.get(KEY_MODE))) {
      try {
        // for Playlists we need to store the actual Playlist as it could be a temporary one from 
        // the PlaylistView, so a lookup might not work here
        files = getPlaylistFiles((String) data.get(KEY_ITEM));
      } catch (Exception e1) {
        Log.error(e1);
        return;
      }
    } else if (Mode.Shuffle.equals(data.get(KEY_MODE))) {
      files = getShuffleFiles();
    } else {
      throw new IllegalArgumentException("Unknown mode in PreparePartyWizard: "
          + data.get(KEY_MODE));
    }

    // filter by media first
    if (isTrue(KEY_ONE_MEDIA_ON)) {
      files = filterMedia(files, (String) data.get(KEY_MEDIA));
    }

    // then filter out by rating
    if (data.containsKey(KEY_RATINGS_LEVEL)) {
      files = filterRating(files, (Integer) data.get(KEY_RATINGS_LEVEL));
    }

    // filter max length
    if (isTrue(KEY_MAX_LENGTH_ON)) {
      files = filterMaxLength(files, (Integer) data.get(KEY_MAX_LENGTH));
    }

    // filter max size
    if (isTrue(KEY_MAX_SIZE_ON)) {
      files = filterMaxSize(files, (Integer) data.get(KEY_MAX_SIZE));
    }

    // filter max tracks
    if (isTrue(KEY_MAX_TRACKS_ON)) {
      files = filterMaxTracks(files, (Integer) data.get(KEY_MAX_TRACKS));
    }

    final File fDir = (File) data.get(KEY_DEST_PATH);

    // TODO: create progress dialog, this did not work for some reason
//    JDialog progress = new JDialog(getDialog());
//    progress.setLayout(new MigLayout("insets 10,gapx 10,gapy 15"));
//    JLabel label = new JLabel();
//    progress.add(label, "grow,wrap");
//    JProgressBar bar = new JProgressBar(0, files.size());
//    progress.add(bar, "grow,wrap");
//    progress.pack();
//    progress.setVisible(true);

    final Date curDate = new Date();
    // Do not use ':' character in destination directory, it's
    // forbidden under Windows
    final SimpleDateFormat stamp = new SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault());
    final String dirName = "Party-" + stamp.format(curDate);
    final java.io.File destDir = new java.io.File(fDir.getAbsolutePath() + "/" + dirName);
    if (!destDir.mkdir()) {
      Log.warn("Could not create destination directory " + destDir);
    }

    Log.debug("Going to copy " + files.size() + " files to directory {{" + destDir.getAbsolutePath() + "}}");

    UtilGUI.waiting();
    final java.io.File file = new java.io.File(destDir.getAbsolutePath() + "/playlist.m3u");
    try {
      final BufferedWriter bw = new BufferedWriter(new FileWriter(file));
      try {
        bw.write(Const.PLAYLIST_NOTE);
        int count = 0;
        for (final org.jajuk.base.File entry : files) {
          // update progress
          count++;
//          bar.setValue(count);
//          label.setText(entry.getName());

          // We can use the actual file name as we do numbering of the files, 
          // this is important for existing playlists to keep the order
          String name = StringUtils.leftPad(new Integer(
              count).toString(), 5, '0') + '_' + entry.getFIO().getName();
          FileUtils.copyFile(entry.getFIO(), new File(destDir, name));
          //  + '.' + entry.getType().getExtension()

          bw.newLine();
          bw.write(name);

          // Notify that a file has been copied
          Properties properties = new Properties();
          properties.put(Const.DETAIL_CONTENT, entry.getName());
          ObservationManager.notify(new JajukEvent(JajukEvents.FILE_COPIED, properties));
        }

        bw.flush();
      } finally {
        bw.close();
      }
      // Send a last event with null properties to inform the
      // client that the party is done
      ObservationManager.notify(new JajukEvent(JajukEvents.FILE_COPIED));

    } catch (final IOException e) {
      Log.error(e);
      Messages.showErrorMessage(180, e.getMessage());
      return;
    } finally {
      //progress.dispose();
      UtilGUI.stopWaiting();
    }
    Messages.showInfoMessage(dirName + " " + Messages.getString("AbstractPlaylistEditorView.28")
        + " " + fDir.getAbsolutePath() + ".\n" + files.size()
        + Messages.getString("PreparePartyWizard.23"));
  }

  /**
   * @param files
   * @param integer
   * @return
   */
  private List<org.jajuk.base.File> filterRating(List<org.jajuk.base.File> files, Integer rate) {
    final List<org.jajuk.base.File> newFiles = new ArrayList<org.jajuk.base.File>();
    for (org.jajuk.base.File file : files) {
      // only add files that have a rate equal or higher than the level set
      if (file.getTrack().getStarsNumber() >= rate) {
        newFiles.add(file);
      }
    }

    return newFiles;
  }

  /**
   * @param files
   * @param integer
   * @return
   */
  private List<org.jajuk.base.File> filterMaxLength(List<org.jajuk.base.File> files, Integer time) {
    final List<org.jajuk.base.File> newFiles = new ArrayList<org.jajuk.base.File>();
    long accumulated = 0;
    for (org.jajuk.base.File file : files) {
      // check if we now exceed the max length, getDuration() is in seconds, but
      // we want to use minutes
      if ((accumulated + file.getTrack().getDuration()) / 60 > time) {
        return newFiles;
      }

      accumulated += file.getTrack().getDuration();
      newFiles.add(file);
    }

    // there were not enough files to reach the limit, return the full list
    return files;
  }

  /**
   * @param files
   * @param integer
   * @return
   */
  private List<org.jajuk.base.File> filterMaxSize(List<org.jajuk.base.File> files, Integer size) {
    final List<org.jajuk.base.File> newFiles = new ArrayList<org.jajuk.base.File>();
    long accumulated = 0;
    for (org.jajuk.base.File file : files) {
      // check if we now exceed the max size, getSize() is in byte, but we want
      // to use MB
      if ((accumulated + file.getSize()) / (1024 * 1024) > size) {
        return newFiles;
      }

      accumulated += file.getSize();
      newFiles.add(file);
    }

    // there were not enough files to reach the limit, return the full list
    return files;
  }

  /**
   * @param files
   * @param integer
   * @return
   */
  private List<org.jajuk.base.File> filterMaxTracks(List<org.jajuk.base.File> files, Integer tracks) {
    final List<org.jajuk.base.File> newFiles = new ArrayList<org.jajuk.base.File>();
    int count = 0;
    for (org.jajuk.base.File file : files) {
      // check if we have reached the max
      if (count > tracks) {
        return newFiles;
      }

      count++;
      newFiles.add(file);
    }

    // there were not enough files to reach the limit, return the full list
    return files;
  }

  /**
   * @param files
   * @return
   */
  private List<org.jajuk.base.File> filterMedia(final List<org.jajuk.base.File> files,
      final String ext) {
    final List<org.jajuk.base.File> newFiles = new ArrayList<org.jajuk.base.File>();
    for (org.jajuk.base.File file : files) {
      if (file.getType().getExtension().equals(ext)) {
        newFiles.add(file);
      }
    }

    return newFiles;
  }

  private List<org.jajuk.base.File> getDJFiles(final String name) {
    DigitalDJ dj = DigitalDJManager.getInstance().getDJByName(name);
    return dj.generatePlaylist();
  }

  private List<org.jajuk.base.File> getAmbienceFiles(String name) {
    final List<org.jajuk.base.File> files;
    Ambience ambience = AmbienceManager.getInstance().getAmbienceByName(name);

    files = new ArrayList<org.jajuk.base.File>();
    // Get a shuffle selection
    List<org.jajuk.base.File> allFiles = FileManager.getInstance().getGlobalShufflePlaylist();
    // Keep only right styles and check for unicity
    for (org.jajuk.base.File file : allFiles) {
      if (ambience.getStyles().contains(file.getTrack().getStyle())) {
        files.add(file);
      }
    }
    return files;
  }

  private List<org.jajuk.base.File> getPlaylistFiles(String name) throws JajukException {
    // if we chose the temp-playlist, use this one
    if(tempPlaylist != null &&  name.equals(tempPlaylist.getName())) {
      return tempPlaylist.getFiles();
    }

    // get the Playlist from the Manager by name
    Playlist playlist = PlaylistManager.getInstance().getPlaylistByName(name);
    return playlist.getFiles();
  }

  private List<org.jajuk.base.File> getShuffleFiles() {
    // Get a shuffle selection from all files
    List<org.jajuk.base.File> allFiles = FileManager.getInstance().getGlobalShufflePlaylist();

    return allFiles;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.qdwizard.Wizard#getNextScreen(java.lang.Class)
   */
  @Override
  public Class<? extends Screen> getNextScreen(Class<? extends Screen> screen) {
    if (ActionSelectionPanel.class.equals(getCurrentScreen())) {
      return GeneralOptionsPanel.class;
    } else if (GeneralOptionsPanel.class.equals(getCurrentScreen())) {
      return PathSelectionPanel.class;
    }

    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.qdwizard.Wizard#getPreviousScreen(java.lang.Class)
   */
  @Override
  public Class<? extends Screen> getPreviousScreen(Class<? extends Screen> screen) {
    // there is no "back" if we got a playlist passed in
    if (GeneralOptionsPanel.class.equals(getCurrentScreen())
        && !Mode.ProvidedPlaylist.equals(data.get(KEY_MODE))) {
      return ActionSelectionPanel.class;
    } else if (PathSelectionPanel.class.equals(getCurrentScreen())) {
      return GeneralOptionsPanel.class;
    }

    return null;
  }

  public static class ActionSelectionPanel extends Screen implements ActionListener, ClearPoint {

    private static final long serialVersionUID = -6981770030816500259L;

    private ButtonGroup bgActions;

    private JRadioButton jrbDJ;
    private JComboBox jcbDJ;

    private JRadioButton jrbAmbience;
    private JComboBox jcbAmbience;

    private JRadioButton jrbPlaylist;
    private JComboBox jcbPlaylist;

    private JRadioButton jrbShuffle;
    
    /**
     * Create panel UI
     */

    @Override
    public void initUI() {
      bgActions = new ButtonGroup();

      jrbDJ = new JRadioButton(Messages.getString("PreparePartyWizard.6"));
      jrbDJ.addActionListener(this);

      // populate DJs
      List<DigitalDJ> djs = DigitalDJManager.getInstance().getDJsSorted();
      jcbDJ = new JComboBox();
      for (DigitalDJ dj : djs) {
        jcbDJ.addItem(dj.getName());
      }
      jcbDJ.addActionListener(this);

      jrbAmbience = new JRadioButton(Messages.getString("PreparePartyWizard.7"));
      jrbAmbience.addActionListener(this);

      List<Ambience> ambiences = AmbienceManager.getInstance().getAmbiences();
      jcbAmbience = new JComboBox();
      for (Ambience amb : ambiences) {
        jcbAmbience.addItem(amb.getName());
      }
      jcbAmbience.addActionListener(this);

      jrbPlaylist = new JRadioButton(Messages.getString("PreparePartyWizard.8"));
      jrbPlaylist.addActionListener(this);

      jcbPlaylist = new JComboBox();

      if(tempPlaylist != null) {
        // check if this is a "temporary" playlist that is provided by the PlaylistView (i.e. not yet stored in PlaylistManager)
          jcbPlaylist.addItem(tempPlaylist.getName());
      }
      
      List<Playlist> playlists = PlaylistManager.getInstance().getPlaylists();
      for (Playlist pl : playlists) {
        jcbPlaylist.addItem(pl.getName());
      }
      jcbPlaylist.addActionListener(this);

      jrbShuffle = new JRadioButton(Messages.getString("PreparePartyWizard.9"));
      jrbShuffle.addActionListener(this);

      bgActions.add(jrbDJ);
      bgActions.add(jrbAmbience);
      bgActions.add(jrbPlaylist);
      bgActions.add(jrbShuffle);

      readData();

      // populate the screen
      setLayout(new MigLayout("insets 10,gapx 10,gapy 15", "[][grow]"));
      add(jrbDJ, "left");
      add(jcbDJ, "grow,wrap");
      add(jrbAmbience, "left");
      add(jcbAmbience, "grow,wrap");
      add(jrbPlaylist, "left");
      add(jcbPlaylist, "grow,wrap");
      add(jrbShuffle, "left");

      // store initial values
      updateData();
    }

    private void readData() {
      if (data.containsKey(KEY_MODE)) {
        // read values set before
        switch ((Mode) data.get(KEY_MODE)) {
        case DJ:
          bgActions.setSelected(jrbDJ.getModel(), true);
          jcbDJ.setSelectedItem(data.get(KEY_ITEM));
          break;

        case Ambience:
          bgActions.setSelected(jrbAmbience.getModel(), true);
          jcbAmbience.setSelectedItem(data.get(KEY_ITEM));
          break;

        case Playlist:
        case ProvidedPlaylist: // we did a "PrepareParty" from a Playlist
                               // before, in this case show the Playlist again
                               // here
          bgActions.setSelected(jrbPlaylist.getModel(), true);
          jcbPlaylist.setSelectedItem((data.get(KEY_ITEM)));
          break;
          
        case Shuffle:
          bgActions.setSelected(jrbShuffle.getModel(), true);
          // no combo box for shuffle...
          break;

        default:
          throw new IllegalArgumentException("Unexpected value in switch!");
        }
      } else {
        // no values set yet, select a useful radio button at least

        // disabled DJ and select Ambience if there is no DJ
        if (jcbDJ.getItemCount() == 0) {
          jrbDJ.setEnabled(false);
          jcbDJ.setEnabled(false);
          bgActions.setSelected(jrbAmbience.getModel(), true);
        } else {
          // otherwise select DJ as default option
          bgActions.setSelected(jrbDJ.getModel(), true);
        }

        // disable Playlist UI if there is no Playlist
        if (jcbPlaylist.getItemCount() == 0) {
          jrbPlaylist.setEnabled(false);
          jcbPlaylist.setEnabled(false);
        }
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      if (e.getSource().equals(jcbDJ)) {
        bgActions.setSelected(jrbDJ.getModel(), true);
      } else if (e.getSource().equals(jcbAmbience)) {
        bgActions.setSelected(jrbAmbience.getModel(), true);
      } else if (e.getSource().equals(jcbPlaylist)) {
        bgActions.setSelected(jrbPlaylist.getModel(), true);
      }

      // now update all the values that are needed later
      updateData();
    }

    /**
     * 
     */
    private void updateData() {
      // depending on the selected radio button read the combo box value and set
      // the selected MODE
      if (jrbDJ.isSelected()) {
        data.put(KEY_MODE, Mode.DJ);
        data.put(KEY_ITEM, jcbDJ.getSelectedItem());
      } else if (jrbAmbience.isSelected()) {
        data.put(KEY_MODE, Mode.Ambience);
        data.put(KEY_ITEM, jcbAmbience.getSelectedItem());
      } else if (jrbPlaylist.isSelected()) {
        data.put(KEY_MODE, Mode.Playlist);
        data.put(KEY_ITEM, jcbPlaylist.getSelectedItem());
      } else if (jrbShuffle.isSelected()) {
        data.put(KEY_MODE, Mode.Shuffle);
        data.remove(KEY_ITEM);
      }
    }

    @Override
    public String getDescription() {
      return Messages.getString("PreparePartyWizard.3");
    }

    @Override
    public String getName() {
      return Messages.getString("PreparePartyWizard.2");
    }

  }

  /**
   * 
   * General options panel
   */
  public static class GeneralOptionsPanel extends Screen implements ActionListener, ChangeListener,
      ClearPoint {

    private static final long serialVersionUID = 1L;

    private static final String NO_VALUE = " ";

    // Max. number of tracks to include
    private JCheckBox jcbMaxTracks;
    private JSlider jsMaxTracks;
    private JLabel jnMaxTracks;

    // Max size (in MB) of party
    private JCheckBox jcbMaxSize;
    private JSlider jsMaxSize;
    private JLabel jnMaxSize;

    // Max playing length of party
    private JCheckBox jcbMaxLength;
    private JSlider jsMaxLength;
    private JLabel jnMaxLength;

    // Limit to one type of media
    private JCheckBox jcbOneMedia;
    private JComboBox jcbMedia;

    private JLabel jlRatingLevel;
    private JSlider jsRatingLevel;

    @Override
    public String getDescription() {
      return Messages.getString("PreparePartyWizard.5");
    }

    @Override
    public String getName() {
      return Messages.getString("PreparePartyWizard.4");
    }

    /**
     * Create panel UI
     */

    @Override
    public void initUI() {
      { // Max Tracks
        jcbMaxTracks = new JCheckBox(Messages.getString("PreparePartyWizard.10"));
        jcbMaxTracks.setToolTipText(Messages.getString("PreparePartyWizard.11"));

        jsMaxTracks = new JSlider(0, 5000, 100);
        jnMaxTracks = new JLabel(NO_VALUE);
        jnMaxTracks.setBorder(new BevelBorder(BevelBorder.LOWERED));
        jsMaxTracks.setMajorTickSpacing(100);
        jsMaxTracks.setMinorTickSpacing(10);
        jsMaxTracks.setPaintTicks(false);
        jsMaxTracks.setPaintLabels(false);
        jsMaxTracks.setToolTipText(Messages.getString("PreparePartyWizard.11"));
      }

      { // Max Size
        jcbMaxSize = new JCheckBox(Messages.getString("PreparePartyWizard.12"));
        jcbMaxSize.setToolTipText(Messages.getString("PreparePartyWizard.13"));

        jsMaxSize = new JSlider(0, 5000, 100);
        jnMaxSize = new JLabel(NO_VALUE);
        jnMaxSize.setBorder(new BevelBorder(BevelBorder.LOWERED));
        jsMaxSize.setMajorTickSpacing(100);
        jsMaxSize.setMinorTickSpacing(10);
        jsMaxSize.setPaintTicks(false);
        jsMaxSize.setPaintLabels(false);
        jsMaxSize.setToolTipText(Messages.getString("PreparePartyWizard.13"));
      }

      { // Max Length
        jcbMaxLength = new JCheckBox(Messages.getString("PreparePartyWizard.14"));
        jcbMaxLength.setToolTipText(Messages.getString("PreparePartyWizard.15"));

        jsMaxLength = new JSlider(0, 5000, 100);
        jnMaxLength = new JLabel(NO_VALUE);
        jnMaxLength.setBorder(new BevelBorder(BevelBorder.LOWERED));
        jsMaxLength.setMajorTickSpacing(100);
        jsMaxLength.setMinorTickSpacing(10);
        jsMaxLength.setPaintTicks(false);
        jsMaxLength.setPaintLabels(false);
        jsMaxLength.setToolTipText(Messages.getString("PreparePartyWizard.15"));
      }

      { // Choose Media
        jcbOneMedia = new JCheckBox(Messages.getString("PreparePartyWizard.16"));
        jcbOneMedia.setToolTipText(Messages.getString("PreparePartyWizard.17"));

        jcbMedia = new JComboBox();
        List<Type> types = TypeManager.getInstance().getTypes();
        // sort the list on extension here
        Collections.sort(types, new Comparator<Type>() {
          @Override
          public int compare(Type o1, Type o2) {
            // handle null, always equal
            if (o1 == null || o2 == null) {
              return 0;
            }

            // otherwise sort on extension here
            return o1.getExtension().compareTo(o2.getExtension());
          }
        });
        for (Type type : types) {
          jcbMedia.addItem(type.getExtension());
        }
        jcbMedia.setToolTipText(Messages.getString("PreparePartyWizard.17"));
      }

      jlRatingLevel = new JLabel(Messages.getString("DigitalDJWizard.8"));
      jlRatingLevel.setToolTipText(Messages.getString("DigitalDJWizard.53"));
      jsRatingLevel = new JSlider(0, 4, 0);
      jsRatingLevel.setMajorTickSpacing(1);
      jsRatingLevel.setMinorTickSpacing(1);
      jsRatingLevel.setPaintTicks(true);
      jsRatingLevel.setSnapToTicks(true);
      jsRatingLevel.setPaintLabels(true);
      jsRatingLevel.setToolTipText(Messages.getString("DigitalDJWizard.53"));
      jsRatingLevel.addMouseWheelListener(new DefaultMouseWheelListener(jsRatingLevel));
      jsRatingLevel.addChangeListener(this);

      readData();

      // add listeners after reading initial data to not overwrite with
      // init-state-change actions

      // enable/disable slider depending on checkbox
      jcbMaxTracks.addActionListener(this);
      jsMaxTracks.addMouseWheelListener(new DefaultMouseWheelListener(jsMaxTracks));
      jsMaxTracks.addChangeListener(this);

      // enable/disable slider depending on checkbox
      jcbMaxSize.addActionListener(this);
      jsMaxSize.addMouseWheelListener(new DefaultMouseWheelListener(jsMaxSize));
      jsMaxSize.addChangeListener(this);

      // enable/disable slider depending on checkbox
      jcbMaxLength.addActionListener(this);
      jsMaxLength.addMouseWheelListener(new DefaultMouseWheelListener(jsMaxLength));
      jsMaxLength.addChangeListener(this);

      // enable/disable combobox depending on checkbox
      jcbOneMedia.addActionListener(this);
      jcbMedia.addActionListener(this);

      setLayout(new MigLayout("insets 10,gapx 10,gapy 15", "[][grow]"));
      add(jcbMaxTracks);
      {
        JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("", "[grow][]"));
        panel.add(jsMaxTracks, "grow");
        panel.add(jnMaxTracks);
        add(panel, "grow,wrap");
      }
      add(jcbMaxSize);
      {
        JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("", "[grow][]"));
        panel.add(jsMaxSize, "grow");
        panel.add(jnMaxSize);
        add(panel, "grow,wrap");
      }
      add(jcbMaxLength);
      {
        JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("", "[grow][]"));
        panel.add(jsMaxLength, "grow");
        panel.add(jnMaxLength);
        add(panel, "grow,wrap");
      }
      add(jcbOneMedia);
      add(jcbMedia, "grow,wrap");
      add(jlRatingLevel);
      add(jsRatingLevel, "grow,wrap");

      // store initial values and adjust values
      updateData();
    }

    /**
     * 
     */
    private void readData() {
      // set the values from the stored data
      // initially these are not set, so we need to query for "containsKey"...

      if (isTrue(KEY_MAX_TRACKS_ON)) {
        jsMaxTracks.setEnabled(true);
        jcbMaxTracks.setSelected(true);
      } else {
        jsMaxTracks.setEnabled(false);
        jcbMaxTracks.setSelected(false);
      }
      if (data.containsKey(KEY_MAX_TRACKS)) {
        jsMaxTracks.setValue((Integer) data.get(KEY_MAX_TRACKS));
      }

      if (data.containsKey(KEY_MAX_SIZE_ON) && Boolean.TRUE.equals(data.get(KEY_MAX_SIZE_ON))) {
        jsMaxSize.setEnabled(true);
        jcbMaxSize.setSelected(true);
      } else {
        jsMaxSize.setEnabled(false);
        jcbMaxSize.setSelected(false);
      }
      if (data.containsKey(KEY_MAX_SIZE)) {
        jsMaxSize.setValue((Integer) data.get(KEY_MAX_SIZE));
      }

      if (data.containsKey(KEY_MAX_LENGTH_ON) && Boolean.TRUE.equals(data.get(KEY_MAX_LENGTH_ON))) {
        jsMaxLength.setEnabled(true);
        jcbMaxLength.setSelected(true);
      } else {
        jsMaxLength.setEnabled(false);
        jcbMaxLength.setSelected(false);
      }
      if (data.containsKey(KEY_MAX_LENGTH)) {
        jsMaxLength.setValue((Integer) data.get(KEY_MAX_LENGTH));
      }

      if (data.containsKey(KEY_ONE_MEDIA_ON) && Boolean.TRUE.equals(data.get(KEY_ONE_MEDIA_ON))) {
        jcbMedia.setEnabled(true);
        jcbOneMedia.setSelected(true);
      } else {
        jcbMedia.setEnabled(false);
        jcbOneMedia.setSelected(false);
      }
      if (data.containsKey(KEY_MEDIA)) {
        jcbMedia.setSelectedItem(data.get(KEY_MEDIA));
      } else {
        // default to MP3 initially
        jcbMedia.setSelectedItem("mp3");
      }

      if (data.containsKey(KEY_RATINGS_LEVEL)) {
        jsRatingLevel.setValue((Integer) data.get(KEY_RATINGS_LEVEL));
      }
    }

    /**
     * 
     */
    private void updateData() {
      // store if checkbox is enabled and update the label accordingly
      updateOneItem(jcbMaxTracks, jsMaxTracks, jnMaxTracks, KEY_MAX_TRACKS, KEY_MAX_TRACKS_ON);
      updateOneItem(jcbMaxSize, jsMaxSize, jnMaxSize, KEY_MAX_SIZE, KEY_MAX_SIZE_ON);
      updateOneItem(jcbMaxLength, jsMaxLength, jnMaxLength, KEY_MAX_LENGTH, KEY_MAX_LENGTH_ON);

      if (jcbOneMedia.isSelected()) {
        data.put(KEY_MEDIA, jcbMedia.getSelectedItem());
        data.put(KEY_ONE_MEDIA_ON, Boolean.TRUE);
      } else {
        // keep old value... data.remove(KEY_MEDIA);
        data.put(KEY_ONE_MEDIA_ON, Boolean.FALSE);
      }

      data.put(KEY_RATINGS_LEVEL, jsRatingLevel.getValue());
    }

    private void updateOneItem(JCheckBox cb, JSlider slider, JLabel label, String key, String keyOn) {
      if (cb.isSelected()) {
        if (!slider.getValueIsAdjusting()) {
          data.put(key, slider.getValue());
          data.put(keyOn, Boolean.TRUE);
        }
        label.setText(Integer.toString(slider.getValue()));
      } else {
        if (!slider.getValueIsAdjusting()) {
          // keep value... data.remove(key);
          data.put(keyOn, Boolean.FALSE);
        }
        label.setText(NO_VALUE);
      }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */

    public void actionPerformed(ActionEvent ae) {
      // if a checkbox is pressed, adjust the sliders/comboboxes

      if (ae.getSource() == jcbMaxTracks) {
        jsMaxTracks.setEnabled(jcbMaxTracks.isSelected());
      }

      if (ae.getSource() == jcbMaxSize) {
        jsMaxSize.setEnabled(jcbMaxSize.isSelected());
      }

      if (ae.getSource() == jcbMaxLength) {
        jsMaxLength.setEnabled(jcbMaxLength.isSelected());
      }

      if (ae.getSource() == jcbOneMedia) {
        jcbMedia.setEnabled(jcbOneMedia.isSelected());
      }

      updateData();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent
     * )
     */

    /*
     * (non-Javadoc)
     * 
     * @see
     * javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent
     * )
     */
    public void stateChanged(ChangeEvent ie) {
      if (ie.getSource() == jsMaxTracks) {
        updateData();
      } else if (ie.getSource() == jsMaxSize) {
        updateData();
      } else if (ie.getSource() == jsMaxLength) {
        updateData();
      } else if (ie.getSource() == jcbMedia) {
        updateData();
      } else if (ie.getSource() == jsRatingLevel) {
        updateData();
      }
    }
  }

  public static class PathSelectionPanel extends Screen implements ActionListener {
    private static final long serialVersionUID = -236180699495019177L;

    JButton jbFileSelection;

    JLabel jlSelectedFile;

    JPanel jpMain;

    /** Selected directory */
    private File fDir;

    /*
     * (non-Javadoc)
     * 
     * @see org.qdwizard.Screen#initUI()
     */
    @Override
    public void initUI() {
      JLabel jlFileSelection = new JLabel(Messages.getString("FirstTimeWizard.2"));
      jbFileSelection = new JButton(IconLoader.getIcon(JajukIcons.OPEN_DIR));
      jbFileSelection.addActionListener(this);

      JLabel jlSelectedFileText = new JLabel(Messages.getString("FirstTimeWizard.8"));
      jlSelectedFile = new JLabel(Messages.getString("FirstTimeWizard.9"));
      jlSelectedFile.setBorder(new BevelBorder(BevelBorder.LOWERED));

      // previous value if available
      if (data.containsKey(KEY_DEST_PATH)) {
        jlSelectedFile.setText(((File) data.get(KEY_DEST_PATH)).getAbsolutePath());

        // we also can finish the dialog
        setCanFinish(true);
      } else {
        setProblem(Messages.getString("PreparePartyWizard.22"));

        // now we can not finish the dialog
        setCanFinish(false);
      }

      // Add items
      setLayout(new MigLayout("insets 10,gapx 10,gapy 15", "[][grow]"));
      add(jlFileSelection);
      add(jbFileSelection, "grow,wrap");
      add(jlSelectedFileText);
      add(jlSelectedFile, "grow,wrap");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      if (e.getSource() == jbFileSelection) {
        final JajukFileChooser jfc = new JajukFileChooser(new JajukFileFilter(DirectoryFilter
            .getInstance()), fDir);
        jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        jfc.setDialogTitle(Messages.getString("PreparePartyWizard.22"));
        jfc.setMultiSelectionEnabled(false);
        final int returnVal = jfc.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
          // retrieve selected directory and update it in all necessary places
          fDir = jfc.getSelectedFile();
          jlSelectedFile.setText(fDir.getAbsolutePath());
          data.put(KEY_DEST_PATH, fDir);
          setProblem(null);

          // now we can finish the dialog
          setCanFinish(true);
        }
      }
    }

    @Override
    public String getDescription() {
      return Messages.getString("PreparePartyWizard.19");
    }

    @Override
    public String getName() {
      return Messages.getString("PreparePartyWizard.18");
    }
  }
}
