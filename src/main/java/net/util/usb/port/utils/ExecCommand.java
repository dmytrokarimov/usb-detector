package net.util.usb.port.utils;

public class ExecCommand {

	public static String exec(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);

            StreamReader reader = new StreamReader(process.getInputStream());
            reader.start();
            process.waitFor();
            reader.join();
            return reader.getResult();
        }
        catch (Exception e) {
            return null;
        }

	}
}
