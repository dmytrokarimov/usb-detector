package net.util.usb.port.windows;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.util.usb.port.utils.ExecCommand;

public class WindowsReqistry {

	/**
	 * 
	 * @param location
	 *            path in the registry
	 * @param key
	 *            registry key
	 * @return registry value or null if not found
	 */
	public static final String readRegistry(String location, String key) {
		try {
			String output = ExecCommand.exec("reg query \"" + location + "\" /v " + key);

			//there is no value
			if (!output.contains(key)) {
				return null;
			}

			String[] parsed = output.split("\t|(    )");
			return parsed[parsed.length - 1].trim();
		} catch (Exception e) {
			return null;
		}
	}
	public static final List<String> readLocation(String location) {
		try {
			String output = ExecCommand.exec("reg query \"" + location + "\"");
			return Arrays.asList(output.split("\n")).stream().filter(s -> !s.isEmpty()).collect(Collectors.toList());
		} catch (Exception e) {
			return null;
		}
	}
}