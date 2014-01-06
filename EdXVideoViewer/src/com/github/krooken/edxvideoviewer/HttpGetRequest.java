package com.github.krooken.edxvideoviewer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.util.LinkedList;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.util.Log;

public class HttpGetRequest {
	
	private static final String TAG = "HttpGetRequest";
	private URI getRequestUri;
	private String cookieHeader = "";
	private Header[] responseHeaders;
	private String responseContent;
	private LinkedList<BasicNameValuePair> postData = 
			new LinkedList<BasicNameValuePair>();
	private String referer = "";
	private String xCsrfToken = "";
	private int statusCode = -1;

	public HttpGetRequest(URI uri) {
		getRequestUri = uri;
	}
	
	public void addCookieHeader(String cookieString) {
		cookieHeader += cookieString;
	}
	
	public void addPostData(String name, String value) {
		postData.add(new BasicNameValuePair(name, value));
	}
	
	public void setReferer(String referer) {
		this.referer = referer;
	}
	
	public void setXCsrfToken(String csrfToken) {
		this.xCsrfToken = csrfToken;
	}
	
	private String executeRequest(String method) {
		Log.d(TAG, "Requesting " + getRequestUri.toString());
		
		HttpResponse response = null;
		try {        
			HttpClient client = new DefaultHttpClient();
			HttpRequestBase request;
			if(method.trim().equalsIgnoreCase("post")) {
				UrlEncodedFormEntity entity = new UrlEncodedFormEntity(postData);
				HttpPost postRequest = new HttpPost();
				postRequest.setEntity(entity);
				postRequest.addHeader("Referer", referer);
				postRequest.addHeader("X-CSRFToken", xCsrfToken);
				postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");
				request = postRequest;
			} else {
				// Method get is default.
				request = new HttpGet();
			}
			request.setURI(getRequestUri);
			request.addHeader("Cookie", cookieHeader);
			response = client.execute(request);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if(response != null) {
			
			Log.d(TAG, "Status code: " + response.getStatusLine().getStatusCode()
					+ " " + response.getStatusLine().getReasonPhrase());
			statusCode = response.getStatusLine().getStatusCode();
			
			responseHeaders = response.getAllHeaders();
			for(int i=0; i<responseHeaders.length; i++) {
				Log.d(TAG, "Header name: " + responseHeaders[i].getName() + 
						" value: " + responseHeaders[i].getValue());
			}
		}
		
		InputStream responseStream = null;
		try {
			responseStream = response.getEntity().getContent();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if(responseStream != null) {
			Writer writer = new StringWriter();

			char[] buffer = new char[1024];
			try {
				Reader reader = new BufferedReader(new InputStreamReader(responseStream, "UTF-8"),1024);
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					responseStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			responseContent = writer.toString();
		}
		else {
			responseContent = "";
		}
		
		if(response != null) {
			try {
				response.getEntity().consumeContent();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return responseContent;
	}
	
	public String executeGetRequest() {
		return executeRequest("GET");
	}
	
	public String executePostRequest() {
		return executeRequest("POST");
	}
	
	public Header[] getResponseHeaders() {
		return responseHeaders;
	}
	
	public String getResponseContent() {
		return responseContent;
	}
	
	public int getStatusCode() {
		return statusCode;
	}
}
