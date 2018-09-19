package net.util.usb;

import java.io.File;

public class USBDevice {

	private final File rootDirectory;

	private String port;

	public USBDevice(File rootDirectory, String port) {
		this.rootDirectory = rootDirectory;
		this.port = port;
	}

	public File getRootDirectory() {
		return rootDirectory;
	}
	
	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	@Override
	public String toString() {
		return "USBDevice [rootDirectory=" + rootDirectory + ", port=" + port + "]";
	}
}
