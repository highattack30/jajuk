/** Jajuk Specific version of this swingx class to fix 
 * this: https://swingx.dev.java.net/issues/show_bug.cgi?id=464
 *
 * This file has been adapted to Jajuk by the Jajuk Team.
 * Jajuk Copyright (C) 2007 The Jajuk Team
 *
 * The original copyrights and license follow:
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150
 * Network Circle, Santa Clara, California 95054, U.S.A. All rights
 * reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package ext;

import javax.swing.UIManager;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import org.jdesktop.swingx.autocomplete.AbstractAutoCompleteAdaptor;
import org.jdesktop.swingx.autocomplete.ObjectToStringConverter;

/**
 * A document that can be plugged into any JTextComponent to enable automatic
 * completion. It finds and selects matching items using any implementation of
 * the AbstractAutoCompleteAdaptor.
 */
public class AutoCompleteDocument extends PlainDocument {

  /** Generated serialVersionUID. */
  private static final long serialVersionUID = -4353609211147483101L;

  /** Flag to indicate if adaptor.setSelectedItem has been called. Subsequent calls to remove/insertString should be ignored as they are likely have been caused by the adapted Component that is trying to set the text for the selected component. */
  boolean selecting = false;

  /** true, if only items from the adaptors's list can be entered false, otherwise (selected item might not be in the adaptors's list). */
  boolean strictMatching;

  /** The adaptor that is used to find and select items. */
  AbstractAutoCompleteAdaptor adaptor;

  /** DOCUMENT_ME. */
  ObjectToStringConverter stringConverter;

  /**
   * Creates a new AutoCompleteDocument for the given
   * AbstractAutoCompleteAdaptor.
   * 
   * @param adaptor The adaptor that will be used to find and select matching items.
   * @param strictMatching true, if only items from the adaptor's list should be allowed to
   * be entered
   * @param stringConverter the converter used to transform items to strings
   */
  public AutoCompleteDocument(AbstractAutoCompleteAdaptor adaptor, boolean strictMatching,
      ObjectToStringConverter stringConverter) {
    this.adaptor = adaptor;
    this.strictMatching = strictMatching;
    this.stringConverter = stringConverter;
    // Handle initially selected object
    Object selected = adaptor.getSelectedItem();
    if (selected != null) {
      setText(stringConverter.getPreferredStringForItem(selected));
    }
    adaptor.markEntireText();
  }

  /**
   * Creates a new AutoCompleteDocument for the given
   * AbstractAutoCompleteAdaptor.
   * 
   * @param strictMatching true, if only items from the adaptor's list should be allowed to
   * be entered
   * @param adaptor The adaptor that will be used to find and select matching items.
   */
  public AutoCompleteDocument(AbstractAutoCompleteAdaptor adaptor, boolean strictMatching) {
    this(adaptor, strictMatching, ObjectToStringConverter.DEFAULT_IMPLEMENTATION);
  }

  /**
   * Returns if only items from the adaptor's list should be allowed to be
   * entered.
   * 
   * @return if only items from the adaptor's list should be allowed to be
   * entered
   */
  public boolean isStrictMatching() {
    return strictMatching;
  }

  /* (non-Javadoc)
   * @see javax.swing.text.AbstractDocument#remove(int, int)
   */
  @Override
  public void remove(int offs, int len) throws BadLocationException {
    // return immediately when selecting an item
    if (selecting) {
      return;
    }

    super.remove(offs, len);
    if (!strictMatching) {
      setSelectedItem(getText(0, getLength()), getText(0, getLength()));
      adaptor.getTextComponent().setCaretPosition(offs);
    }
  }

  /* (non-Javadoc)
   * @see javax.swing.text.PlainDocument#insertString(int, java.lang.String, javax.swing.text.AttributeSet)
   */
  @Override
  public void insertString(int pOffs, String str, AttributeSet a) throws BadLocationException {
    int offs = pOffs;
    // return immediately when selecting an item
    if (selecting) {
      return;
    }

    // insert the string into the document
    super.insertString(offs, str, a);
    // lookup and select a matching item
    LookupResult lookupResult = lookupItem(getText(0, getLength()));
    if (lookupResult.matchingItem != null) {
      setSelectedItem(lookupResult.matchingItem, lookupResult.matchingString);
    } else if (strictMatching) {
      // keep old item selected if there is no match
      lookupResult.matchingItem = adaptor.getSelectedItem();
      lookupResult.matchingString = adaptor.getSelectedItemAsString();
      // imitate no insert (later on offs will be incremented by
      // str.length(): selection won't move forward)
      offs = offs - str.length();
      // provide feedback to the user that his input has been received but can
      // not be accepted
      UIManager.getLookAndFeel().provideErrorFeedback(adaptor.getTextComponent());
    } else {
      // no item matches => use the current input as selected item
      lookupResult.matchingItem = getText(0, getLength());
      lookupResult.matchingString = getText(0, getLength());
      setSelectedItem(lookupResult.matchingItem, lookupResult.matchingString);
    }
    setText(lookupResult.matchingString);
    // select the completed part
    adaptor.markText(offs + str.length());
  }

