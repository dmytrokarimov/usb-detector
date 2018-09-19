package net.util.usb.port.macos;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.NSString;
import com.dd.plist.PropertyListFormatException;
import com.dd.plist.PropertyListParser;

import net.util.usb.port.PortDetectionException;
import net.util.usb.port.PortDetector;
import net.util.usb.port.utils.ExecCommand;

public class OSXPortDetector extends PortDetector {
	private static final Logger LOG = LoggerFactory.getLogger(OSXPortDetector.class);

	public static final String PROFILER_CMD = "system_profiler SPUSBDataType -xml";
	public static final String DISK_UTIL_CMD = "diskutil info";
	public static final String DISK_UTIL_ALL = " -all";
	public static final String DISK_UTIL_DEVICE_FMT = " %s";
	public static final String DEVICE_NAME_FMT = "/dev/%s";
	public static final String INFO_MOUNTPOINT = "Mount Point";

	@Override
	public synchronized String getDeviceUSBPortLocation(File file) throws PortDetectionException {
		String result = "";
		String rootPath = file.getAbsolutePath();
		try {
			String plistXml = ExecCommand.exec(PROFILER_CMD);
			result = findDeviceLocation(rootPath, plistXml);
		} catch (IOException | PropertyListFormatException | ParseException | ParserConfigurationException | SAXException e) {
			throw new IllegalStateException(e);
		}

		if (result.equals("")) {
			LOG.warn("Device has not been found in system_profiler!");
		}

		return result;
	}

	@Override
	public void runDiagnostic() {
		LOG.info("Running diagnostic: " + PROFILER_CMD);
		LOG.info(ExecCommand.exec(PROFILER_CMD));
		LOG.info("Running diagnostic: " + DISK_UTIL_CMD + DISK_UTIL_ALL);
		LOG.info(ExecCommand.exec(DISK_UTIL_CMD + DISK_UTIL_ALL));
	}

	protected String findDeviceLocation(String rootPath, String plistXml) throws IOException, PropertyListFormatException,
			ParseException, ParserConfigurationException, SAXException {
		NSArray devices = (NSArray) PropertyListParser.parse(plistXml.getBytes());
		String result = traverseItems(devices, rootPath);
		if (StringUtils.isNotBlank(result)) {
			result = result.trim();
		}
		return result;
	}

	private String traverseItems(NSArray items, String root) {
		for (NSObject nsObject : Arrays.asList(items.getArray())) {
			if (nsObject instanceof NSDictionary) {
				NSDictionary device = (NSDictionary) nsObject;

				if (device.containsKey("Media")) {
					for (NSObject media : Arrays.asList(((NSArray)device.get("Media")).getArray())) {
						if (media instanceof NSDictionary) {
							if (((NSDictionary) media).containsKey("volumes")) {
								for (NSObject volumeObj : Arrays.asList(((NSArray) ((NSDictionary) media).get("volumes")).getArray())) {
									NSDictionary volume = (NSDictionary) volumeObj;
									if (volume.containsKey("mount_point")) {
										NSString mountPoint = (NSString) volume.get("mount_point");
										if (root.equals(mountPoint.getContent())) {
											return ((NSString) device.get("location_id")).getContent().split("/")[0];
										}
									}
								}
							} else { // no volumes, look in df
								String fullDeviceName = String.format(DEVICE_NAME_FMT, ((NSString) ((NSDictionary) media).get("bsd_name")).getContent());
								String mountPoint = findMountPointByDevice(fullDeviceName);
								if (mountPoint.equals(root)) {
									return ((NSString) device.get("location_id")).getContent().split("/")[0];
								}
							}
						}
					}
				}

				if (device.containsKey("_items")) {
					String result = traverseItems((NSArray) device.get("_items"), root);
					if (!result.equals("")) {
						return result;
					}
				}
			}
		}

		return "";
	}

	protected String findMountPointByDevice(String deviceName) {
		String mountPoint = "";

		String dfOutput = executeDiskUtil(deviceName);
		if (StringUtils.isNotBlank(dfOutput)) {
			String[] dfSplit = dfOutput.split("\\n");
			if (dfSplit != null && dfSplit.length > 0) {
				mountPoint = Arrays.stream(dfSplit)
						.map(line -> line.split(":"))
						.filter(parts -> parts[0].trim().equals(INFO_MOUNTPOINT))
						.map(parts -> parts[1].trim())
						.findFirst().orElse("");
			}
		}

		return mountPoint;
	}

	protected String executeDiskUtil(String deviceName) {
		return ExecCommand.exec(DISK_UTIL_CMD + String.format(DISK_UTIL_DEVICE_FMT, deviceName));
	}

}
