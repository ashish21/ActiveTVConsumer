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
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.crittercism.app.Crittercism;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ContentFragment.OnContentFragmentInteractionListener, TypeFragment.OnTypeFragmentInteractionListener{

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 11;
    private static WifiManager wifiManager;
    private MixpanelAPI mixpanelAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Crittercism.initialize(this, "b85902493eb74754bec34163e6bf7c6800555300");
        mixpanelAPI = MixpanelAPI.getInstance(this, "944ba55b0438792632412369f541b1b3");
        final MixpanelAPI.People people = mixpanelAPI.getPeople();
        people.identify(mixpanelAPI.getDistinctId());

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        if (!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);
        if (wifiManager.getConnectionInfo().getSSID().contains("reach-")) {
            showTypes(this);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                registerReceiver(new WifiScanReceiver(),
                        new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION))
                    Toast.makeText(this, "Location permission is needed to scan wifi", Toast.LENGTH_SHORT).show();
                else
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                            MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
            }
        }
        else
            registerReceiver(new WifiScanReceiver(),
                    new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    @Override
    public void onOpenContent(String type) {
        getSupportFragmentManager().beginTransaction().replace(R.id.container,
                ContentFragment.newInstance(type)).addToBackStack(null).commit();
    }

    private static final class WifiScanReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, Intent intent) {

            final List<ScanResult> wifiScanList = wifiManager.getScanResults();
            for (int i = 0; i < wifiScanList.size(); i++) {
                final ScanResult scanResult = wifiScanList.get(i);
                if (scanResult.SSID.contains("reach-")) {
                    context.unregisterReceiver(this);

                    final WifiConfiguration conf = new WifiConfiguration();
                    conf.SSID = "\"" + scanResult.SSID + "\"";
                    conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    wifiManager.addNetwork(conf);

                    final List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();

                    for (WifiConfiguration j : list) {
                        if (j.SSID != null && j.SSID.equals("\"" + scanResult.SSID + "\"")) {
                            wifiManager.disconnect();
                            wifiManager.enableNetwork(j.networkId, true);
                            wifiManager.reconnect();

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    showTypes((AppCompatActivity) context);
                                }
                            }, 5000L);
                            break;
                        }
                    }
                    return;
                }
            }
            Toast.makeText(context, "Trying to find Reach Seeders", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    registerReceiver(new WifiScanReceiver(),
                            new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                else
                    Toast.makeText(this, "Please connect to the wifi network: reach-2016", Toast.LENGTH_SHORT).show();
                break;
            }
        }
    }

    private static void showTypes(AppCompatActivity activity) {
        activity.getSupportFragmentManager().beginTransaction().replace(R.id.container,
                TypeFragment.newInstance()).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();

        if (id == R.id.action_update) {
            new GetUpdate(this).execute();
            return true;
        }

        return super.onOptionsItemSelected(item);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
}
