# USB Detector

Detects inserting of removable devices with their port.  
at start detects all devices, has listeners to detect new devices

```java
UsbDetector detector = UsbDetector.getInstance();

detector.addEventListener(event -> {
	print(event.getType()); //can be CONNECTED/REMOVED
	print(event.getRootDirectory()); // prints root directory of device
	print(event.getPort()); //print physical USB port which this device have been inserted. Each port is uniq
});

detector.start();

detector.forceEvents(); //should be called if needed to recognize all inserted devices (REMOVED event still be caught)
```


supported systems: Windows, Mac OS X