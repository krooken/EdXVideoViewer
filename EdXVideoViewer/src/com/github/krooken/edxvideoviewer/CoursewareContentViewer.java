package com.github.krooken.edxvideoviewer;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

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
				String responseText = getRequest.executeGetRequest();
				
				Log.d(TAG, "Content: " + responseText);
				
				Pattern startTagPattern = Pattern.compile("<section[^<>]*class=\"course-index\">");
				Matcher startTagMatcher = startTagPattern.matcher(responseText);
				startTagMatcher.find();
				Log.d(TAG, "Start match: " + startTagMatcher.group());
				Log.d(TAG, "Position: " + startTagMatcher.end());
				Pattern endTagPattern = Pattern.compile("</section>");
				Matcher endTagMatcher = endTagPattern.matcher(responseText);
				endTagMatcher.find(startTagMatcher.end());
				Log.d(TAG, "End match: " + endTagMatcher.group());
				Log.d(TAG, "Position: " + endTagMatcher.start());
				
				String coursewareContents = responseText.substring(startTagMatcher.end(), endTagMatcher.start());
				Log.d(TAG, coursewareContents);
				
				try {
					XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
					XmlPullParser xpp = factory.newPullParser();

					xpp.setInput(new StringReader(coursewareContents));
					while(xpp.next() != XmlPullParser.END_DOCUMENT) {
						if(xpp.getEventType() == XmlPullParser.START_TAG) {
							if(xpp.getName().equals("div")) {
								if(xpp.getAttributeValue(null, "class").equals("chapter")) {
									while(xpp.getEventType() != XmlPullParser.TEXT) {
										xpp.next();
									}
									Log.d(TAG, xpp.getText());
								}
							}
						}
					}
				} catch (XmlPullParserException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				finally {
					
				}
			}
		});
		
		headerThread.start();
	}

}
