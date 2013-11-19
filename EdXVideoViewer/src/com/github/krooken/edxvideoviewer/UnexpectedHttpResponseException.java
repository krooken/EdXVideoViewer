package com.github.krooken.edxvideoviewer;

import org.apache.http.Header;

public class UnexpectedHttpResponseException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String requestUrl;
	private Header[] responseHeaders;
	private String httpResponse;

	public UnexpectedHttpResponseException() {
		super();
	}

	public UnexpectedHttpResponseException(String detailMessage) {
		super(detailMessage);
	}

	public UnexpectedHttpResponseException(Throwable throwable) {
		super(throwable);
	}

	public UnexpectedHttpResponseException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}
	
	public void setRequestUrl(String url) {
		requestUrl = url;
	}
	
	public void setResponseHeader(Header[] headers) {
		responseHeaders = headers;
	}
	
	public void setHttpResponse(String response) {
		httpResponse = response;
	}
	
	public String getRequestUrl() {
		if (requestUrl != null) {
			return requestUrl;
		}else {
			return "null";
		}
	}
	
	public String getFormattedHeaders() {
		String ret = "";
		if(responseHeaders != null) {
			for(int i=0; i<responseHeaders.length; i++) {
				ret += responseHeaders[i].getName();
				ret += ": ";
				ret += responseHeaders[i].getValue();
				ret += "\n";
			}
			return ret;
		}else {
			return "null";
		}
	}
	
	public String getResponse() {
		if(httpResponse != null) {
			return httpResponse;
		}else {
			return "null";
		}
	}

}
