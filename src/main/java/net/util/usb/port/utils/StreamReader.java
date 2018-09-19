package net.util.usb.port.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

class StreamReader extends Thread {
    private final InputStream is;
    private final StringWriter sw= new StringWriter();

    public StreamReader(InputStream is) {
    	setDaemon(true);
        this.is = is;
    }

    @Override
	public void run() {
        try {
            int c;
            while ((c = is.read()) != -1) {
				sw.write(c);
			}
        }
        catch (IOException e) { 
    }
    }

    public String getResult() {
        return sw.toString();
    }
}
