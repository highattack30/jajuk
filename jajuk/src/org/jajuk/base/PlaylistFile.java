/*
 *  Jajuk
 *  Copyright (C) 2003 bflorat
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.jajuk.util.Util;
import org.jajuk.util.log.Log;


/**
 *  A playlist file
 * <p> Physical item
 * @Author     bflorat
 * @created    17 oct. 2003
 */
public class PlaylistFile extends PropertyAdapter implements Comparable {

	/**ID. Ex:1,2,3...*/
	private String  sId;
	/**Playlist name */
	private String sName;
	/**Playlist hashcode*/
	private String  sHashcode;
	/**Playlist parent directory*/
	private Directory dParentDirectory;
	/**Basic Files list*/
	private ArrayList alBasicFiles = new ArrayList(10);
	/**Modification flag*/
	private boolean bModified = false;
	/**Associated physical file*/
	private File fio;
	
	
	/**
	 * Playlist file constructor
	 * @param sId
	 * @param sName
	 * @param sHashcode
	 * @param sParentDirectory
	 */
	public PlaylistFile(String sId, String sName,String sHashcode,Directory dParentDirectory) {
		this.sId = sId;
		this.sName = sName;
		this.sHashcode = sHashcode;
		this.dParentDirectory = dParentDirectory;
		this.fio = new File(getDirectory().getDevice().getUrl()+getDirectory().getAbsolutePath()+"/"+getName());
		load(); //populate playlist
	}

	
	/**
	 * toString method
	 */
	public String toString() {
		return "Playlist file[ID="+sId+" Name=" + getName() + " Hashcode="+sHashcode+" Dir="+dParentDirectory.getId()+"]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$//$NON-NLS-4$//$NON-NLS-5$
	}

	/**
	 * Return an XML representation of this item  
	 * @return
	 */
	public String toXml() {
		StringBuffer sb = new StringBuffer("\t\t<playlist_file id='" + sId);
		sb.append("' name='");
		sb.append(Util.formatXML(sName));
		sb.append("' hashcode='");
		sb.append(sHashcode);
		sb.append("' directory='");
		sb.append(dParentDirectory.getId()).append("' ");
		sb.append(getPropertiesXml());
		sb.append("/>\n");
		return sb.toString();
	}


	/**
	 * Equal method to check two playlist files are identical
	 * @param otherPlaylistFile
	 * @return
	 */
	public boolean equals(Object otherPlaylistFile){
		return this.getHashcode().equals(((PlaylistFile)otherPlaylistFile).getHashcode());
	}	
	
	/**
	 * hashcode ( used by the equals method )
	 */
	public int hashCode(){
		return getId().hashCode();
	}

	/**
	 * @return
	 */
	public String getHashcode() {
		return sHashcode;
	}

	/**
	 * @return
	 */
	public String getId() {
		return sId;
	}

	/**
	 * @return
	 */
	public String getName() {
		return sName;
	}

	/**
	 * @return
	 */
	public Directory getDirectory() {
		return dParentDirectory;
	}
	
	/**
	 *Alphabetical comparator used to display ordered lists of playlist files
	 *@param other playlistfile to be compared
	 *@return comparaison result 
	 */
	public int compareTo(Object o){
		PlaylistFile otherPlaylistFile = (PlaylistFile)o;
		return  getName().compareToIgnoreCase(otherPlaylistFile.getName());
	}

	/**
	 * @return Returns the list of basic files this playlist maps to
	 */
	public ArrayList getBasicFiles() {
		return alBasicFiles;
	}
	
	/**
	 * Add a basic file to this playlist file
	 * @param bf
	 */
	public void addBasicFile(BasicFile bf){
		alBasicFiles.add(bf);
		bModified = true;
	}
	
	/**
	 * Update playlist file on disk if needed
	 *
	 */
	public void commit(){
		BufferedWriter bw = null;
		if ( bModified){
			try {
				bw = new BufferedWriter(new FileWriter(fio));
				bw.write(PLAYLIST_NOTE+"\n");
				Iterator it = alBasicFiles.iterator();
				while ( it.hasNext()){
					BasicFile bfile = (BasicFile)it.next();
					bw.write(bfile.getAbsolutePath()+"\n");
				}
			}
			catch(Exception e){
				Log.error("017",getName(),e);
			}
			finally{
				if ( bw != null){
					try {
						bw.flush();
						bw.close();
					} catch (IOException e1) {
						Log.error(e1);
					}
				}
			}
		}
	}
	
	/**
	 * Parse a playlist file
	 */
	public void load(){
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(fio));
			String sLine = null;
			while ((sLine = br.readLine()) != null){
				StringBuffer sb = new StringBuffer(sLine);
				if ( sb.charAt(0) == '#'){  //comment
					continue;
				}
				else{
					File fileTrack = null;
					if (sb.charAt(0) == '.' || (sb.indexOf("/")==-1 && sb.indexOf("\\")==-1)){  //relative path or not directory at all, move to this directory
						fileTrack = new File(getDirectory().getDevice().getUrl()+getDirectory().getAbsolutePath()+"/"+sLine);			
					}
					else{//a full path is specified
						fileTrack = new File(sLine); 
					}
					if ( fileTrack.exists()){
						BasicFile bfile = new BasicFile(fileTrack);
						alBasicFiles.add(bfile);
					}
				}
			}
		}
		catch(Exception e){
			Log.error("017",getName(),e);
		}
		finally{
			if ( br != null){
				try {
					br.close();
				} catch (IOException e1) {
					Log.error(e1);
				}
			}
		}
	}
	

}
