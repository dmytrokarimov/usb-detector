package net.util.usb;

import java.io.File;

public class USBEventType {

	private final File rootDirectory;
	private DeviceEventType type;
	private String port;

	public USBEventType(DeviceEventType type, File rootDirectory, String port) {
		this.rootDirectory = rootDirectory;
		this.type = type;
		this.port = port;
	}

	public DeviceEventType getType() {
		return type;
	}

	public void setType(DeviceEventType type) {
		this.type = type;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}
	
	public File getRootDirectory() {
		return rootDirectory;
	}

	@Override
	public String toString() {
		return "USBEventType [type=" + type + ", port=" + port + ", device=" + super.toString() + "]";
	}

	public static enum DeviceEventType {
	    /**
	     * A device has been removed.
	     */
	    REMOVED,

	    /**
	     * A new device has been connected. Port read.
	     */
	    CONNECTED,
	    
	    /**
	     * Device has been connected, port do not recognized yet
	     */
	    NEW_DEVICE
	}
}
