package net.util.usb.port.windows;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.util.async.AsyncTask;
import net.util.usb.port.PortDetectionException;
import net.util.usb.port.PortDetector;
import net.util.usb.port.utils.ExecCommand;

public class WindowsPortDetector extends PortDetector {
	private static final Logger LOG = LoggerFactory.getLogger(WindowsPortDetector.class);
	public static final String CMD_LOGICAL_DISKS = "wmic partition assoc /assocclass:Win32_LogicalDiskToPartition";
	public static final String CMD_DRIVES = "wmic DiskDrive Assoc /assocclass:Win32_DiskDriveToDiskPartition";
	public static final String REGISTRY_LOCATION = "HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Enum\\USB";

	@Override
	public synchronized String getDeviceUSBPortLocation(File file) throws PortDetectionException {
		
		AsyncTask<Map<String, String>> partitionTask = AsyncTask.of(() -> {
			/*
			\\WORKSHEEP\ROOT\CIMV2:Win32_DiskPartition.DeviceID="Disk #2, Partition #2" <-- this can be
			\\WORKSHEEP\ROOT\CIMV2:Win32_DiskPartition.DeviceID="Disk #2, Partition #0"
			\\WORKSHEEP\ROOT\CIMV2:Win32_LogicalDisk.DeviceID="E:".....
			\\WORKSHEEP\ROOT\CIMV2:Win32_DiskPartition.DeviceID="Disk #3, Partition #0"
			\\WORKSHEEP\ROOT\CIMV2:Win32_LogicalDisk.DeviceID="H:".....
			\\WORKSHEEP\ROOT\CIMV2:Win32_DiskPartition.DeviceID="Disk #4, Partition #0"
			\\WORKSHEEP\ROOT\CIMV2:Win32_LogicalDisk.DeviceID="F:".....
			 */
			String partitions = Arrays.stream(ExecCommand.exec(CMD_LOGICAL_DISKS).split("\n"))
					.map(s -> s.trim())
					//find only lines i.e. \\WORKSHEEP\ROOT\CIMV2:Win32_DiskPartition.DeviceID="Disk #2, Partition #0"
					.filter(s -> s.startsWith("\\\\"))
					.collect(Collectors.joining("\n"));
			
			Map<String, String> partitionMap = new HashMap<>();
			String partition = null;
			for (String line : Arrays.asList(partitions.split("\n"))) {
				if (line.contains("Win32_DiskPartition.DeviceID")) {
					//\\WORKSHEEP\ROOT\CIMV2:Win32_DiskPartition.DeviceID="Disk #2, Partition #0"
					partition = line.split("Win32_DiskPartition.DeviceID=\"")[1];
					//Disk #2, Partition #0"\n\r
					partition = partition.substring(0, partition.indexOf('"'));
				}
				if (line.contains("Win32_LogicalDisk.DeviceID")) {
					//\\WORKSHEEP\ROOT\CIMV2:Win32_LogicalDisk.DeviceID="E:"...
					String disk = line.split("Win32_LogicalDisk.DeviceID=\"")[1];
					//E:"...
					disk = disk.substring(0, disk.indexOf('"'));
					partitionMap.put(disk, partition);
				}
			}
			
			return partitionMap;
		});
		
		AsyncTask<Map<String, String>> diskDrivesTask = AsyncTask.of(() -> {
			/*
			\\WORKSHEEP\ROOT\CIMV2:Win32_DiskDrive.DeviceID="\\\\.\\PHYSICALDRIVE2" 
			\\WORKSHEEP\ROOT\CIMV2:Win32_DiskPartition.DeviceID="Disk #2, Partition #0"....
			 */
			String diskDrives = Arrays.stream(ExecCommand.exec(CMD_DRIVES).split("\n"))
					.map(s -> s.trim())
					//find only lines i.e. \\WORKSHEEP\ROOT\CIMV2:Win32_DiskPartition.DeviceID="Disk #2, Partition #0"
					.filter(s -> s.startsWith("\\\\"))
					.collect(Collectors.joining("\n"));
			
			Map<String, String> diskDrivesMap = new HashMap<>();
			String deviceId = null;
			for (String line : Arrays.asList(diskDrives.split("\n"))) {
				if (line.contains("Win32_DiskDrive.DeviceID")) {
					//\\WORKSHEEP\ROOT\CIMV2:Win32_DiskDrive.DeviceID="\\\\.\\PHYSICALDRIVE2" 
					deviceId = line.split("Win32_DiskDrive.DeviceID=\"")[1];
					//\\\\.\\PHYSICALDRIVE2"\n\r
					deviceId = deviceId.substring(0, deviceId.indexOf('"'));
				}
				if (line.contains("Win32_DiskPartition.DeviceID")) {
					//\\WORKSHEEP\ROOT\CIMV2:Win32_DiskPartition.DeviceID="Disk #2, Partition #0"....
					String currentPartition = line.split("Win32_DiskPartition.DeviceID=\"")[1];
					//Disk #2, Partition #0"....
					currentPartition = currentPartition.substring(0, currentPartition.indexOf('"'));
					diskDrivesMap.put(currentPartition, deviceId);
				}
			}
			
			return diskDrivesMap;
		});
		
		partitionTask.start();
		diskDrivesTask.start();
		
		try {
			partitionTask.join();
			diskDrivesTask.join();
			
			String diskLetter = file.getAbsolutePath().substring(0, 2);
			
			String physicalDrive = diskDrivesTask.getResult().get(partitionTask.getResult().get(diskLetter));
			
			String pnpDeviceID = ExecCommand.exec("wmic DiskDrive where \"DeviceID='" + physicalDrive + "'\" get PNPDeviceID").split("\n")[1].trim();
			
			String containerID = WindowsReqistry.readRegistry("HKLM\\SYSTEM\\CurrentControlSet\\Enum\\" + pnpDeviceID, "ContainerID");
			
			if (containerID == null) {
				return "";
			}
			
			return WindowsReqistry.readLocation(REGISTRY_LOCATION)
					.stream()
					//HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Enum\USB\ROOT_HUB20
					//HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Enum\USB\VID_AAAA&PID_8816
					//...
					.flatMap(loc -> WindowsReqistry.readLocation(loc).stream())
					//HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Enum\USB\VID_AAAA&PID_8816\130818v01
					.map(loc -> WindowsReqistry.readLocation(loc))
					.filter(params -> params.stream().anyMatch(param -> param.contains(containerID)))
					.flatMap(List::stream)
					.filter(param -> param.contains("LocationInformation"))
					.findFirst()
					//    LocationInformation    REG_SZ    Port_#0004.Hub_#0006 \n
					.map(param -> {
						String[] parsed = param.split("\t|(    )");
						return parsed[parsed.length - 1].trim();
					})
					.orElse("");
		} catch (Exception e) {
			throw new PortDetectionException(e);
		}
	}

	@Override
	public void runDiagnostic() {
		LOG.info("Running diagnostic: ");
		LOG.info(CMD_LOGICAL_DISKS);
		LOG.info(ExecCommand.exec(CMD_LOGICAL_DISKS));
		LOG.info(CMD_DRIVES);
		LOG.info(ExecCommand.exec(CMD_DRIVES));
		LOG.info("Reading registry: " + REGISTRY_LOCATION);
		LOG.info(String.join("\n", WindowsReqistry.readLocation(REGISTRY_LOCATION).toArray(new String[0])));
	}

}
