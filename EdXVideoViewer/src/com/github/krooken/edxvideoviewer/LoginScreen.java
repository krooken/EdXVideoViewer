package com.github.krooken.edxvideoviewer;

import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

public class LoginScreen extends Activity {
	
	private static String TAG = "LoginScreenActivity";

	private boolean userDetailsSubmitted = false;
	private boolean pageHasLoaded = false;
	private WebView loginWebView;
	private String userName;
	private String password;
	private String cookieData = "";
	private boolean remember;

	private boolean dataAdded = false;

	private boolean loggedIn = false;
	
	private Button continueButton;

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login_screen);
		
		if(!(Thread.getDefaultUncaughtExceptionHandler() instanceof UncaughtExceptionLogger)) {
			UncaughtExceptionHandler defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
			File file = new File(getExternalFilesDir(null), "log-files");
			file.mkdir();
			Thread.setDefaultUncaughtExceptionHandler(
					new UncaughtExceptionLogger(file.getAbsolutePath(), defaultUEH));
		}
		
		loginWebView = (WebView)findViewById(R.id.login_web_view);
		
		CookieManager cookieMgr = CookieManager.getInstance();
		String cookieMgrStr = "";
		if(cookieMgr == null) {
			cookieMgrStr = "null";
		}else{
			String rawCookieHeader = cookieMgr.getCookie("courses.edx.org");
			if(rawCookieHeader == null) {
				cookieMgrStr = "no cookie data!";
			}else {
				cookieMgrStr = rawCookieHeader;
			}
		}
		
		Log.d(TAG, "CookieMgr before load: " + cookieMgrStr);
		
		loginWebView.getSettings().setJavaScriptEnabled(true);
		loginWebView.getSettings().setLoadsImagesAutomatically(false);
		loginWebView.loadUrl("https://courses.edx.org/login");
		getWindow().setSoftInputMode(
			      WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		
		loginWebView.setWebViewClient(new WebViewClient() {

			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				
				Log.d(TAG,"onPageFinished(): Loaded completed!");
				
				pageHasLoaded = true;
				tryAddDataToForm();
				
				CookieManager cookieMgr = CookieManager.getInstance();
				String cookieMgrStr = "";
				if(cookieMgr == null) {
					cookieMgrStr = "null";
				}else{
					String rawCookieHeader = cookieMgr.getCookie("courses.edx.org");
					if(rawCookieHeader == null) {
						cookieMgrStr = "no cookie data!";
					}else {
						cookieMgrStr = rawCookieHeader;
					}
				}
				
				Log.d(TAG, "CookieMgr after load: " + cookieMgrStr);
				
				CookieManager mgr = CookieManager.getInstance();
				String cookies = mgr.getCookie("courses.edx.org");
				cookieData = cookies;
				Pattern edxLoggedInPattern = Pattern.compile("edxloggedin=([^;]+);");
				Pattern sessionIdPattern = Pattern.compile("sessionid=([^;]+);");
				Matcher edxLoggedInMatcher = edxLoggedInPattern.matcher(cookies);
				Matcher sessionIdMatcher = sessionIdPattern.matcher(cookies);
				if(edxLoggedInMatcher.find()) {
					Log.d(TAG, edxLoggedInMatcher.group(1));
					if(edxLoggedInMatcher.group(1).equalsIgnoreCase("true")) {
						loggedIn = true;
						if(continueButton != null) {
							continueButton.setEnabled(true);
						}
					} else {
						if(continueButton != null) {
							continueButton.setEnabled(false);
						}
					}
				}
				if(sessionIdMatcher.find()) {
					Log.d(TAG, sessionIdMatcher.group(1));
				}
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				Log.d(TAG,"ShouldOverride returns false.");
				
				return false;
			}
			
		});
		
		Button addDataToFormButton = (Button)findViewById(R.id.add_data_button);
		Button sendDataButton = (Button)findViewById(R.id.send_data_button);
		continueButton = (Button)findViewById(R.id.continue_button);
		
		addDataToFormButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				userName = ((EditText)findViewById(R.id.username_field)).getText().toString();
				password = ((EditText)findViewById(R.id.password_field)).getText().toString();
				remember = ((CheckBox)findViewById(R.id.remember_field)).isChecked();
				
				userDetailsSubmitted = true;
				tryAddDataToForm();
			}
		});
		
		sendDataButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				tryLoginFromPrefilledForm();
			}
		});
		
		continueButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//TODO: Start new activity
				if(loggedIn) {
					Intent intent = new Intent(LoginScreen.this, CourseViewer.class);
					intent.putExtra("cookie_data", cookieData);
					startActivity(intent);
				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.login_screen, menu);
		return true;
	}
	
	private boolean isLoginReady() {
		return userDetailsSubmitted && pageHasLoaded;
	}
	
	private void tryLogin() {
		if(isLoginReady()) {
			loginWebView.loadUrl("javascript:" + 
					"(function() {" +
					"document.getElementById('email').value = '" + userName + "';" +
					"document.getElementById('password').value = '" + password + "';" +
					"document.getElementById('remember-yes').checked = " + (remember ? "'true'" : "''") + ";" +
					"document.getElementById('submit').click();" + 
					"})()");
		}
	}
	
	private void tryAddDataToForm() {
		
		Log.d(TAG, "tryAddDataToForm(): submitted: " + userDetailsSubmitted + " loaded: " + pageHasLoaded);
		
		if(isLoginReady()) {
			loginWebView.loadUrl("javascript:" + 
					"(function() {" +
					"document.getElementById('email').value = '" + userName + "';" +
					"document.getElementById('password').value = '" + password + "';" +
					"document.getElementById('remember-yes').checked = " + (remember ? "'true'" : "''") + ";" +
					"})()");
			
			dataAdded  = true;
		}
	}
	
	private void tryLoginFromPrefilledForm() {
		if(dataAdded) {
			loginWebView.loadUrl("javascript:" + 
					"(function() {" + 
					"document.getElementById('submit').click();" +
					"})()");
		}
	}

}
