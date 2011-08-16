package com.zest.smsservice;

import java.util.ArrayList;

import android.content.Context;
import android.widget.TextView;

public class Message extends Object {
	private String content;
	private String type;
	private static final ArrayList<String> typeKeys = new ArrayList<String>();
	private static final String LOG_TAG = SmsService.class.getSimpleName();
	public Message( String a, String b ){
		content = a;
		type = b;
		typeKeys.add( "info" );
		typeKeys.add( "warning" );
		typeKeys.add( "error" );
	}
	
	public String toString(){
		return content + "|" + type;
	}
	
	private String getContent(){
		return content;
	}
	
	private int getType(){
		return typeKeys.indexOf( type );
	}
	
	public TextView getView( Context context ){
		TextView oneLine = new TextView( context );
		int type = getType();
		oneLine.setText( getContent() );
		switch( type ) {
			case 0: oneLine.setTextColor(context.getResources().getColor(R.color.info));
			break;
			case 1: oneLine.setTextColor(context.getResources().getColor(R.color.warning));
			break;
			case 2: oneLine.setTextColor(context.getResources().getColor(R.color.error));
			break;
		}
		oneLine.setTextSize(11);
		return oneLine;
	}
}
