package com.github.krooken.edxvideoviewer;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
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
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.SimpleExpandableListAdapter;

public class CoursewareContentViewer extends Activity {

	private SimpleExpandableListAdapter expandableAdapter;
	private String cookieData;
	
	private static final String TAG = "CoursewareContentViewer";
	
	private LinkedList<Map<String, String>> sectionGroups;
	private LinkedList<LinkedList<Map<String, String>>> sectionChildren;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.course_content_viewer_screen);

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
				
				LinkedList<Map<String, String>> sectionGroups = 
						new LinkedList<Map<String, String>>();
				LinkedList<LinkedList<Map<String, String>>> sectionChildren =
						new LinkedList<LinkedList<Map<String, String>>>();
				
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
									
									HashMap<String, String> groupMap = new HashMap<String, String>();
									groupMap.put("text", xpp.getText());
									
									LinkedList<Map<String, String>> childrenList = 
											new LinkedList<Map<String, String>>();
									
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
											
											HashMap<String, String> childrenMap = 
													new HashMap<String, String>();
											
											childrenMap.put("text", xpp.getText() + "\n" + sectionAddress);
											childrenMap.put("address", sectionAddress);
											
											childrenList.add(childrenMap);
										}
										xpp.next();
									}
									
									sectionGroups.add(groupMap);
									sectionChildren.add(childrenList);
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
				
				final LinkedList<Map<String, String>> sectionGroupsTemp = sectionGroups;
				final LinkedList<LinkedList<Map<String, String>>> sectionChildrenTemp = sectionChildren;
				
				responseHandler.post(new Runnable() {
					
					@Override
					public void run() {
						
						CoursewareContentViewer.this.sectionGroups = sectionGroupsTemp;
						CoursewareContentViewer.this.sectionChildren = sectionChildrenTemp;
						
						addSectionContents();
					}
				});
			}
		});
		
		headerThread.start();
	}
	
	private void addSectionContents() {
		
		String[] groupFrom = {"text"};
		int groupLayout = R.layout.course_viewer_item;
		int[] groupTo = {R.id.course_viewer_item_text};
		String[] childFrom = groupFrom;
		int childLayout = groupLayout;
		int[] childTo = groupTo;
		expandableAdapter = new SimpleExpandableListAdapter(this, sectionGroups, groupLayout, groupFrom, groupTo, sectionChildren, childLayout, childFrom, childTo);
		
		ExpandableListView listView = (ExpandableListView)findViewById(R.id.content_list_view);
		listView.setAdapter(expandableAdapter);
		listView.setOnChildClickListener(new OnChildClickListener() {
			
			@Override
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {
				
				Log.d(TAG, "Adapter: group position: " + groupPosition + 
						"child position: " + childPosition + " id: " + id);
				if(sectionChildren.get(groupPosition).get(childPosition) != null) {
					Intent intent = new Intent(CoursewareContentViewer.this, CoursewareSectionViewer.class);
					intent.putExtra("cookie_data", cookieData);
					intent.putExtra("section_contents_address", 
							sectionChildren.get(groupPosition).get(childPosition).get("address"));
					startActivity(intent);
				}
				
				return true;
			}
		});
	}

}
