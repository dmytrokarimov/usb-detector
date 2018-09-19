package net.util.usb;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.util.usb.PollingJob.DevicePathEventListener;
import net.util.usb.PollingJob.DevicePathEventType;
import net.util.usb.USBEventType.DeviceEventType;
import net.util.usb.port.PortDetectionException;
import net.util.usb.port.PortDetector;

/**
 * Detects inserting of removable devices with their port.
 *  
 * @author Dmytro Karimov
 */
public class UsbDetector {
    private static final Logger LOG = LoggerFactory.getLogger(UsbDetector.class);

    private final static int MAX_RETRIES = 5;

    private long pollingInterval = 200;

    private final List<Consumer<DetectorState>> detectorStateListeners = new ArrayList<>();
    
    private final List<USBEventListener> listeners = new ArrayList<>();

    private final PollingJob deviceDetector = new PollingJob();

    private final PortDetector portDetector = PortDetector.getInstance();

    private final DevicePathEventListener usbDriveListener;

    private static UsbDetector instance;

    private UsbDetector() {
        usbDriveListener = (ev) -> {
        	fireDetectorState(DetectorState.NEW_DEVICE_FOUND);

            synchronized (listeners) {
                LOG.debug("Device event: " + ev);


                if (ev.getType() == DevicePathEventType.CONNECTED) {
                    listeners.forEach(l -> l.usbEvent(new USBEventType(DeviceEventType.NEW_DEVICE, ev.getPath(), null)));

                    String port = getPort(ev.getPath());
                    LOG.debug("port " + port);
                    if (port != null && !port.isEmpty()) {
                        listeners.forEach(l -> l.usbEvent(new USBEventType(DeviceEventType.CONNECTED, ev.getPath(), port)));
                    }
                } else {
                    listeners.forEach(l -> l.usbEvent(new USBEventType(DeviceEventType.REMOVED, ev.getPath(), null)));
                }
            }

            fireDetectorState(DetectorState.NEW_DEVICE_RECOGNIZED);
        };

        deviceDetector.setPollingInterval(pollingInterval);
        deviceDetector.start();
    }
    
    private void fireDetectorState(DetectorState state) {
    	detectorStateListeners.forEach(l -> l.accept(state));
    }

    public static UsbDetector getInstance() {
        if (instance == null) {
            synchronized (UsbDetector.class) {
                if (instance == null) {
                    instance = new UsbDetector();
                }
            }
        }
        return instance;
    }

    private String getPort(File deviceRootDir) {
        int retryCount = 0;
        while (retryCount++ <= MAX_RETRIES) {
            if (retryCount > 1) {
                LOG.debug("retrying...");
            }
            try {
                return portDetector.getDeviceUSBPortLocation(deviceRootDir);
            } catch (PortDetectionException e) {
                LOG.error("Can't read port for device: " + deviceRootDir, e);
            }
        }

        return null;
    }

    /**
     * Note: blocking operation
     */
    public void runDiagnostic() {
        portDetector.runDiagnostic();
    }

    /**
     * Create events for all connected removable devices
     *
     * @return true in case if at least 1 listener is present
     */
    public boolean forceEvents() {
        if (!isStarted()) {
            return false;
        }

        if (listeners.isEmpty()) {
            return false;
        }

        LOG.debug("Force events for all currently connected devices");

        deviceDetector.resetRoots();

        return true;
    }
    
    /**
     * Can be used to show useful information about process of recognizition of new devices
     */
    public boolean addDetectorStateEventListener(Consumer<DetectorState> listener) {
        if (detectorStateListeners.contains(listener)) {
            return false;
        }

        synchronized (detectorStateListeners) {
        	detectorStateListeners.add(listener);
            return true;
        }
    }

    public boolean removeDetectorStateEventListener(Consumer<DetectorState> listener) {
        synchronized (detectorStateListeners) {
            return detectorStateListeners.remove(listener);
        }
    }

    /**
     * @return true in case if listener already added
     */
    public boolean isListenerExist(USBEventListener listener) {
        return listeners.contains(listener);
    }
    
    /**
     * Adds listner that react on new devices
     * @return false in case if listener already added
     */
    public boolean addEventListener(USBEventListener listener) {
        if (listeners.contains(listener)) {
            return false;
        }

        synchronized (listeners) {
            listeners.add(listener);
            return true;
        }
    }

    public boolean removeEventListener(USBEventListener listener) {
        synchronized (listeners) {
            return listeners.remove(listener);
        }
    }

    public boolean isStarted() {
        return deviceDetector.containsEventListener(usbDriveListener);
    }

    public void start() {
        if (!isStarted()) {
            LOG.debug("Starting drive detector");
            deviceDetector.addEventListener(usbDriveListener);

            forceEvents();
        }
    }

    public void stop() {
        LOG.debug("Stopping drive detector");
        deviceDetector.removeEventListener(usbDriveListener);
    }

    public List<USBEventListener> getListeners() {
        return Collections.unmodifiableList(listeners);
    }


    /**
     * @return pollingInterval
     */
    public long getPollingInterval() {
        return pollingInterval;
    }

    /**
     * Sets the polling interval
     *
     * @param pollingInterval the interval in milliseconds to poll for the USB
     *                        storage devices on the system.
     */
    public void setPollingInterval(long pollingInterval) {
        deviceDetector.setPollingInterval(pollingInterval);
        this.pollingInterval = pollingInterval;
    }
    
    public static enum DetectorState {
    	/**
    	 * new device inserted but do not recognized as removable yet
    	 */
    	NEW_DEVICE_FOUND, 
    	/**
    	 * new device inserted and recognized as removable (or not)
    	 */
    	NEW_DEVICE_RECOGNIZED
    }
}
