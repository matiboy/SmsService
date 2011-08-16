package com.zest.smsservice;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.zest.smsservice.R;
import com.zest.smsservice.Message;
import com.zest.smsservice.SmsService.SmsserviceBinder;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/*
 * This is the UI of the program smsservice
 * 
 * This program gets in the DB the SMS that need to be sent
 * 
 * Here, the user enter the username and password
 * The phone must be connected to mobile network and can also be connected
 * to wifi. If no mobile network available, the sms will not be sent
 * Once the service (class SmsService) is started, it works in Background
 * and executes itself every 10 minutes
 */

public class UIsmsservice extends Activity {
	public SmsService smsservice;
	private SmsService mService = null;
    /** Flag indicating whether we have called bind on the service. */
    boolean mBound;
	private Intent serviceintent;
	public BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals("com.zest.smsservice.action")) {
				int infoFromService = intent.getIntExtra("test",-2);
				if (infoFromService == 1) {
					buttonStop.setEnabled(true);
					buttonStart.setEnabled(false);
				} else {
					buttonStop.setEnabled(false);
					// smsservice.handler.removeCallbacks(smsservice.MyRunner);
				}
			}
		}
	};
	private static final int LoopTimeSeparation = 1000;
	private SharedPreferences preferences;
	private EditText inputUsername,inputPassword;
	private Button buttonStart,buttonStop;
	public final Handler handler = new Handler();
	
	static final int DIALOG_LOAD = 0;
	static final int DIALOG_INVALID_URL = 1;
	static final String LOG_TAG = "UISmsService";
	protected static final int NOT_CONNECTED = 0;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Log.i(LOG_TAG, SmsService.class.getName());
		
