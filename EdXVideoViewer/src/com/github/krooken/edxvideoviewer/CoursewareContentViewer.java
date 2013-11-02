package com.github.krooken.edxvideoviewer;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.Header;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ArrayAdapter;

public class CoursewareContentViewer extends Activity {

	private ArrayAdapter<String> adapter;
	private String cookieData;
	
	private static final String TAG = "CoursewareContentViewer";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.course_viewer_screen);

		adapter = new ArrayAdapter<String>(this, R.layout.course_viewer_item);

		Intent intent = getIntent();
		cookieData = intent.getCharSequenceExtra("cookie_data").toString();
		String courseAddress = intent.getCharSequenceExtra("course_info_address").toString();
		courseAddress = courseAddress.replace("/info", "/courseware");
		HttpGetRequest getRequest = null;
		try {
			getRequest = new HttpGetRequest(new URI("https://courses.edx.org" + courseAddress));
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		getRequest.addCookieHeader(cookieData);

		final HttpGetRequest request = getRequest;

		final Handler responseHandler = new Handler();
		
		Thread headerThread = new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				HttpGetRequest getRequest = request;
				request.executeGetRequest();
				Header[] responseHeaders = request.getResponseHeaders();
				for(int i=0; i<responseHeaders.length; i++) {
					Log.d(TAG, "Header name: " + responseHeaders[i].getName() + 
							" value: " + responseHeaders[i].getValue());
				}
			}
		});
		
		headerThread.start();
	}

}
