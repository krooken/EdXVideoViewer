package com.github.krooken.edxvideoviewer;

import java.net.URI;
import java.net.URISyntaxException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ArrayAdapter;

public class CoursewareContentViewer extends Activity {

	private ArrayAdapter<String> adapter;
	private String cookieData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.course_viewer_screen);

		adapter = new ArrayAdapter<String>(this, R.layout.course_viewer_item);

		Intent intent = getIntent();
		cookieData = intent.getCharSequenceExtra("cookie_data").toString();
		String courseAddress = intent.getCharSequenceExtra("course_info_address").toString();
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
	}

}
