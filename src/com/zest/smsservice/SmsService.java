package com.zest.smsservice;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.zest.smsservice.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

public class SmsService extends Service {
	/*
	 * This is the service of my program. It runs in Background
	 */
	private static final String LOG_TAG = SmsService.class.getSimpleName();
	private static final int LoopTimeSeparation = 8000;
	private static final int MaxQueueMessages = 1000;
	public static final String BROADCAST_ACTION = "com.zest.smsservice.handler";
	public final IBinder mBinder = new SmsserviceBinder();
	public final Handler handler = new Handler();
	public Intent intent, serviceintent = new Intent("com.zest.smsservice.action");
	private ConnectivityManager connectivity;
	private NetworkInfo wifiInfo, mobileInfo;
	private JSONObject jso;
	private JSONArray jsa;
	private HttpEntity mainEntity = null;
	private Notification notification;
	private NotificationManager notificationManager;
	public int infoForActivity = 0;
	private Integer lastCheck = null;
	private String username, password, listmessagestosend, modifyretrievaldate,
			modifyactualsendingdate, loginandpasswordcorrect,
			cancelretrievaldate, urlint, urlext, url;
	private SharedPreferences preferences;
	private Boolean wasMax = false;
	private int connectedStatus = 0;
	private ArrayList<com.zest.smsservice.Message> messageQueue = new ArrayList<com.zest.smsservice.Message>();
	
	@Override
	public void onCreate() {
		super.onCreate();
		enqueueMessage( "Created service","info" );
//		timer = new Timer("SmsServiceTimer");
//	    timer.schedule(updateTask, 1000L, 60 * 1000L);
		this.preferences = PreferenceManager.getDefaultSharedPreferences( this );
	}
	
	// TODO: This does not receive the broadcast that I want...
	public class NetworkReceiver extends BroadcastReceiver {
		private static final String LOG_TAG = "NetworkReceiver";
		
		@Override
		public void onReceive( Context context, Intent intent )
		{
			Log.i(LOG_TAG,"Change in connectivity"); 
			ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService( Context.CONNECTIVITY_SERVICE );
			Bundle extras = intent.getExtras();
			if( extras != null ) {
				Boolean state;
				state = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
				if(state == true){
					//hasNetwork = false;
				}
//				state = extras.getString(ConnectivityManager.EX);
//				Log.i(LOG_TAG,state);
			}
		}
	};
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d("smsservice", "Started service");
		// read url, username and password from extras
		Bundle extras = intent.getExtras();
		if( extras != null ){
			username = extras.getString("com.zest.smsservice.Username");
			password = extras.getString("com.zest.smsservice.Password");
			url = extras.getString("com.zest.smsservice.Server");
		}
		// Launch the loop
		handler.postDelayed(MyRunner, LoopTimeSeparation);
		