//		this.connectedVisible( true );
		serviceintent = new Intent( SmsService.class.getName() );
		// Very first thing to do, check if service is running
		if( isMyServiceRunning() == true ) {
			this.connectedVisible( true );
		}
		
		// Create and bind the service
		doBindService();
		
		// Load all the UI elements that we might need later
		// TODO: Create a elementsArray class or anything like that, is it possible?
		this.inputUsername = (EditText) this.findViewById( R.id.loginInput );
		this.inputPassword = (EditText) this.findViewById( R.id.passwordInput );
		this.buttonStart = (Button) this.findViewById( R.id.connectButton );
		this.buttonStop = (Button) this.findViewById( R.id.stopButton );
		
		this.preferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		// Hide keyboard on start
		this.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		
		// Get the elements
		this.buttonStart.setOnClickListener(new View.OnClickListener() {
			// when i click on connect button
			public void onClick(View v) {
				// Read values from elements
				String username = inputUsername.getText().toString();
				String password = inputPassword.getText().toString();

				if(mBound==false){
					// We've lost connection to service, reconnect
					ArrayList<Message> q = new ArrayList<Message>();
					q.add(new Message(getString(R.string.MESSAGE_lostconeectiontoservice), "warning"));
					displayMessages(q);
					doBindService();
					Log.d(LOG_TAG,"Not bound...");
					return;
				} else {
					//Give current (new data) to service
					mService.setUsername(username);
					mService.setPassword(password);
					mService.setServer(makeServerUrl());
				}
			}
		});
	}
	
	@Override
	public void onResume(){
		super.onResume();
		isMyServiceRunning();
	}
	
	public Runnable MyRunner = new Runnable() {
		// the tasks to execute
		public void run() {
			// Add the loop back to the handler
			handler.postDelayed(this, LoopTimeSeparation);
			
			// If we have a bound
			if(mBound == true ) {
				// See if service has anything to tell us
				ArrayList<Message> queue = mService.getMessageQueue();
				int c = mService.getConnectedStatus();
				connectedVisible( c != NOT_CONNECTED );
					
				if( queue != null ) {
					displayMessages( queue );
				}
				
				
			} else {
				ArrayList<Message> q = new ArrayList<Message>();
				q.add(new Message(getString(R.string.MESSAGE_lostconeectiontoservice), "warning"));
				displayMessages(q);
				doBindService();
			}
		}
	};
	
	private void displayMessages( ArrayList<Message> queue ) {
		LinearLayout report = (LinearLayout) findViewById(R.id.reportLayout);
		for(Iterator<Message> i = queue.iterator();i.hasNext();){
			Message msg = i.next();
			Log.v(LOG_TAG,msg.toString());
			report.addView(msg.getView(UIsmsservice.this),0);
		}
	}
	
	private String makeServerUrl(){
		// Attempt to connect to the server
		URL url = null;
		
		//Make sure we have a valid url
		String complete = this.preferences.getString("server", "");
		try {
			url = new URL( complete );
		} catch( Exception e ) {
			onCreateDialog(DIALOG_INVALID_URL).show();
			return null;
		}
		return url.toExternalForm();
	}
	
	private boolean isMyServiceRunning() {
	    ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (SmsService.class.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
	
	private void connectedVisible( Boolean direction ){
		LinearLayout connected=(LinearLayout) this.findViewById(R.id.connectedLayout);
		LinearLayout notconnected=(LinearLayout) this.findViewById(R.id.notConnectedLayout);
		
		int conn;
		int notconn;
		if( direction == true ) {
			conn = (int) android.view.View.VISIBLE;
			notconn = (int) android.view.View.GONE;
		} else {
			notconn = (int) android.view.View.VISIBLE;
			conn = (int) android.view.View.GONE;
		}
		
		connected.setVisibility(conn);
		notconnected.setVisibility(notconn);
	}
	
	private Boolean updateServiceValues( String user, String pw ) {
		// Attempt to connect to the server
		URL url = null;
		
		//Make sure we have a valid url
		String complete = this.preferences.getString("server", "");
			
		try {
			url = new URL( complete );
		} catch( Exception e ) {
			onCreateDialog(DIALOG_INVALID_URL).show();
			return false;
		}
		
		// Url is valid start service
		// NOTE: Service will be started even if no network on
		if( mService != null ) {
			mService.setServer(makeServerUrl());
			mService.setUsername(user);
			mService.setPassword(pw);
		} else {
			doBindService();
		}
		return true;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// Currently, we have only one menu option
		case R.id.preferences:
			gotoPreferences("");
			break;
		}
		return true;
	}

	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
	    switch(id) {
	    case DIALOG_LOAD:
	    	ProgressDialog.Builder builder = new ProgressDialog.Builder(this);
	    	builder.setMessage( getString( R.string.DIALOG_connecting ) )
	    		.setCancelable(false);
	    	dialog = builder.create();
	    	
	        break;
	    case DIALOG_INVALID_URL:
	    	AlertDialog.Builder abuilder = new AlertDialog.Builder( this );
	    	abuilder.setMessage( getString( R.string.DIALOG_invalidurl ) )
	    		.setPositiveButton( getString( R.string.BUTTON_gotopreference ), new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int id) {
	            	// send to preferences and include the "path" to the preference we want to highlight
	                gotoPreferences(getString(R.string.PREFERENCE_server));
	           }
	       })
	       .setNegativeButton(R.string.BUTTON_cancel, new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	                dialog.cancel();
	           }
	       });
	    	dialog = abuilder.create();
	    	break;
	    	
	    default:
	        dialog = null;
	    }
	    return dialog;
	}

	/** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {
    	@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
    		SmsserviceBinder binder = (SmsserviceBinder) service;
    		mService = binder.getService();
    		startService(serviceintent);
            mBound = true;
            handler.postDelayed(MyRunner, LoopTimeSeparation);
            
            // Quick look at config file to see how we behave
    		String username = preferences.getString( "username", "" );
    		Boolean rememberpassword = preferences.getBoolean( "rememberpassword", false );
    		
    		// If password is remembered, load it
    		String password;
    		if( rememberpassword == true ) {
    			password = preferences.getString( "password", "" );
    		} else {
    			password = "";
    		}
    		
    		// During debug time, to avoid starting service
    		password = "";
    		// If we have both username and password, start service
    		if( username != "" && password != "" ) {
    			connectedVisible( true );
    			updateServiceValues( username, password );
    		} else {
    			inputUsername.setText(username);
    			// If we have a username, let's give focus to the password input
    			if( username != "" ){
    				inputPassword.requestFocus();
    			}
    		}
    		
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			mService = null;
			mBound = false;
		}
		
    };
	
	private void gotoPreferences( String preference ){
		// Launch Preference activity
		Intent i = new Intent(this, Preferences.class);
		if( preference != "" ) {
			i.putExtra( "com.zest.smsservice.Preference", preference );
		}
		startActivity(i);
	}

	public void onDestroy() {
		Log.d(LOG_TAG, "OnDestroy of UIsmstoparents");
		super.onDestroy();
		handler.removeCallbacks(MyRunner);
	}

	void doBindService() {
		bindService(serviceintent, mConnection, BIND_AUTO_CREATE);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		/*
		 * If I press back button The application keep the username and login,
		 * and I still can press the stop_service_button to end the service
		 */
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			moveTaskToBack(true);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}