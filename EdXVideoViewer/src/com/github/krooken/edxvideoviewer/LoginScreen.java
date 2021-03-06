package com.github.krooken.edxvideoviewer;

import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;

import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class LoginScreen extends Activity {
	
	private static String TAG = "LoginScreenActivity";
	
	private final Handler handler = new Handler();
	private boolean loginInProgress = false;
	private boolean startupInProgress = true;

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
		
		Button loginButton = (Button)findViewById(R.id.login_button);
		
		loginButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(!loginInProgress) {
					String username = ((EditText)findViewById(R.id.username_field)).getText().toString();
					String password = ((EditText)findViewById(R.id.password_field)).getText().toString();
					boolean remember = ((CheckBox)findViewById(R.id.remember_field)).isChecked();
					tryLogin(username, password, remember);
				}
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		
		Thread alreadyLoggedInThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				String sessionId = prefs.getString("session_cookie", null);
				String edxLoggedIn = prefs.getString("edxloggedin_cookie", null);
				
				boolean loggedIn = false;
				
				if(sessionId != null && edxLoggedIn != null) {
					HttpGetRequest httpRequest = null;
					try {
						httpRequest = new HttpGetRequest(new URI(
								"https://courses.edx.org/dashboard"));
					} catch (URISyntaxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					httpRequest.addCookieHeader(sessionId);
					httpRequest.addCookieHeader(edxLoggedIn);
					
					httpRequest.executeGetRequest();
					
					if(httpRequest.getStatusCode() == 200) {
						loggedIn = true;
					} else {
						loggedIn = false;
					}
				} else {
					loggedIn = false;
				}
				
				if(loggedIn) {
					handler.post(new Runnable() {
						public void run() {
							alreadyLoggedIn();
						}
					});
				} else {
					handler.post(new Runnable() {
						
						@Override
						public void run() {
							needToLogIn();
						}
					});
				}
			}
		});
		alreadyLoggedInThread.start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.login_screen, menu);
		return true;
	}
	
	private void tryLogin(String username, String password, boolean remember) {

		final String postUsername = username;
		final String postPassword = password;
		final boolean postRemember = remember;
		
		Thread thread = new Thread(new Runnable(){

			@Override
			public void run() {
				HttpGetRequest httpRequest = null;
				try {
					httpRequest = new HttpGetRequest(new URI(
							"https://courses.edx.org/accounts/login?next=/dashboard"));
				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				httpRequest.executeGetRequest();
				Header[] headers = httpRequest.getResponseHeaders();
				
				String csrfToken = null;
				
				for(int i=0; i<headers.length; i++) {
					
					if(headers[i].getName().equals("Set-Cookie")) {
						String cookieString = headers[i].getValue();
						Pattern pattern = Pattern.compile("csrftoken=([^;]+);");
						Matcher matcher = pattern.matcher(cookieString);
						if(matcher.find()) {
							csrfToken = matcher.group(1);
							Log.d(TAG, "csrftoken: " + csrfToken);
						}
					}
				}
				
				if(csrfToken == null) {
					handler.post(new Runnable() {
						
						@Override
						public void run() {
							loginFailed("Cannot send login information");
						}
					});
					return;
				}
				
				HttpGetRequest loginRequest = null;
				try {
					loginRequest = new HttpGetRequest(new URI("https://courses.edx.org/login_ajax"));
				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				loginRequest.addCookieHeader("csrftoken=" + csrfToken);
				loginRequest.setReferer(
						"https://courses.edx.org/accounts/login?next=/dashboard");
				loginRequest.setXCsrfToken(csrfToken);
				loginRequest.addPostData("email", postUsername);
				loginRequest.addPostData("password", postPassword);
				if(postRemember) {
					loginRequest.addPostData("remember", "true");
				}
				loginRequest.executePostRequest();
				
				headers = loginRequest.getResponseHeaders();
				String sessioncookie = null;
				String edxloggedincookie = null;
				
				for(int i=0; i<headers.length; i++) {
					
					if(headers[i].getName().equals("Set-Cookie")) {
						String cookieString = headers[i].getValue();
						Pattern edxLoggedInPattern = Pattern.compile("edxloggedin=[^;]+;");
						Pattern sessionIdPattern = Pattern.compile("sessionid=[^;]+;");
						Matcher edxLoggedInMatcher = edxLoggedInPattern.matcher(cookieString);
						Matcher sessionIdMatcher = sessionIdPattern.matcher(cookieString);
						
						if(edxLoggedInMatcher.find()) {
							edxloggedincookie = edxLoggedInMatcher.group(0);
							Log.d(TAG, "EdxLoggedInCookie: " + edxloggedincookie);
						}
						if(sessionIdMatcher.find()) {
							sessioncookie = sessionIdMatcher.group(0);
							Log.d(TAG, "SessionIdCookie: " + sessioncookie);
						}
					}
				}
				
				if(sessioncookie != null 
						&& edxloggedincookie != null 
						&& edxloggedincookie.equalsIgnoreCase("edxloggedin=true;")) {
					
					final String session = sessioncookie;
					final String edxloggedin = edxloggedincookie;
					
					handler.post(new Runnable() {
						public void run() {
							loginSuccessful(session, edxloggedin);
						}
					});
				} else {
					handler.post(new Runnable() {
						
						@Override
						public void run() {
							loginFailed("Username or password is incorrect!");
						}
					});
				}
			}
			
		});
		
		loginInProgress = true;
		((Button)findViewById(R.id.login_button)).setEnabled(false);
		((TextView)findViewById(R.id.login_error_message_text)).setVisibility(View.GONE);
		((ProgressBar)findViewById(R.id.login_in_progress_bar)).setVisibility(View.VISIBLE);
		thread.start();
	}
	
	private void loginSuccessful(String sessionId, String edxLoggedIn) {
		SharedPreferences.Editor prefsEditor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
		prefsEditor.putString("session_cookie", sessionId);
		prefsEditor.putString("edxloggedin_cookie", edxLoggedIn);
		prefsEditor.commit();
		
		String cookieString = sessionId + edxLoggedIn;
		leaveLoginActivity(cookieString);
		loginProgressStop();
	}
	
	private void loginFailed(String errorMessage) {
		TextView errorMessageView = (TextView)findViewById(R.id.login_error_message_text);
		errorMessageView.setText(errorMessage);
		loginProgressStop();
		((TextView)findViewById(R.id.login_error_message_text)).setVisibility(View.VISIBLE);
	}
	
	private void loginProgressStop() {
		loginInProgress = false;
		((Button)findViewById(R.id.login_button)).setEnabled(true);
		((ProgressBar)findViewById(R.id.login_in_progress_bar)).setVisibility(View.GONE);
	}
	
	private void alreadyLoggedIn() {
		SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
		String cookies = prefs.getString("session_cookie", "") +
				prefs.getString("edxloggedin_cookie", "");
		leaveLoginActivity(cookies);
	}
	
	private void needToLogIn() {
		((ProgressBar)findViewById(R.id.already_logged_in_progress_bar)).setVisibility(View.GONE);
		((RelativeLayout)findViewById(R.id.login_form_container)).setVisibility(View.VISIBLE);
		startupInProgress = false;
	}
	
	private void leaveLoginActivity(String cookies) {
		Intent intent = new Intent(LoginScreen.this, CourseViewer.class);
		intent.putExtra("cookie_data", cookies);
		startActivity(intent);
	}

}
