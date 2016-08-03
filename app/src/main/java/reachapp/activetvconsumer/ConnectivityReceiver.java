package reachapp.activetvconsumer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mixpanel.android.mpmetrics.MixpanelAPI;

/**
 * Created by ashish on 04/08/16.
 */

public class ConnectivityReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final MixpanelAPI mixpanelAPI  = MixpanelAPI.getInstance(context, "944ba55b0438792632412369f541b1b3");
        mixpanelAPI.flush();
    }
}