  /**
   * Sets the text of this AutoCompleteDocument to the given text.
   * 
   * @param text the text that will be set for this document
   */
  private void setText(String text) {
    try {
      // remove all text and insert the completed string
      super.remove(0, getLength());
      super.insertString(0, text, null);
    } catch (BadLocationException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Selects the given item using the AbstractAutoCompleteAdaptor.
   * 
   * @param itemAsString string representation of the item to be selected
   * @param item the item that is to be selected
   */
  private void setSelectedItem(Object item, String itemAsString) {
    selecting = true;
    adaptor.setSelectedItem(item);
    adaptor.setSelectedItemAsString(itemAsString);
    selecting = false;
  }

  /**
   * Searches for an item that matches the given pattern. The
   * AbstractAutoCompleteAdaptor is used to access the candidate items. The
   * match is case-sensitive and will only match at the beginning of each item's
   * string representation.
   * 
   * @param pattern the pattern that should be matched
   * 
   * @return the first item that matches the pattern or <code>null</code> if
   * no item matches
   */
  private LookupResult lookupItem(String pattern) {
    // iterate over all items to find an exact match
    LookupResult ret = findMatch(pattern, true);
    if (ret != null) {
      return ret;
    }

    // check if the currently selected item matches
    Object selectedItem = adaptor.getSelectedItem();
    String[] possibleStrings = stringConverter.getPossibleStringsForItem(selectedItem);
    if (possibleStrings != null) {
      for (String element : possibleStrings) {
        if (startsWith(element, pattern)) {
          return new LookupResult(selectedItem, element);
        }
      }
    }
    // search for any matching item, if the currently selected does not match
    ret = findMatch(pattern, false);
    if (ret != null) {
      return ret;
    }

    // no item starts with the pattern => return null
    return new LookupResult(null, "");
  }

  /**
   * Find match.
   * 
   * @param pattern DOCUMENT_ME
   * @param exactMatch DOCUMENT_ME
   * 
   * @return the lookup result
   */
  private LookupResult findMatch(final String pattern, final boolean exactMatch) {
    String[] possibleStrings;
    for (int i = 0, n = adaptor.getItemCount(); i < n; i++) {
      Object currentItem = adaptor.getItem(i);
      possibleStrings = stringConverter.getPossibleStringsForItem(currentItem);
      if (possibleStrings != null) {
        // check if current item exactly matches the pattern
        // or starts with the string depending on flag
        for (String element : possibleStrings) {
          if ((exactMatch && element.equals(pattern))
              || (!exactMatch && startsWith(element, pattern))) {
            return new LookupResult(currentItem, element);
          }
        }
      }
    }

    return null;
  }

  /**
   * DOCUMENT_ME.
   */
  private static class LookupResult {

    /** DOCUMENT_ME. */
    Object matchingItem;

    /** DOCUMENT_ME. */
    String matchingString;

    /**
     * Instantiates a new lookup result.
     * 
     * @param matchingItem DOCUMENT_ME
     * @param matchingString DOCUMENT_ME
     */
    public LookupResult(Object matchingItem, String matchingString) {
      this.matchingItem = matchingItem;
      this.matchingString = matchingString;
    }
  }

  /**
   * Returns true if <code>base</code> starts with <code>prefix</code>
   * (taking case into account).
   * 
   * @param base the string to be checked
   * @param prefix the prefix to check for
   * 
   * @return true if <code>base</code> starts with <code>prefix</code>;
   * false otherwise
   */
  private boolean startsWith(String base, String prefix) {
    if (base.length() < prefix.length()) {
      return false;
    }

    return base.regionMatches(false, 0, prefix, 0, prefix.length());
  }

}
