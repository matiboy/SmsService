package com.zest.smsservice;

import com.zest.smsservice.R;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

public class Preferences extends PreferenceActivity {
	private static final String SCREEN_KEY="prefKey";
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    addPreferencesFromResource(R.xml.preferences);
	    Bundle extras = getIntent().getExtras();
	    
	    String pref="";
    	if(extras != null){
		    // If we find a key in extras, try and reach that preference
		    pref = extras.get("com.zest.smsservice.Preference").toString();
    	}
	    
	    if( pref != "" ){
	    	this.click( pref );
	    }
	}
	
	private void click( String preference ){
		PreferenceScreen screen = (PreferenceScreen) findPreference(Preferences.SCREEN_KEY);
    	android.widget.Adapter ada = screen.getRootAdapter();
    	
    	// Loop through all preferences
    	// NOTE: We can't use findPreference("dsadas").getOrder() because this depends on the level of the preference
    	Integer position=null;
    	for( int i=0; i < ada.getCount(); i++) {
    		String prefKey = ( (Preference) ada.getItem(i) ).getKey();
            if( prefKey != null && prefKey.equals( preference ) ) {
                position = i;
            	break;
            }
    	}
    	
    	// Simulate the click to open preference
    	if(position != null){
    		screen.onItemClick( null, null, position, 0 );
    	}
	}
}