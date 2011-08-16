package com.zest.smsservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
//
//// TODO: How to combine this with my service?
//public class NetworkReceiver extends BroadcastReceiver {
//	
//	private static final String LOG_TAG = "NetworkReceiver";
//	
//	@Override
//	public void onReceive( Context context, Intent intent )
//	{
//		Log.i(LOG_TAG,"Change in connectivity"); 
//		ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService( Context.CONNECTIVITY_SERVICE );
//		NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
//		NetworkInfo mobNetInfo = connectivityManager.getNetworkInfo( ConnectivityManager.TYPE_MOBILE );
//		Bundle extras = intent.getExtras();
//		if( extras != null ) {
//			Boolean state;
//			state = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
//			if(state == true){
//				
//			}
////			state = extras.getString(ConnectivityManager.EX);
////			Log.i(LOG_TAG,state);
//		}
//	}
//};