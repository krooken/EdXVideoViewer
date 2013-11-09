package com.github.krooken.edxvideoviewer;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class CoursewareContentViewer extends Activity {

	private ArrayAdapter<String> adapter;
	private String cookieData;
	
	private static final String TAG = "CoursewareContentViewer";
	
	private String[] sectionTexts = new String[1];
	private String[] sectionAddresses = new String[1];

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
				
				LinkedList<String> sectionStrings = new LinkedList<String>();
				LinkedList<String> sectionAddresses = new LinkedList<String>();
				
				try {
					XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
					XmlPullParser xpp = factory.newPullParser();

					xpp.setInput(new StringReader(coursewareContents));
					while(xpp.next() != XmlPullParser.END_DOCUMENT) {
						if(xpp.getEventType() == XmlPullParser.START_TAG) {
							if(xpp.getName().equals("div")) { // Find div-tag.
								//Log.d(TAG, xpp.getName());
								//Log.d(TAG, "" + xpp.getAttributeCount());
								/*for(int i=0; i<xpp.getAttributeCount(); i++) {
									Log.d(TAG, xpp.getAttributeName(i));
									Log.d(TAG, xpp.getAttributeValue(i));
								}*/
								// Find the div with class attribute chapter.
								String attributeClassValue = xpp.getAttributeValue(null, "class");
								if(attributeClassValue != null && attributeClassValue.equals("chapter")) {
									// Find the first non-whitespace text after the opening div-tag.
									// This text is the chapter description text.
									while(xpp.getEventType() != XmlPullParser.TEXT || xpp.isWhitespace()) {
										xpp.next();
									}
									Log.d(TAG, xpp.getText().trim());
									sectionStrings.add(xpp.getText());
									sectionAddresses.add(null);
									
									// Find the ul-tag. All class info is organized in li-tags under the ul-tag.
									// Find all li-tags in the ul-tag.
									while(xpp.getName() == null || !xpp.getName().equals("ul")) {
										xpp.next();
									}
									while( !(xpp.getEventType() == XmlPullParser.END_TAG && 
											xpp.getName().equals("ul"))) {
										if(xpp.getEventType() == XmlPullParser.START_TAG && 
												xpp.getName().equals("a")) {
											Log.d(TAG, xpp.getAttributeValue(null, "href"));
											String sectionAddress = xpp.getAttributeValue(null, "href");
											
											while(xpp.getEventType() != XmlPullParser.TEXT ||
													xpp.isWhitespace()) {
												xpp.next();
											}
											Log.d(TAG, xpp.getText());
											sectionAddresses.add(sectionAddress);
											sectionStrings.add(xpp.getText() + "\n" + sectionAddress);
										}
										xpp.next();
									}
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
				
				final LinkedList<String> finalSectionTexts = sectionStrings;
				final LinkedList<String> finalSectionAddresses = sectionAddresses;
				
				responseHandler.post(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						CoursewareContentViewer.this.sectionAddresses = (String[])finalSectionAddresses.toArray(CoursewareContentViewer.this.sectionAddresses);
						CoursewareContentViewer.this.sectionTexts = (String[])finalSectionTexts.toArray(CoursewareContentViewer.this.sectionTexts);
						
						addSectionContents();
					}
				});
			}
		});
		
		headerThread.start();
	}
	
	private void addSectionContents() {
		
		for(int i=0; i<sectionTexts.length; i++) {
			adapter.add(sectionTexts[i]);
		}
		
		ListView listView = (ListView)findViewById(R.id.courses_list_view);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position,
					long id) {
				// TODO Auto-generated method stub
				Toast.makeText(CoursewareContentViewer.this, sectionTexts[position], Toast.LENGTH_SHORT).show();
			}
		});
	}

}
