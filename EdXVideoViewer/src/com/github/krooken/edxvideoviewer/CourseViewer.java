package com.github.krooken.edxvideoviewer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class CourseViewer extends Activity {
	
	static String TAG = "CourseViewer";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent = getIntent();
		String cookieData = intent.getCharSequenceExtra("cookie_data").toString();
		HttpGetRequest getRequest = null;
		try {
			getRequest = new HttpGetRequest(new URI("https://courses.edx.org/dashboard"));
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		getRequest.addCookieHeader(cookieData);
		
		final HttpGetRequest request = getRequest;
		
		final Handler responseHandler = new Handler();
		
		Thread getRequestThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				HttpGetRequest req = request;
				
				final String response = req.executeGetRequest();
				responseHandler.post(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						Log.d(TAG, response);
						Pattern pattern = Pattern.compile("<h3>\\s*<a href=\"([^<\"]+)\">([^<]+)</a>\\s*</h3>");
						Matcher matcher = pattern.matcher(response);
						while(matcher.find()) {
							Log.d(TAG, matcher.group(1));
							Log.d(TAG,  matcher.group(2));
						}
					}
				});
			}
		});
		
		getRequestThread.start();
	}
	
}
