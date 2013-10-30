package com.github.krooken.edxvideoviewer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class CourseViewer extends Activity {
	
	private static final String TAG = "CourseViewer";
	
	private ArrayAdapter<String> adapter;
	private LinkedList<String> courseAddresses = new LinkedList<String>();
	private String cookieData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.course_viewer_screen);
		
		adapter = new ArrayAdapter<String>(this, R.layout.course_viewer_item);
		
		Intent intent = getIntent();
		cookieData = intent.getCharSequenceExtra("cookie_data").toString();
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
							adapter.add(matcher.group(2) + "\n" + matcher.group(1));
							courseAddresses.add(matcher.group(1));
						}
						ListView listView = (ListView)findViewById(R.id.courses_list_view);
						listView.setAdapter(adapter);
						listView.setOnItemClickListener(new OnItemClickListener() {

							@Override
							public void onItemClick(AdapterView<?> parent,
									View v, int position, long id) {
								// TODO Auto-generated method stub
								Log.d(TAG, "Adapter: position: " + position + " id: " + id);
								Log.d(TAG, "Selected address: " + courseAddresses.get(position));
								Intent intent = new Intent(CourseViewer.this, null);
								intent.putExtra("cookie_data", cookieData);
								intent.putExtra("course_info_address", courseAddresses.get(position));
								//startActivity(intent);
							}
						});
					}
				});
			}
		});
		
		getRequestThread.start();
	}
	
}
