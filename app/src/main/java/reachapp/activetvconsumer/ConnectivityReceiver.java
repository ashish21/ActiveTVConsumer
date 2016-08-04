package reachapp.activetvconsumer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.crittercism.app.Crittercism;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

/**
 * Created by ashish on 04/08/16.
 */

public class ConnectivityReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final Boolean noConnectivity = intent.getExtras().getBoolean("noConnectivity", false);
        Log.d(ConnectivityReceiver.class.getSimpleName(), "onReceive, isOnline = " + !noConnectivity);
        if (noConnectivity)
            return;
        Crittercism.initialize(context, "b85902493eb74754bec34163e6bf7c6800555300");
        final MixpanelAPI mixpanelAPI  = MixpanelAPI.getInstance(context, "944ba55b0438792632412369f541b1b3");
        mixpanelAPI.flush();
    }
}
