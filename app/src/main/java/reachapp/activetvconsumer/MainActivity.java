package reachapp.activetvconsumer;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.crittercism.app.Crittercism;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ContentFragment.OnContentFragmentInteractionListener, TypeFragment.OnTypeFragmentInteractionListener{

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 11;
    private WifiManager wifiManager;
    private FragmentManager fragmentManager;
    private TextView searchText;
    private ProgressBar searchBar;
    private Toolbar toolbar;

    private static final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final MainActivity activity = (MainActivity) context;
            final WifiInfo wifiInfo = activity.wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                final String ssid = wifiInfo.getSSID();
                if (!TextUtils.isEmpty(ssid) && ssid.contains("reach-")) {
                    activity.unregisterReceiver(this);
                    showTypes(activity);
                    return;
                }
            }

            final List<ScanResult> wifiScanList = activity.wifiManager.getScanResults();
            for (int i = 0; i < wifiScanList.size(); i++) {
                final ScanResult scanResult = wifiScanList.get(i);
                if (scanResult.SSID.contains("reach-")) {

                    final WifiConfiguration conf = new WifiConfiguration();
                    conf.SSID = "\"" + scanResult.SSID + "\"";
                    conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    activity.wifiManager.addNetwork(conf);

                    final List<WifiConfiguration> list = activity.wifiManager.getConfiguredNetworks();

                    for (WifiConfiguration j : list) {
                        if (j.SSID != null && j.SSID.equals("\"" + scanResult.SSID + "\"")) {
                            activity.wifiManager.disconnect();
                            activity.wifiManager.enableNetwork(j.networkId, true);
                            activity.wifiManager.reconnect();
                            break;
                        }
                    }
                    return;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Crittercism.initialize(this, "b85902493eb74754bec34163e6bf7c6800555300");
        final MixpanelAPI mixpanelAPI = MixpanelAPI.getInstance(this, "944ba55b0438792632412369f541b1b3");
        final MixpanelAPI.People people = mixpanelAPI.getPeople();
        people.identify(mixpanelAPI.getDistinctId());

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        fragmentManager = getSupportFragmentManager();
        searchText = (TextView) findViewById(R.id.searchText);
        searchBar = (ProgressBar) findViewById(R.id.searchBar);

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);
        final WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null) {
            final String ssid = wifiInfo.getSSID();
            if (!TextUtils.isEmpty(ssid) && ssid.contains("reach-")) {
                showTypes(this);
                return;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                registerReceiver(wifiScanReceiver,
                        new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION))
                    Toast.makeText(this, "Location permission is needed to scan wifi", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
            }
        }
        else
            registerReceiver(wifiScanReceiver,
                    new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    @Override
    public void onOpenContent(String type) {
        fragmentManager.beginTransaction().replace(R.id.container,
                ContentFragment.newInstance(type)).addToBackStack(null).commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    registerReceiver(wifiScanReceiver,
                            new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                else {
                    try {
                        unregisterReceiver(wifiScanReceiver);
                    } catch (IllegalArgumentException ignored) {}
                    searchBar.setVisibility(View.GONE);
                    searchText.setText("Please connect to the wifi network named 'reach-'");
                }
                break;
            }
        }
    }

    private static void showTypes(MainActivity activity) {
        activity.searchText.setVisibility(View.GONE);
        activity.searchBar.setVisibility(View.GONE);
        activity.fragmentManager.beginTransaction().replace(R.id.container,
                TypeFragment.newInstance()).commit();
    }

    @Override
    public void setTitle(String title) {
        toolbar.setTitle(title);
    }

    private static class GetUpdate extends AsyncTask<Void, Void, Document> {

        private final Context context;

        GetUpdate(Context context) {
            super();
            this.context = context;
        }

        @Override
        protected Document doInBackground(Void... voids) {
            try {
                return Jsoup.connect("http://192.168.43.1:1993/").get();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Document document) {
            super.onPostExecute(document);
            final String href = document.getElementsByClass("files").select("a").get(0).attr("href");
            if (href.contains(".apk")) {
                Uri myUri = Uri.parse("http://192.168.43.1:1993" + href);
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
                intent.setData(myUri);
                context.startActivity(intent);
            }
        }
    }

    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver(wifiScanReceiver);
        } catch (IllegalArgumentException ignored) {}
        final WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null) {
            final String ssid = wifiInfo.getSSID();
            if (!TextUtils.isEmpty(ssid) && ssid.contains("reach-"))
                wifiManager.disconnect();
        }
        super.onDestroy();
    }
}