		// Find out about the connection
//		ConnectionInfoUpdate();
//		if( ( wifiInfo.isConnected() ) || ( mobileInfo.isConnected() ) ) {
//			Log.d("smsservice", "Beginning of Service");
//			// retrieve the username and password
//			
//
//			// test if login and password are correct
//			Boolean maingood = GoodLoginPassword();
//
//			if (maingood) {
//				Toast goodlog = Toast.makeText(SmsService.this,
//						"Good login and password \n Service Running", 2000);
//				goodlog.setGravity(Gravity.CENTER, 0, 0);
//				goodlog.show();
//				infoForActivity = 1;
//				handler.removeCallbacks(MyRunner);
//				handler.postDelayed(MyRunner, 5000); // 1 second
//
//				// Create a notification that tells if the service is running
//				// This notification can't be cleared by the user
//				// It is destroyed when service finish
//				notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//				long when = System.currentTimeMillis();
//				notification = new Notification(R.drawable.icon,
//						"SmsService", when);
//				PendingIntent pi = PendingIntent.getActivity(this, 0,
//						new Intent(this, SmsService.class), 0);
//				notification.flags = Notification.FLAG_NO_CLEAR;
//				notification.setLatestEventInfo(this,
//						"Service SmsService Running",
//						"The service SmsService is currently running", pi);
//				notificationManager.notify(1, notification);
//			} else {
//				Toast badlog = Toast.makeText(SmsService.this,
//						"Wrong login or password", 2000);
//				badlog.setGravity(Gravity.CENTER, 0, 0);
//				badlog.show();
//				infoForActivity = -1;
//				Toast ta = Toast.makeText(SmsService.this, "Try again", 2000);
//				ta.setGravity(Gravity.CENTER, 0, 0);
//				ta.show();
//				this.stopSelf();
//			}
//		} else {
//			Toast noconnect = Toast.makeText(SmsService.this,
//					"No connection available", 2000);
//			noconnect.setGravity(Gravity.CENTER, 0, 0);
//			Log.d("smstoparents", "No connection available");
//			noconnect.show();
//			Toast tl = Toast.makeText(SmsService.this,
//					"Connect to mobile network and/or wifi and then try again",
//					2000);
//			tl.setGravity(Gravity.CENTER, 0, 0);
//			Log.d("smstoparents",
//					"Connect to mobile network and/or wifi and then try again");
//			tl.show();
//			SmsService.this.stopSelf();
//			infoForActivity = -1;
//		}
		// I send to the UI, the actual state of my service (running or not)
//		serviceintent.putExtra("test", infoForActivity);
//		sendBroadcast(serviceintent);
		return START_STICKY;
	}
	
	public void setUsername( String u ){
		enqueueMessage(String.format(getString(R.string.MESSAGE_usernamevaluesetto),u), "warning");
		username = u;
	}
	
	public void setPassword( String u ){
		String fake = "";
		for( int i=0; i<u.length(); i++ ){
			fake += "*";
		}
		enqueueMessage(String.format(getString(R.string.MESSAGE_passwordvaluesetto),fake), "warning");
		password = u;
	}
	
	public void setServer( String u ){
		enqueueMessage(String.format(getString(R.string.MESSAGE_servervaluesetto),u), "warning");
		url = u;
	}

	public Runnable MyRunner = new Runnable() {
		// the tasks to execute
		public void run() {
			Log.v(LOG_TAG, "Beginning loop");
			// Add the loop back to the handler
			handler.postDelayed(this, LoopTimeSeparation);
			
			// New connection or check?
			Boolean attemptToConnect = false;
			
			// First things first, do we know which server to connect to?
			if( url != null ){
				
				// LastCheck set to null means that either we just started
				// or we failed to get connection
				if( lastCheck == null ){
					attemptToConnect = true;
				} else {
					// If last check is set, see whether it is long time enough ago
					// This could depend on whether we reached maximum count last time around
					Integer period = null;
					String ifCantParse = getString(R.string.defaultperiod);
					if( wasMax == true && preferences.getBoolean("changecheckfrequencyonmaximumsms", false) == true ) {
						try {
							period = Integer.parseInt( preferences.getString("frequency", ifCantParse) );
						} catch (NumberFormatException e) {
							period = Integer.parseInt(ifCantParse);
						}
					} else {
						try {
							period = Integer.parseInt( preferences.getString("checkfrequencyonmaximumsms", "1200") );
						} catch (NumberFormatException e) {
							period = Integer.parseInt(ifCantParse);
						}
					}
					//Period is in seconds change to milliseconds
					period = period * 1000;
					Log.i(LOG_TAG,period.toString());
					
					// Now see if time difference is enough
					attemptToConnect = ( (int) SystemClock.elapsedRealtime() - lastCheck > period );
				}
				
				// check whether there might is a positive change in network status
				Boolean hadnetwork = false;
				if( hasNetwork() == false ) {
					// check whether we care
					if( preferences.getBoolean( "checkonnetwork", false) ) {
						ConnectionInfoUpdate();
						if( hasNetwork() == true ) {
							enqueueMessage(getString(R.string.MESSAGE_networkhaschangedforthebest), "info");
							attemptToConnect = true;
						}
					}
				} else {
					hadnetwork = true;
				}
				// From above, do we need at all to try and connect?
				if( attemptToConnect == true ) {
					// Refresh connection data
					ConnectionInfoUpdate();
					if( hasNetwork() == true ) {
						if( hadnetwork == false ) {
							enqueueMessage(getString(R.string.MESSAGE_networkhaschangedforthebest), "error");
						}
						
						// So we want to connect AND we have network...
						// Let's do it
						retrieveFromServer();
						
					} else {
						if( hadnetwork == true ) {
							enqueueMessage(getString(R.string.ERROR_Noconnection), "error");
						}
					}
				}
			}
			
//			
//			mainEntity = null;
//			mainEntity = MyConnection(1, 0);
//			if (mainEntity != null) {
//				jsa = MyJSONGetter(mainEntity);
//				GetSendMessages(jsa);
//			} else {
//				Log.e("smstoparents", "connection problem");
//				SmsService.this.stopSelf();
//			}
		}
	};
	
	private void retrieveFromServer(){
		lastCheck = (int) SystemClock.elapsedRealtime();
		// Connect and get data
		enqueueMessage(getString(R.string.MESSAGE_connectingtoserver), "info");
		HttpEntity entity = MyConnection();
		String result = null;
		try {
			result = EntityUtils.toString(entity, HTTP.UTF_8);
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		enqueueMessage(result, "warning");
		connectedStatus = 1;
	}
	
	private void enqueueMessage( String message, String type ){
		if(messageQueue.size() == MaxQueueMessages){
			messageQueue.remove(messageQueue.size());
		}
		messageQueue.add( new com.zest.smsservice.Message(message, type) );
	}
	
	private Boolean hasNetwork() {
		if( mobileInfo != null && wifiInfo != null ) {
			return mobileInfo.isConnected() || wifiInfo.isConnected();
		} else {
			return false;
		}
	}
	
	private void ConnectionInfoUpdate() {
		// Function that update all my connections informations (mobile network
		// and wifi)
		connectivity = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		wifiInfo = connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		mobileInfo = connectivity
				.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
	}
	
	public ArrayList<com.zest.smsservice.Message> getMessageQueue()
	{
		ArrayList<com.zest.smsservice.Message> topass = (ArrayList<com.zest.smsservice.Message>) messageQueue.clone();
		messageQueue.clear();
		return topass;
	}

	private HttpEntity MyConnection() {
		HttpEntity entity = null;

		HttpParams httpParameters = new BasicHttpParams();
		// Set the default socket timeout (SO_TIMEOUT)
		// in milliseconds which is the timeout for waiting for serviceintent.
		// TODO: Make that a parameter of some sort?
		int timeoutSocket = 60000;
		HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

		HttpClient hc = new DefaultHttpClient(httpParameters);
		HttpPost hp = new HttpPost(url);
		
		// Put the username and passwords into the post
		List<BasicNameValuePair> param = new ArrayList<BasicNameValuePair>();
		param.add(new BasicNameValuePair("username", username));
		param.add(new BasicNameValuePair("password", password));
		
		try {
			hp.setEntity(new UrlEncodedFormEntity(param, HTTP.UTF_8));
		} catch (UnsupportedEncodingException e1) {
			enqueueMessage(e1.getLocalizedMessage(), "error");
			return null;
		}
		HttpResponse resp = null;
		try {
			resp = hc.execute(hp);
		} catch (ClientProtocolException e1) {
			enqueueMessage(e1.getLocalizedMessage(), "error");
			return null;
		} catch (IOException e1) {
			enqueueMessage(e1.getLocalizedMessage(), "error");
			return null;
		}
		
		//Got this far means connection to server is ok
		enqueueMessage(getString(R.string.MESSAGE_validconnectiontoserver), "info");
		if( resp != null ) {
			entity = resp.getEntity();
		}
		return entity;
	}

	private boolean RequestSent(HttpEntity httpe) {
		// Function that tells if one request has been well sent and applied in
		// the DB
		boolean good = false;
		JSONObject jsoo;
		try {
			String result = EntityUtils.toString(httpe, HTTP.UTF_8);
			jsoo = new JSONObject(result);
			String jst = jsoo.get("success").toString();
			if (jst.equals("true")) {
				good = true;
			} else {
				Log.e("smstoparents", "Request problem ");
			}
		} catch (Exception e1) {
			Log.e("smstoparents", "Request problem " + e1.toString());
		}
		return good;
	}

	private JSONArray MyJSONGetter(HttpEntity entity) {
		// get the json answer from the request to retrieve messages to send
		JSONArray jsa = null;
		JSONTokener jst = null;
		try {
			String result = EntityUtils.toString(entity, HTTP.UTF_8);
			jso = new JSONObject(result);
			jst = new JSONTokener(jso.get("data").toString());
			Log.d("smstoparents", "parsing data x" + jst + "x");
			if (!jst.equals(" at character 0 of null")) {
				jsa = new JSONArray(jst);
			}
		} catch (Exception e1) {
			onDestroy();
			Log.e("smstoparents", "My Error parsing data " + e1.toString());
		}
		return jsa;
	}

	private void GetSendMessages(JSONArray jsar) {
		// read the JSON, get messages and try to send them if the network
		// permit it
		/*int id;
		String msg, tel;
		boolean sent = false;
		HttpEntity httpe2, httpe3, httpe5;
		try {
			for (int i = 0; i < jsar.length(); i++) {
				JSONObject temp = jsar.getJSONObject(i);
				id = temp.getInt("id");
				// I can send mesage and actualise DB only if i am connected
				// to mobile network, otherhand i can't send the sms
				if (mobileInfo.isAvailable()) {
					msg = temp.getString("message");
					tel = temp.getString("to");
					httpe2 = MyConnection(2, id);
					if (RequestSent(httpe2)) {
						sent = sendSMS(tel, msg, id);
						if (sent) {
							Log.d("smstoparents", "message " + id + " sent");
							httpe3 = MyConnection(3, id);
							if (!RequestSent(httpe3)) {
								Log.e("smstoparents",
										"actual_sending_date request undone!");
								Toast toast = Toast.makeText(SmsService.this,
										"Impossible to modify actualsendingdate", 5000);
								toast.setGravity(Gravity.CENTER, 0, 0);
								toast.show();
							}
						} else {
							Log.e("smstoparents",
									"message not sent! \n unknown problem");
							Toast toast = Toast.makeText(SmsService.this,
									"message not sent, unknown problem", 5000);
							toast.setGravity(Gravity.CENTER, 0, 0);
							toast.show();
							// tell the DB to cancel the retrieval_date of the
							// message (null)
							httpe5 = MyConnection(5, id);
							if (!RequestSent(httpe5)) {
								Log
										.e("smstoparents",
												"Impossible to cancel the retrieval date");
								Log.e("smstoparents", "Problem: The message "
										+ id + "has not been sent");
							}
						}
					} else {
						Log.e("smstoparents", "retrieval_date request undone!");
					}
				} else {
					Log.e("smstoparents", "message " + id
							+ " not sent because no mobile network available");
					Toast toast = Toast.makeText(SmsService.this,
							"message not sent because no mobile network available", 5000);
					toast.setGravity(Gravity.CENTER, 0, 0);
					toast.show();
				}
			}
		} catch (JSONException e) {
			Log.e("smstoparents", "JSON read problem");
			e.printStackTrace();
		}*/
	}

	private boolean sendSMS(String phoneNumber, String message, int id) {
		// send SMS. this function is only called by GetSendMessages
		boolean sent = false;
		PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this,
				SmsService.class), 0);
		SmsManager sms = SmsManager.getDefault();
		sms.sendTextMessage(phoneNumber, null, message, pi, null);
		sent = true;
		Log.d("smstoparents", "message sent to " + phoneNumber);
		if (sent) {
			Toast msgsent = Toast.makeText(SmsService.this,
					"smstoparents: message " + id + " sent to " + phoneNumber,
					5000);
			msgsent.setGravity(Gravity.CENTER, 0, 0);
			msgsent.show();
		}
		return sent;
	}

	public void localConnectionOrNot() {
		// I try to reach local server
		try {
			URL url1 = new URL(urlint);
			HttpURLConnection urlc = (HttpURLConnection) url1.openConnection();
			urlc.setConnectTimeout(1000 * 5);
			urlc.connect();
			if (urlc.getResponseCode() == 200) {
				Log.d("smstoparents", "local");
				url = urlint;
			}
			// If I can't, I try to reach external server
		} catch (Exception e) {
			Log.d("smstoparents", "non local connection");
			try {
				URL url2 = new URL(urlext);
				HttpURLConnection urlc = (HttpURLConnection) url2
						.openConnection();
				urlc.setConnectTimeout(1000 * 30);
				urlc.connect();
				if (urlc.getResponseCode() == 200) {
					Log.d("smstoparents", "external");
					url = urlext;
				}
			} catch (Exception e2) {
				e2.printStackTrace();
				Log.d("smstoparents", "non external connection");
				// The server is unreachable
				Toast toast = Toast.makeText(SmsService.this,
						"Server Unreachable, Please try later", 3000);
				toast.setGravity(Gravity.CENTER, 0, 0);
				toast.show();
				this.stopSelf();
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public class SmsserviceBinder extends Binder {
		SmsService getService() {
			return SmsService.this;
		}
	}
	
	public int getConnectedStatus(){
		return connectedStatus;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	    Log.i(LOG_TAG, "Service destroying");
	    handler.removeCallbacks(MyRunner);
//	    timer.cancel();
//	    timer = null;
		notificationManager.cancelAll();
		this.onUnbind(serviceintent);
	}
}