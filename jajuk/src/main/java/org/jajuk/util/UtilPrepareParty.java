/*
 *  Jajuk
 *  Copyright (C) 2003-2009 The Jajuk Team
 *  http://jajuk.info
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
 *  $Revision: 3132 $
 */
package org.jajuk.util;

import ext.ProcessLauncher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.CharUtils;
import org.jajuk.base.File;
import org.jajuk.base.FileManager;
import org.jajuk.base.Playlist;
import org.jajuk.base.PlaylistManager;
import org.jajuk.services.dj.Ambience;
import org.jajuk.services.dj.AmbienceManager;
import org.jajuk.services.dj.DigitalDJ;
import org.jajuk.services.dj.DigitalDJManager;
import org.jajuk.util.error.JajukException;
import org.jajuk.util.log.Log;

/**
 * 
 */
public class UtilPrepareParty {
  /**
   * character that is used to replace if filename normalization is used.
   * 
   */
  private static final String FILLER_CHAR = "_";

  // private constructor to avoid instantiation
  private UtilPrepareParty() {
  }

  /**
   * Filter provided list by removing files that have lower rating.
   * 
   * @param files
   *          the list to process.
   * @param rate
   *          The require rating level
   * 
   * @return The adjusted list.
   */
  public static List<org.jajuk.base.File> filterRating(List<org.jajuk.base.File> files, Integer rate) {
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
   * Filter the provided list by removing files if the specified length (in
   * minutes) is exceeded
   * 
   * @param files
   *          The list of files to process.
   * @param time
   *          The number of minutes playing length to have at max.
   * 
   * @return The modified list.
   */
  public static List<org.jajuk.base.File> filterMaxLength(List<org.jajuk.base.File> files,
      Integer time) {
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
   * Filter the provided list by removing files after the specified size is
   * reached.
   * 
   * @param files
   *          The list of files to process.
   * @param size
   *          The size in MB that should not be exceeded.
   * 
   * @return The modified list.
   */
  public static List<org.jajuk.base.File> filterMaxSize(List<org.jajuk.base.File> files,
      Integer size) {
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
   * Filter the provided list by removing files after the specified number of
   * tracks is reached.
   * 
   * @param files
   *          The list of files to process.
   * @param tracks
   *          The number of tracks to limit the list.
   * 
   * @return The modified list.
   */
  public static List<org.jajuk.base.File> filterMaxTracks(List<org.jajuk.base.File> files,
      Integer tracks) {
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
   * Filter the provided list by removing files so only the specified media is
   * included.
   * 
   * @param files
   *          The list of files to process.
   * @param ext
   *          The number of tracks to filter the list.
   * 
   * @return The modified list.
   */
  public static List<org.jajuk.base.File> filterMedia(final List<org.jajuk.base.File> files,
      final String ext) {
    final List<org.jajuk.base.File> newFiles = new ArrayList<org.jajuk.base.File>();
    for (org.jajuk.base.File file : files) {
      if (file.getType() != null && file.getType().getExtension() != null
          && file.getType().getExtension().equals(ext)) {
        newFiles.add(file);
      }
    }

    return newFiles;
  }

  // map containing all the replacements that we do to "normalize" a filename
  // TODO: this should be enhanced with more entries for things like nordic
  // languages et. al.
  private static Map<Character, String> replaceMap = null;

  /**
   * Normalize filenames so that they do not
   * 
   * @param files
   * @return
   */
  public static synchronized String normalizeFilename(String name) {
    // initialize map if necessary
    if (replaceMap == null) {
      replaceMap = new HashMap<Character, String>();

      // German umlauts can be handled better than just using the filler_char,
      // we
      // can keep the filename readable
      replaceMap.put('ä', "ae");
      replaceMap.put('å', "a");
      replaceMap.put('ö', "oe");
      replaceMap.put('ü', "ue");
      replaceMap.put('Ä', "AE");
      replaceMap.put('Ö', "OE");
      replaceMap.put('Ü', "UE");
      replaceMap.put('ß', "ss");

      /**
       * To add:
       * 
       * <code>
           à   á   â   ã   ä   å
           æ
           À   Á   Â   Ã   Ä   Å
           Æ
           Ç
           ç
           Ð
           È   É   Ê   Ë
           è   é   ê   ë
           Ì   Í   Î   Ï
           ì   í   î   ï
           Ñ
           ñ
           Ò   Ó   Ô   Õ   Ö
           ò   ó   ô   õ   ö
           Ù   Ú   Û   Ü
           ù   ú   û   ü
           Ý
           ý   ÿ
        </code>
       */

      // some more special characters that can be replaced with more useful
      // values
      // than FILLER_CHAR
      replaceMap.put('€', "EUR");
      replaceMap.put('&', "and");

      // replace path-separators and colon that could cause trouble on other
      // OSes, also question mark and star can produce errors
      replaceMap.put('/', FILLER_CHAR);
      replaceMap.put('\\', FILLER_CHAR);
      replaceMap.put(':', FILLER_CHAR);
      replaceMap.put('?', FILLER_CHAR);
      replaceMap.put('*', FILLER_CHAR);
      replaceMap.put('!', FILLER_CHAR);
    }

    // TODO: is there some utility method that can do this?
    StringBuilder newName = new StringBuilder(name.length());
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);

      // replace some things that we can replace with other useful values
      if (replaceMap.containsKey(c)) {
        newName.append(replaceMap.get(c));
      } else if (CharUtils.isAsciiPrintable(c)) {
        // any other ASCII character is added
        newName.append(c);
      } else {
        // everything else outside the ASCII range is simple removed to not
        // cause any trouble
        newName.append(FILLER_CHAR);
      }
    }

    return newName.toString();
  }

  /**
   * Get files from the specified DJ.
   * 
   * @param name
   *          The name of the DJ.
   * 
   * @return A list of files.
   */
  public static List<org.jajuk.base.File> getDJFiles(final String name) {
    DigitalDJ dj = DigitalDJManager.getInstance().getDJByName(name);
    return dj.generatePlaylist();
  }

  /**
   * Get files from the specified Ambience.
   * 
   * @param name
   *          The name of the Ambience.
   * 
   * @return A list of files.
   */
  public static List<org.jajuk.base.File> getAmbienceFiles(String name) {
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

  /**
   * Get files from the specified Playlist. If the name of the playlist is equal
   * to the name of the temporary playlist provided to the Wizard, then this
   * Playlist is used instead.
   * 
   * @param name
   *          The name of the Playlist.
   * 
   * @return A list of files.
   */
  public static List<org.jajuk.base.File> getPlaylistFiles(String name, Playlist tempPlaylist)
      throws JajukException {
    // if we chose the temp-playlist, use this one
    if (tempPlaylist != null && name.equals(tempPlaylist.getName())) {
      return tempPlaylist.getFiles();
    }

    // get the Playlist from the Manager by name
    Playlist playlist = PlaylistManager.getInstance().getPlaylistByName(name);
    return playlist.getFiles();
  }

  /**
   * Get files in random order.
   * 
   * @return Returns a list of all files shuffled into random order.
   */
  public static List<org.jajuk.base.File> getShuffleFiles() {
    // Get a shuffle selection from all files
    return FileManager.getInstance().getGlobalShufflePlaylist();
  }

  /**
   * Get files from the BestOf-Playlist
   * 
   * @return The list of files that match the "BestOf"-criteria
   * 
   * @throws JajukException
   */
  public static List<org.jajuk.base.File> getBestOfFiles() throws JajukException {
    Playlist pl = new Playlist(Playlist.Type.BESTOF, "tmp", "temporary", null);
    return pl.getFiles();
  }

  /**
   * Get the files from the current "Novelties"-criteria.
   * 
   * @return The files that are new currently.
   * 
   * @throws JajukException
   */
  public static List<org.jajuk.base.File> getNoveltiesFiles() throws JajukException {
    Playlist pl = new Playlist(Playlist.Type.NOVELTIES, "tmp", "temporary", null);
    return pl.getFiles();
  }

  /**
   * Get the files from the current Queue
   * 
   * @return The currently queued files.
   * 
   * @throws JajukException
   */
  public static List<org.jajuk.base.File> getQueueFiles() throws JajukException {
    Playlist pl = new Playlist(Playlist.Type.QUEUE, "tmp", "temporary", null);
    return pl.getFiles();
  }

  /**
   * Get the files that are bookmarked.
   * 
   * @return The currently bookmarked files.
   * 
   * @throws JajukException
   */
  public static List<org.jajuk.base.File> getBookmarkFiles() throws JajukException {
    Playlist pl = new Playlist(Playlist.Type.BOOKMARK, "tmp", "temporary", null);
    return pl.getFiles();
  }

  private static List<String> splitCommand(String command) {
    List<String> list = new ArrayList<String>();

    StringBuilder word = new StringBuilder();
    boolean quote = false;
    int i = 0;
    while (i < command.length()) {
      char c = command.charAt(i);
      // word boundary
      if (Character.isWhitespace(c) && !quote) {
        i++;

        // finish current word
        list.add(word.toString());
        word = new StringBuilder();

        // skip more whitespaces
        while (Character.isWhitespace(command.charAt(i)) && i < command.length()) {
          i++;
        }
      } else {
        // on quote we either start or end a quoted string
        if (c == '"') {
          quote = !quote;
        }

        word.append(c);

        i++;
      }
    }

    // finish last word
    if (word.length() > 0) {
      list.add(word.toString());
    }

    return list;
  }

  /**
   * Check if the Perl Audio Converter can be used
   * 
   * @param pacpl
   *          The command-string to call pacpl, e.g. "pacpl" or "perl
   *          C:\pacpl\pacpl", ...
   */
  public static boolean checkPACPL(String pacpl) {
    // here we just want to verify that we find pacpl
    // first build the commandline for "pacpl --help"

    // see the manual page of "pacpl"
    List<String> list = splitCommand(pacpl);
    list.add("--help");

    // create streams for catching stdout and stderr
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();

    int ret = 0;
    final ProcessLauncher launcher = new ProcessLauncher(out, err, 10000);
    try {
      ret = launcher.exec(list.toArray(new String[list.size()]));
    } catch (IOException e) {
      ret = -1;
      Log
          .debug("Exception while checking for 'pacpl', cannot use functionality to convert media files while copying: "
              + e.getMessage());
    }

    // if we do not find the application or if we got an error, log some details
    // and disable notification support
    if (ret != 0) {
      // log out the results
      Log.debug("pacpl command returned to out(" + ret + "): " + out.toString());
      Log.debug("pacpl command returned to err: " + err.toString());

      Log
          .info("Cannot use functionality to convert media files, application 'pacpl' seems to be not available correctly.");
      return false;
    }

    // pacpl is enabled and seems to be supported by the OS
    return true;
  }

  /**
   * Call the external application "pacpl" to convert the specified file into
   * the specified format and store the resulting file in the directory listed.
   * 
   * @param pacpl
   *          The command-string to call pacpl, e.g. "pacpl" or "perl
   *          C:\pacpl\pacpl", ...
   * @param file
   *          The file to convert.
   * @param toFormat
   *          The target format.
   * @param toDir
   *          The target location.
   * @param newName
   *          The new name to use (this is used for normalizing and numbering
   *          the files, ...)
   * 
   * @return 0 if processing was OK, otherwise the return code indicates the
   *         return code provided by the pacpl script
   * 
   *         TODO: currently this uses the target-location as temporary
   *         directory if intermediate-conversion to WAV is necessary, this
   *         might be sub-optimal for Flash-memory where too many writes kills
   *         the media card earlier. We probably should use the temporary
   *         directory for conversion instead and do another copy at the end.
   */
  public static int convertPACPL(String pacpl, File file, String toFormat, java.io.File toDir,
      String newName) {
    // first build the commandline for "pacpl"

    // see the manual page of "pacpl"

    // first split the command itself with observing quotes, splitting is
    // necessary because it can be something like "perl <locatoin>/pacpl"
    List<String> list = splitCommand(pacpl);

    // where to store the file
    list.add("--outdir");
    list.add(toDir.getAbsolutePath());

    // specify new filename
    list.add("--outfile");
    list.add(newName);

    // specify output format
    list.add("--to");
    list.add(toFormat);

    // now add the actual file to convert
    list.add(file.getFIO().getAbsolutePath());

    // create streams for catching stdout and stderr
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();

    int ret = 0;
    Log.debug("Using this pacpl command: {{" + list.toString() + "}}");
    final ProcessLauncher launcher = new ProcessLauncher(out, err);
    try {
      ret = launcher.exec(list.toArray(new String[list.size()]), null, new java.io.File(System
          .getProperty("java.io.tmpdir")));
    } catch (IOException e) {
      ret = -1;
      Log.error(e);
    }

    // log out the results
    if (!out.toString().isEmpty()) {
      Log.debug("pacpl command returned to out(" + ret + "): " + out.toString());
    } else {
      Log.debug("pacpl command returned: " + ret);
    }

    if (!err.toString().isEmpty()) {
      Log.debug("pacpl command returned to err: " + err.toString());
    }

    return ret;
  }
}