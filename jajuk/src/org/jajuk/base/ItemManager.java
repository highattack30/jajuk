/*
 *  Jajuk
 *  Copyright (C) 2005 Bertrand Florat
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

package org.jajuk.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.jajuk.util.ITechnicalStrings;
import org.jajuk.util.Util;
import org.xml.sax.Attributes;

/**
 *  Managers parent class
 *
 * @author     Bertrand Florat
 * @created    20 juin 2005
 */
public abstract class ItemManager implements ITechnicalStrings{

    /**Custom Properties -> format*/
    LinkedHashMap properties;
    /** Map ids and properties, survives to a refresh, is used to recover old properties after refresh */
	HashMap hmIdProperties = new HashMap(1000);
    
    /**
     * Constructor
     *
     */
    ItemManager(){
        properties = new LinkedHashMap();
    }
    
    /**
     * 
     * @return identifier used for XML generation
     */
    abstract public String getIdentifier();
    
    /**
     * 
     * @param sProperty
     * @return format for given property
     */
    public String getFormat(String sProperty){
        return (String)properties.get(sProperty);
    }
    
    /**
     * Restore properties after a refresh if possible
     * @param item
     * @param sId
     */
    public void restorePropertiesAfterRefresh(IPropertyable item,String sId){
	    LinkedHashMap properties = (LinkedHashMap)hmIdProperties.get(sId); 
		if ( properties == null){  //new file
			hmIdProperties.put(sId,item.getProperties());
		}
		else{  //reset properties before refresh
			item.setProperties(properties);
		}
    }
    
    /**
     * Add a property 
     * @param sProperty
     * @param sFormat
     */
    public void addProperty(String sProperty,String sFormat){
        properties.put(Util.formatXML(sProperty),Util.formatXML(sFormat)); //make sure to clean strings for XML compliance
    }
    
    /**Remove a property **/
    public void removeProperty(String sProperty){
        properties.remove(sProperty);
        applyRemoveProperty(sProperty); //remove ths property to all items
    }
    
    /**Add new property to all items for the given manager*/
    public void applyNewProperty(String sProperty){
        Collection items = getItems();
        if (items != null){
            Iterator it = items.iterator();
            while (it.hasNext()){
                IPropertyable item = (IPropertyable)it.next();
                //just initialize void fields
                if (item.getValue(sProperty) != null){
                	continue;
                }
                String sValue = "";
                if (getFormat(sProperty).equals(FORMAT_BOOLEAN)){
                    sValue = FALSE;
                }
                else if (getFormat(sProperty).equals(FORMAT_NUMBER)){
                    sValue = "0";
                }
                item.setProperty(sProperty,sValue);
            }    
        }
    }   
    
    /**Add new property to all items and all custom properties for the given manager*/
    public void applyNewProperties(){
        Iterator it = properties.keySet().iterator();
        while (it.hasNext()){
        	String sProperty = (String)it.next();
        	applyNewProperty(sProperty);
        }
    }   
    
    

    /**Remove a custom property to all items for the given manager*/
    public void applyRemoveProperty(String sProperty) {
        Collection items = getItems();
        if (items != null){
            Iterator it = items.iterator();
            while (it.hasNext()){
                IPropertyable item = (IPropertyable)it.next();
                item.removeProperty(sProperty);
            }    
        }
    }
    
    /**
     * 
     * @return items for given item manager
     */
    public Collection getItems(){
        Collection items = null;
        if (this instanceof AlbumManager){
            items = AlbumManager.getAlbums();
        }
        else if (this instanceof AuthorManager){
            items = AuthorManager.getAuthors();
        }
        else if (this instanceof DeviceManager){
            items = DeviceManager.getDevicesList();
        }
        else if (this instanceof DirectoryManager){
            items = DirectoryManager.getDirectories();
        }
        else if (this instanceof FileManager){
            items = FileManager.getFiles();
        }
        else if (this instanceof PlaylistFileManager){
            items = PlaylistFileManager.getPlaylistFiles();
        }
        else if (this instanceof PlaylistManager){
            items = PlaylistManager.getPlaylists();
        }
        else if (this instanceof StyleManager){
            items = StyleManager.getStyles();
        }
        else if (this instanceof TrackManager){
            items = TrackManager.getTracks();
        }
        else if (this instanceof TypeManager){
            items = TypeManager.getTypes();
        }
        return items;
    }
    
    /**
     * 
     * @return XML representation of this manager
     */
    public String toXML(){
        StringBuffer sb = new StringBuffer("\t<").append(getIdentifier()); //$NON-NLS-1$
        Iterator it = properties.keySet().iterator();
        while (it.hasNext()) {
            String sProperty = (String) it.next();
            String sFormat = (String)properties.get(sProperty);
            sb.append(" "+sProperty + "='" + sFormat + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        sb.append(">\n"); //$NON-NLS-1$
        return sb.toString();
    }
    
    public Collection getCustomProperties(){
        return properties.keySet();
    }
  
    public String getPropertyAtIndex(int index){
        ArrayList al = new ArrayList(properties.keySet());
    	return (String)al.get(index);
    }
    
    /**
     * Set all personnal properties of an XML file for an item manager
     * 
     * @param attributes :
     *                list of attributes for this XML item
     */
    public void populateProperties(Attributes attributes) {
       for (int i = 0; i < attributes.getLength(); i++) {
             addProperty(attributes.getQName(i), attributes.getValue(i));
        }
    }
    
    /**
     *  Get Item with a given attribute name and ID   
     * @param sItem
     * @param sID
     * @return
     */
    public static IPropertyable getItemByID(String sItem,String sID){
        if (XML_DEVICE.equals(sItem)){
            return DeviceManager.getDevice(sID);
        }
        else if (XML_TRACK.equals(sItem)){
            return TrackManager.getTrack(sID);
        }
        else if (XML_ALBUM.equals(sItem)){
            return AlbumManager.getAlbum(sID);
        }
        else if (XML_AUTHOR.equals(sItem)){
            return AuthorManager.getAuthor(sID);
        }
        else if (XML_STYLE.equals(sItem)){
            return StyleManager.getStyle(sID);
        }
        else if (XML_DIRECTORY.equals(sItem)){
            return DirectoryManager.getDirectory(sID);
        }
        else if (XML_FILE.equals(sItem)){
            return FileManager.getFileById(sID);
        }
        else if (XML_PLAYLIST_FILE.equals(sItem)){
            return PlaylistFileManager.getPlaylistFile(sID);
        }
        else if (XML_PLAYLIST.equals(sItem)){
            return PlaylistManager.getPlaylist(sID);
        }
        else if (XML_TYPE.equals(sItem)){
            return TypeManager.getType(sID);
        }
        else{
            return null;
        }
    }
    
}
