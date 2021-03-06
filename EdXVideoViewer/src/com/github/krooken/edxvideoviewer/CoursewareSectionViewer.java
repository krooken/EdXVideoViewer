package com.github.krooken.edxvideoviewer;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class CoursewareSectionViewer extends Activity {

	private ArrayAdapter<String> adapter;
	private String cookieData;
	
	private static final String TAG = "CoursewareSectionViewer";
	
	private String[] videoTexts = new String[0];
	private String[] videoAddresses = new String[0];

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.course_viewer_screen);

		adapter = new ArrayAdapter<String>(this, R.layout.course_viewer_item);

		Intent intent = getIntent();
		cookieData = intent.getCharSequenceExtra("cookie_data").toString();
		String courseAddress = intent.getCharSequenceExtra("section_contents_address").toString();
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
		
		final String exceptionAddress = courseAddress;
		
		Thread headerThread = new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				HttpGetRequest getRequest = request;
				String responseText = getRequest.executeGetRequest();
				
				Log.d(TAG, "Content: " + responseText);
				
				Pattern startTagPattern = Pattern.compile("<ol[^<>]*id=\"sequence-list\">");
				Matcher startTagMatcher = startTagPattern.matcher(responseText);
				startTagMatcher.find();
				Log.d(TAG, "Start match: " + startTagMatcher.group());
				Log.d(TAG, "Position: " + startTagMatcher.end());
				Pattern endTagPattern = Pattern.compile("</ol>");
				Matcher endTagMatcher = endTagPattern.matcher(responseText);
				endTagMatcher.find(startTagMatcher.end());
				Log.d(TAG, "End match: " + endTagMatcher.group());
				Log.d(TAG, "Position: " + endTagMatcher.start());
				
				String sectionContents = responseText.substring(startTagMatcher.end(), endTagMatcher.start());
				Log.d(TAG, sectionContents);
				
				LinkedList<String> videoStrings = new LinkedList<String>();
				LinkedList<String> videoDataId = new LinkedList<String>();
				LinkedList<String> videoAddresses = new LinkedList<String>();
				
				try {
					XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
					XmlPullParser xpp = factory.newPullParser();

					xpp.setInput(new StringReader(sectionContents.replaceAll("&", "&amp;")));
					while(xpp.next() != XmlPullParser.END_DOCUMENT) {
						if(xpp.getEventType() == XmlPullParser.START_TAG) {
							if(xpp.getName().equals("li")) { // Find li-tag.{

								// Find the a-tag. All class info is organized in a-tags under the li-tag.
								// Find all a-tags in the li-tags.
								while(xpp.getName() == null || !xpp.getName().equals("a")) {
									xpp.next();
								}
								
								// Get the data id for this video.
								String dataId = xpp.getAttributeValue(null, "data-id");
								Log.d(TAG, "data-id: " + dataId);
								
								// Get the title for this video.
								String videoDescription = xpp.getAttributeValue(null, "data-page-title");
								Log.d(TAG, "title: " + videoDescription);
								
								// Get all class names as a space separated string
								String aClassAttributesString = xpp.getAttributeValue(null, "class");
								Log.d(TAG, "class: " + aClassAttributesString);
								String[] aClassAttributes = aClassAttributesString.split(" ");
								// Search the class attributes for seq_video.
								// This is our indicator of a video segment.
								for(int i=0; i<aClassAttributes.length; i++) {
									if(aClassAttributes[i].equals("seq_video")) {
										videoStrings.add(videoDescription);
										videoDataId.add(dataId);
									}
								}
							}

						}
					}

					Iterator<String> it = videoDataId.iterator();
					while(it.hasNext()) {
						try {
							String currentVideoDataId = it.next();
							Pattern videoDataIdPattern = 
									Pattern.compile(currentVideoDataId.replace("/", ";_"));
							Matcher videoDataIdMatcher = videoDataIdPattern.matcher(responseText);
							videoDataIdMatcher.find();
							Log.d(TAG, "Data id match: " + videoDataIdMatcher.group());
							Log.d(TAG, "Position: " + videoDataIdMatcher.end());
							Pattern dataStreamsPattern = Pattern.compile("data-streams=&#34;");
							Matcher dataStreamsMatcher = dataStreamsPattern.matcher(responseText);
							dataStreamsMatcher.find(videoDataIdMatcher.end());
							Log.d(TAG, "Data stream match: " + dataStreamsMatcher.group());
							Log.d(TAG, "Position: " + dataStreamsMatcher.start());
							Pattern regularSpeedPattern = Pattern.compile("1[.]00:([^,&]*)");
							Matcher regularSpeedMatcher = regularSpeedPattern.matcher(responseText);
							regularSpeedMatcher.find(dataStreamsMatcher.end());
							Log.d(TAG, "Video link match: " + regularSpeedMatcher.group());
							Log.d(TAG, "Position: " + regularSpeedMatcher.start());
							videoAddresses.add(regularSpeedMatcher.group(1));
						}catch(IllegalStateException exception) {
							UnexpectedHttpResponseException uhrException = 
									new UnexpectedHttpResponseException("Data stream not found.", exception);
							uhrException.setRequestUrl(exceptionAddress);
							uhrException.setResponseHeader(getRequest.getResponseHeaders());
							uhrException.setHttpResponse(getRequest.getResponseContent());
							throw uhrException;
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
				
				final LinkedList<String> finalVideoTexts = videoStrings;
				final LinkedList<String> finalVideoAddresses = videoAddresses;
				
				responseHandler.post(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						CoursewareSectionViewer.this.videoAddresses = (String[])finalVideoAddresses.toArray(CoursewareSectionViewer.this.videoAddresses);
						CoursewareSectionViewer.this.videoTexts = (String[])finalVideoTexts.toArray(CoursewareSectionViewer.this.videoTexts);
						
						addVideoContents();
					}
				});
			}
		});
		
		headerThread.start();
	}
	
	private void addVideoContents() {
		
		for(int i=0; i<videoTexts.length; i++) {
			adapter.add(videoTexts[i]);
			if(videoTexts[i] != null) {
				Log.d(TAG, "videoTexts[" + i + "]: " + videoTexts[i]);
				Log.d(TAG, "videoAddresses[" + i + "]: " + videoAddresses[i]);
			}else {
				Log.d(TAG, "videoTexts[" + i + "]: null");
				Log.d(TAG, "videoAddresses[" + i + "]: null");
			}
		}
		
		if(videoTexts.length == 0) {
			adapter.add("No videos found in this section");
		}
		
		ListView listView = (ListView)findViewById(R.id.courses_list_view);
		listView.setAdapter(adapter);
		
		if(videoAddresses.length > 0) {
			listView.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View v, int position,
						long id) {
					// Toast.makeText(CoursewareSectionViewer.this, videoTexts[position] + "\n" + videoAddresses[position], Toast.LENGTH_SHORT).show();
					startActivity(new Intent(Intent.ACTION_VIEW, 
							Uri.parse("http://www.youtube.com/watch?v=" + videoAddresses[position])));
				}
			});
		}
	}

}

