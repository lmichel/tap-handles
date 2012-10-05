package metabase;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

import resources.RootClass;

/**
 * This class wraps a {@link LinkedHashMap} of TAP nodes.
 * Each node is referenced by a key (a String) either computed by @see computeKey or
 * given by the caller.
 * @author laurent
 * @version $Id$
 */
public class NodeMap  extends RootClass {
	/**
	 * {@link TapNode} map. Created at loading time
	 */
	private LinkedHashMap<String , TapNode> nodeMap = new LinkedHashMap<String , TapNode>();

	/**
	 * Attempt to extract a key from a node URL. 
	 * The key is built from the host name and from application name 
	 * @param  url
	 * @return Returns the key
	 * @throws MalformedURLException
	 */
	protected String computeKey(String url) throws MalformedURLException {
		if( !url.startsWith("http://") ) {
			return url;
		}
		else {
			URL hurle = new URL(url);
			return hurle.getHost().replaceAll("www.", "").replaceAll("[_\\.]", "") + "_" + hurle.getPath().split("\\/")[1].replaceAll("[_\\.]", "");
		}
	}
	
	/**
	 *  Add to the map node. The key is computed internally.
	 * @param url        URL of the TAP service
	 * @return           Returns the node key
	 * @throws Exception if the service is not valid or if another node is already
	 *                   referenced by that key
	 */
	protected  String addNode(String url) throws Exception {
		return this.addNode(url, this.computeKey(url));
	}
	
	/**
	 * Add to the map node.
	 * @param url        URL of the TAP service
	 * @param key        key referencing the node
	 * @return           Returns  the node key
	 * @throws Exception if the service is not valid or if another node is already
	 *                   referenced by that key
	 */
	public String addNode(String url, String key) throws Exception {
		TapNode nm;
		if( (nm = this.getNode(key)) != null ) {
			throw new Exception("Node with \"" + key + "\" as key already exists (" + nm.getUrl() + ")");
		} else {
			logger.info("Create new Tap node " + url + " referenced with the key " + key);
			nm = new TapNode(url, MetaBaseDir + key, key);
			nodeMap.put(key, nm);
			return key;
		}
	}
	
	/**
	 * Returns true if the a node exist with key as key.
	 * @param key
	 * @return
	 * @throws Exception
	 */
	public boolean hasNode(String key) throws Exception {
		if( this.getNode(key) != null ) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Returns the {@link TapNode} referenced by key 
	 * @param   key
	 * @return  Returns the TAP node
	 * @throws Exception If the node cannot be found
	 */
	public TapNode getNode(String key)  throws Exception{
		return  nodeMap.get(key);
	}
	
	/**
	 * Return then key of the node having url as url
	 * '/' chars are discarded from the comparison to avoid trail chars issues
	 * @param url
	 * @return
	 * @throws Exception
	 */
	protected String getKeyNodeByUrl(String url) throws Exception {
		String rurl = url.replaceAll("/", "");
		for( Entry<String, TapNode> e : nodeMap.entrySet()) {
			if( e.getValue().getUrl().replaceAll("/", "").equals(rurl) ) {
				return e.getKey();
			}
		}
		return null;
	}
	
	/**
	 * Removes the node referenced by key. Does nothing of the node des not exist.
	 * @param Returns key
	 */
	protected void removeNode(String key) {
		nodeMap.remove(key);
	}
	
	/**
	 * @return Returns the set of keys of he node map
	 */
	protected Set<String>  keySet() {
		return nodeMap.keySet();
	}

}
