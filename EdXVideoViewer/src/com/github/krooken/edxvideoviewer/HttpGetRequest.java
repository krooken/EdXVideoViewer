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

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

public class HttpGetRequest {
	
	private URI getRequestUri;
	private String cookieHeader = "";
	private Header[] responseHeaders;
	private String responseContent;

	public HttpGetRequest(URI uri) {
		getRequestUri = uri;
	}
	
	
	public void addCookieHeader(String cookieString) {
		cookieHeader = cookieString;
	}
	
	public String executeGetRequest() {
		HttpResponse response = null;
		try {        
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet();
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
			responseHeaders = response.getAllHeaders();
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
	
	public Header[] getResponseHeaders() {
		return responseHeaders;
	}
	
	public String getResponseContent() {
		return responseContent;
	}
}
