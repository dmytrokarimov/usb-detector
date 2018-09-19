package net.util.usb.port;

public class PortDetectionException extends Exception{

	private static final long serialVersionUID = -5139817127499403761L;
	
	public PortDetectionException() {
        super();
    }

    public PortDetectionException(String message) {
        super(message);
    }

    public PortDetectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public PortDetectionException(Throwable cause) {
        super(cause);
    }
}
