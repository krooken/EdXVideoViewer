package com.github.krooken.edxvideoviewer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;

public class UncaughtExceptionLogger implements UncaughtExceptionHandler {
	
	private String logFolder;
	private UncaughtExceptionHandler defaultHandler;
	
	public UncaughtExceptionLogger(String logFolder, UncaughtExceptionHandler defaultHandler) {
		this.logFolder = logFolder;
		this.defaultHandler = defaultHandler;
	}

	@Override
	public void uncaughtException(Thread thread, Throwable exception) {
		
		String timestamp = System.currentTimeMillis() + "";
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(
					logFolder + "/" + timestamp + ".stacktrace"));
			PrintWriter printer = new PrintWriter(writer);
			exception.printStackTrace(printer);
			writer.flush();
			writer.close();
			printer.close();
		} catch(IOException ex) {
			ex.printStackTrace();
		}
		
		defaultHandler.uncaughtException(thread, exception);
	}

}
