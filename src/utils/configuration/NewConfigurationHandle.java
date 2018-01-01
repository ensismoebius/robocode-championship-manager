/**
 * 
 */
package utils.configuration;

import java.util.Properties;

/**
 * This program is licensed under GPLv3 see the README file for more information
 * 
 * Implement this interface to load new configuration if not any
 * 
 * @author André Furlan - ensismoebius@gmail.com
 */
public interface NewConfigurationHandle {

	/**
	 * Returns the properties for the system
	 * 
	 * @return
	 */
	public Properties askForProperties();

}
