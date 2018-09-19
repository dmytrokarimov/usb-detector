package net.util.usb;

@FunctionalInterface
public interface USBEventListener {
    
	/**
	 * Port will be identified only for connected devices
	 * @param event
	 */
    void usbEvent(USBEventType event);
}
