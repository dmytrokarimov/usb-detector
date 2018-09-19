package net.util.usb.port;

import java.io.File;

import net.util.usb.port.macos.OSXPortDetector;
import net.util.usb.port.windows.WindowsPortDetector;

/**
 * Detect ports that some device uses
 * @author Dmytro Karimov
 */
public abstract class PortDetector {
	private static final PortDetector instance;
	
	private static final String OSName = System.getProperty("os.name").toLowerCase();
	static {
        if (OSName.startsWith("win")) {
            instance = new WindowsPortDetector();
        } else if (OSName.startsWith("mac")) {
            instance = new OSXPortDetector();
        } else {
            instance = null;
        }
    }
	
	/**
	 * Try do identify device location for current OS
	 * @param file path to drive
	 * @return empty string in case if port cannot be read for device
	 * @throws PortDetectionException on any Exception
	 */
	public abstract String getDeviceUSBPortLocation(File file) throws PortDetectionException;

	/**
	 * Runs diagnostic and prints ueful information to log. Useful for debugging and reporting
	 */
	public abstract void runDiagnostic();
	
	/**
	 * @return instance for current OS
	 */
	public static PortDetector getInstance() {
		return instance;
	}
}
