package net.util.usb;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PollingJob extends Thread {

	private static final Logger LOG = LoggerFactory.getLogger(PollingJob.class);
    private static final String VOLUMES_PATH = "/Volumes";

	private final List<DevicePathEventListener> listeners = new ArrayList<>();

	private long pollingInterval;

	private File[] lastRoots;

	public PollingJob() {
		this.lastRoots = new File[0];
		setDaemon(true);
	}

	/**
	 * drop all stored roots - after this system will check all ports for all devices
	 */
	public void resetRoots() {
		lastRoots = new File[0];
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(pollingInterval);

				if (listeners.isEmpty()) {
					lastRoots = new File[0];
					continue;
				}

                File[] roots = checkOS();

                if (Objects.nonNull(roots)) {
					Arrays.sort(roots);

					if (!Arrays.equals(roots, lastRoots)) {
						synchronized (listeners) {
                            detectDevice(roots);
						}
					}

					lastRoots = roots;
				}
			} catch (InterruptedException e) {
				LOG.debug("Polling job have been interrupted!");
				break;
			}
		}
	}

    /**
     * Method detects new devices.
     *
     * @param roots device paths.
     */
    public void detectDevice(File[] roots) {
        Set<File> rootsSet = new HashSet<>(Arrays.asList(roots));
        rootsSet.removeAll(Arrays.asList(lastRoots));
        rootsSet.forEach(path -> listeners.forEach(listener -> listener.event(new DevicePathEvent(DevicePathEventType.CONNECTED, path))));

        rootsSet = new HashSet<>(Arrays.asList(roots));
        rootsSet.retainAll(Arrays.asList(lastRoots));
        Set<File> rootsSetDisconnected = new HashSet<>(Arrays.asList(lastRoots));
        rootsSetDisconnected.removeAll(rootsSet);
        rootsSetDisconnected.forEach(path -> listeners.forEach(listener -> listener.event(new DevicePathEvent(DevicePathEventType.DISCONNECTED, path))));
    }

    private File[] checkOS() {
        File[] roots = null;
        if (SystemUtils.IS_OS_WINDOWS) {
            roots = File.listRoots();
        } else if (SystemUtils.IS_OS_MAC) {
            roots = new File(VOLUMES_PATH).listFiles();
        }
        return roots;
    }

	public boolean containsEventListener(DevicePathEventListener listener) {
		return listeners.contains(listener);
	}
	
	/**
	 * @return true if listener have been added
	 */
	public boolean addEventListener(DevicePathEventListener listener) {
		if (listeners.contains(listener)) {
            return false;
		}

		synchronized (listeners) {
			return listeners.add(listener);
		}
	}

    void removeEventListener(DevicePathEventListener listener) {
		synchronized (listeners) {
            listeners.remove(listener);
		}
	}

    public enum DevicePathEventType {
		CONNECTED, DISCONNECTED
	}

	public static class DevicePathEvent {

		private DevicePathEventType type;
		private File path;

        DevicePathEvent(DevicePathEventType type, File path) {
			super();
			this.type = type;
			this.path = path;
		}

		public DevicePathEventType getType() {
			return type;
		}

		public void setType(DevicePathEventType type) {
			this.type = type;
		}

		public File getPath() {
			return path;
		}

		public void setPath(File path) {
			this.path = path;
		}

		@Override
		public String toString() {
            return new ToStringBuilder(this)
                    .append("type", type)
                    .append("path", path)
                    .toString();
		}
	}

    public interface DevicePathEventListener {
		void event(DevicePathEvent event);
	}

	public long getPollingInterval() {
		return pollingInterval;
	}

	public void setPollingInterval(long pollingInterval) {
		this.pollingInterval = pollingInterval;
	}
}
