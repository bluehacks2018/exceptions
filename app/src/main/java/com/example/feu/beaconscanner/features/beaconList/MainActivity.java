package com.example.feu.beaconscanner.features.beaconList;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.example.feu.beaconscanner.AppSingleton;
import com.example.feu.beaconscanner.R;
import com.example.feu.beaconscanner.events.Events;
import com.example.feu.beaconscanner.events.RxBus;
import com.example.feu.beaconscanner.models.BeaconSaved;
import com.example.feu.beaconscanner.utils.BluetoothManager;
import com.example.feu.beaconscanner.utils.DividerItemDecoration;
import com.example.feu.beaconscanner.utils.PreferencesHelper;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.utils.UrlBeaconUrlCompressor;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements BeaconConsumer, EasyPermissions.PermissionCallbacks {
    private static final String[] perms = {Manifest.permission.ACCESS_COARSE_LOCATION};
    private static final int RC_COARSE_LOCATION = 1;
    private static final int RC_SETTINGS_SCREEN = 2;

    private Disposable bluetoothStateDisposable;
    private Disposable rangeDisposable;
    private MaterialDialog dialog;
    private RealmResults<BeaconSaved> beaconResults;

    public static double current_distance = 0.0;
    public static List<BeaconInfo> list = new ArrayList<>();
    public static boolean statusInit = false;
    TextToSpeech textToSpeech;
    public static List<BeaconInfo> tempList = new ArrayList<>();
    public static boolean isClose = true;
    public static boolean scanAgain = true;

    @Inject BluetoothManager bluetooth;
    @Inject BeaconManager beaconManager;
    @Inject RxBus rxBus;
    @Inject Realm realm;
    @Inject PreferencesHelper prefs;
    @Inject FirebaseAnalytics tracker;

    @Inject @Named("play_to_pause") AnimatedVectorDrawable playToPause;
    @Inject @Named("pause_to_play") AnimatedVectorDrawable pauseToPlay;

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.progress) ProgressBar progress;
    @BindView(R.id.activity_main) CoordinatorLayout rootView;
    @BindView(R.id.bluetooth_state) TextView bluetoothState;

    @BindView(R.id.empty_view) RelativeLayout emptyView;
    @BindView(R.id.beacons_rv) RecyclerView beaconsRv;

    @BindView(R.id.scan_fab) FloatingActionButton scanFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        AppSingleton.activityComponent().inject(this);

        setSupportActionBar(toolbar);
        toolbar.inflateMenu(R.menu.main_menu);
        progress.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(this, R.color.progressColor),
                android.graphics.PorterDuff.Mode.MULTIPLY);

        beaconResults = realm.where(BeaconSaved.class).findAllSortedAsync(new String[]{"lastMinuteSeen", "distance"}, new Sort[]{Sort.DESCENDING, Sort.ASCENDING});

        beaconsRv.setHasFixedSize(true);
        beaconsRv.setLayoutManager(new LinearLayoutManager(this));
        beaconsRv.addItemDecoration(new DividerItemDecoration(this, null));
        beaconsRv.setAdapter(new BeaconsRecyclerViewAdapter(this, beaconResults, true));

        bluetoothStateDisposable = bluetooth.asFlowable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(e -> {
                    if (e instanceof Events.BluetoothState) {
                        bluetoothStateChanged(((Events.BluetoothState) e).getState());
                    }
                });
    }

    @Override
    protected void onResume() {
        scanAgain = true;
        beaconResults.addChangeListener(results -> {
            if (results.size() == 0 && emptyView.getVisibility() != View.VISIBLE) {
                beaconsRv.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            } else if (results.size() > 0 && beaconsRv.getVisibility() != View.VISIBLE) {
                beaconsRv.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
            }
        });

        if (bluetooth.isEnabled()) {
                startScan();
        }
        super.onResume();
    }

    private void updateUiWithBeaconsArround(Collection<Beacon> beacons) {
        realm.executeTransactionAsync(tRealm -> {
            Observable.fromIterable(beacons)
                    .subscribe(b -> {
                        BeaconSaved beacon = new BeaconSaved();
                        beacon.setHashcode(b.hashCode());
                        beacon.setLastSeen(new Date());
                        beacon.setLastMinuteSeen(new Date().getTime() / 1000 / 60);
                        beacon.setBeaconAddress(b.getBluetoothAddress());
                        beacon.setRSSI(b.getRssi());
                        beacon.setManufacturer(b.getManufacturer());
                        beacon.setTxPower(b.getTxPower());
                        beacon.setDistance(b.getDistance());
                        if (b.getServiceUuid() == 0xfeaa) { // This is an Eddystone beacon
                            if (b.getExtraDataFields().size() > 0) {
                                beacon.setHasTelemetryData(true);
                                beacon.setTelemetryVersion(b.getExtraDataFields().get(0));
                                beacon.setBatteryMilliVolts(b.getExtraDataFields().get(1));
                                beacon.setTemperature(b.getExtraDataFields().get(2));
                                beacon.setPduCount(b.getExtraDataFields().get(3));
                                beacon.setUptime(b.getExtraDataFields().get(4));
                            } else {
                                beacon.setHasTelemetryData(false);
                            }

                            switch (b.getBeaconTypeCode()) {
                                case 0x00:
                                    beacon.setBeaconType(BeaconSaved.TYPE_EDDYSTONE_UID);
                                    // This is a Eddystone-UID frame
                                    beacon.setNamespaceId(b.getId1().toString());
                                    beacon.setInstanceId(b.getId2().toString());
                                    break;
                                case 0x10:
                                    beacon.setBeaconType(BeaconSaved.TYPE_EDDYSTONE_URL);
                                    // This is a Eddystone-URL frame
                                    beacon.setURL(UrlBeaconUrlCompressor.uncompress(b.getId1().toByteArray()));
                                    break;
                            }
                        } else { // This is an iBeacon or ALTBeacon
                            beacon.setBeaconType(b.getBeaconTypeCode() == 0xbeac? BeaconSaved.TYPE_ALTBEACON : BeaconSaved.TYPE_IBEACON); // 0x4c000215 is iBeacon
                            beacon.setUUID(b.getId1().toString());
                            beacon.setMajor(b.getId2().toString());
                            beacon.setMinor(b.getId3().toString());
                        }

                        Bundle infos = new Bundle();

                        infos.putInt("manufacturer", beacon.getManufacturer());
                        infos.putInt("type", beacon.getBeaconType());
                        infos.putDouble("distance", beacon.getDistance());
                        list.add(new BeaconInfo(beacon.getBeaconAddress(), beacon.getDistance()));
                        tempList.add(new BeaconInfo(beacon.getBeaconAddress(), beacon.getDistance()));

                        textToSpeech = new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {
                            @Override
                            public void onInit(int status) {
                                textToSpeech.setLanguage(Locale.ENGLISH);
                                if (!statusInit && scanAgain){
                                    DecimalFormat df = new DecimalFormat("#.##");
                                    textToSpeech.speak("Nearest exit is " + df.format(list.get(0).getDistance()) + "meters away.", TextToSpeech.QUEUE_FLUSH, null);
                                    statusInit = true;
                                }
                                else if (scanAgain){
                                    DecimalFormat df = new DecimalFormat("#.##");
                                    Collections.sort(list, new Comparator<BeaconInfo>() {
                                        @Override
                                        public int compare(BeaconInfo o1, BeaconInfo o2) {
                                            if (o1.getDistance() == o2.getDistance()) {
                                                return 0;
                                            }
                                            else if (o1.getDistance() < o2.getDistance()) {
                                                return -1;
                                            }
                                            else {
                                                return 1;
                                            }
                                        }
                                    });
                                    String tempAddressHolder = list.get(0).getAddress();
                                    if (!tempAddressHolder.equals(tempList.get(0).getAddress())) {
                                        tempList.add(0, new BeaconInfo(list.get(0).getAddress(), list.get(0).getDistance()));
                                        statusInit = false;
                                    }

                                    double distance = Double.parseDouble(df.format(list.get(0).getDistance()));
                                    if (((distance - current_distance) > 0.8) || ((distance - current_distance) < - 0.8)) {
                                        textToSpeech.speak("Nearest exit is " + distance + " meters away.", TextToSpeech.QUEUE_FLUSH, null);
                                        current_distance = Double.parseDouble(df.format(list.get(0).getDistance()));
                                    }
                                    if (((distance) >= (tempList.get(0).getDistance() / 2)) && ((distance) < ((tempList.get(0).getDistance() / 2) + 0.2))) {
                                        textToSpeech.speak("Will arrive in " + (int)distance + " seconds.", TextToSpeech.QUEUE_FLUSH, null);
                                    }
                                    if ((distance < 1) && (isClose)){
                                        textToSpeech.speak("Please proceed to the exit", TextToSpeech.QUEUE_FLUSH, null);
                                        isClose = false;
                                    }
                                    else {
                                        isClose = true;
                                    }
                                }
                            }
                        });
                        tracker.logEvent("adding_or_updating_beacon", infos);
                        tRealm.copyToRealmOrUpdate(beacon);
                    });
        });
        list.clear();
        if (tempList.size() >= 1) {
            String address = tempList.get(0).getAddress();
            double initial_distance = tempList.get(0).getDistance();
            tempList.clear();
            tempList.add(new BeaconInfo(address, initial_distance));
        }
    }

    private void bluetoothStateChanged(int state) {
        bluetoothState.setVisibility(View.VISIBLE);
        switch (state) {
            case BluetoothAdapter.STATE_OFF:
                bluetoothState.setTextColor(ContextCompat.getColor(this, R.color.bluetoothDisabledLight));
                bluetoothState.setBackgroundColor(ContextCompat.getColor(this, R.color.bluetoothDisabled));
                bluetoothState.setText(getString(R.string.bluetooth_disabled));
                stopScan();
                invalidateOptionsMenu();
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                bluetoothState.setTextColor(ContextCompat.getColor(this, R.color.bluetoothTurningOffLight));
                bluetoothState.setBackgroundColor(ContextCompat.getColor(this, R.color.bluetoothTurningOff));
                bluetoothState.setText(getString(R.string.turning_bluetooth_off));
                break;
            case BluetoothAdapter.STATE_ON:
                bluetoothState.setVisibility(View.GONE); // If the bluetooth is ON, we don't warn the user
                bluetoothState.setText(getString(R.string.bluetooth_enabled));
                invalidateOptionsMenu();
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                bluetoothState.setTextColor(ContextCompat.getColor(this, R.color.bluetoothTurningOnLight));
                bluetoothState.setBackgroundColor(ContextCompat.getColor(this, R.color.bluetoothTurningOn));
                bluetoothState.setText(getString(R.string.turning_bluetooth_on));
                break;
        }
    }

    public boolean bindBeaconManager() {
        if (EasyPermissions.hasPermissions(this, perms)) { // Ask permission and bind the beacon manager
            if (!beaconManager.isBound(this)) {
                beaconManager.bind(this);
            }
            return true;
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, perms, RC_COARSE_LOCATION);
        }
        return false;
    }

    @OnClick(R.id.scan_fab)
    public void startStopScan() {
        if (!isScanning()) {
            scanAgain = true;
            tracker.logEvent("start_scanning_clicked", null);
            if (!bluetooth.isEnabled()) {
                Snackbar.make(rootView, getString(R.string.enable_bluetooth_to_start_scanning), Snackbar.LENGTH_LONG).show();
                return ;
            }
            startScan();

        } else {
            scanAgain = false;
            tracker.logEvent("stop_scanning_clicked", null);
            stopScan();
        }
    }

    public boolean isScanning() {
        return rangeDisposable != null && !rangeDisposable.isDisposed();
    }

    public void startScan() {
        scanAgain = true;
        if (!isScanning() && bindBeaconManager()) {
            rangeDisposable = rxBus.asFlowable()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(e -> {
                        if (e instanceof Events.RangeBeacon) {
                            updateUiWithBeaconsArround(((Events.RangeBeacon) e).getBeacons());
                        }
                    });

            toolbar.setTitle(getString(R.string.scanning_for_beacons));
            progress.setVisibility(View.VISIBLE);
            scanFab.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.colorPauseFab)));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                scanFab.setImageDrawable(playToPause);
                playToPause.start();
            } else {
                scanFab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pause_icon));
            }
        }
    }

    public void stopScan() {
        if (isScanning()) {
            scanAgain = false;
            rangeDisposable.dispose();

            toolbar.setTitle(getString(R.string.app_name));
            progress.setVisibility(View.GONE);
            scanFab.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.colorAccent)));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                scanFab.setImageDrawable(pauseToPlay);
                pauseToPlay.start();
            } else {
                scanFab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.play_icon));
            }
            realm.executeTransactionAsync(tRealm -> {
                tRealm.where(BeaconSaved.class).findAll().deleteAllFromRealm();
            });
        }
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.addRangeNotifier((beacons, region) -> {
            rxBus.send(new Events.RangeBeacon(beacons, region));
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("Beacon Scanner", null, null, null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        tracker.logEvent("permission_granted", null);
        startScan();
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> permList) {
        if (requestCode == RC_COARSE_LOCATION) {
            tracker.logEvent("permission_denied", null);
            if (EasyPermissions.somePermissionPermanentlyDenied(this, permList)) {
                tracker.logEvent("permission_denied_permanently", null);
                showPermissionSnackbar();
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, perms, RC_COARSE_LOCATION);
            }
        }
    }

    public void showPermissionSnackbar() {
        final Snackbar snackBar = Snackbar.make(rootView, getString(R.string.enable_permission_from_settings), Snackbar.LENGTH_INDEFINITE);
        snackBar.setAction(getString(R.string.enable),v -> {
            snackBar.dismiss();
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivityForResult(intent, RC_SETTINGS_SCREEN);
        });
        snackBar.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        if (!bluetooth.isEnabled()) {
            menu.getItem(0).setIcon(R.drawable.ic_bluetooth_disabled_white_24dp);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_bluetooth:
                bluetooth.toggle();
                tracker.logEvent("action_bluetooth", null);
                break ;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onPause() {
        scanAgain = false;
        prefs.setScanningState(isScanning());
        stopScan();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (dialog != null) {
            dialog.dismiss();
        }
        if (beaconManager.isBound(this)) {
            beaconManager.unbind(this);
        }
        if (bluetoothStateDisposable != null && !bluetoothStateDisposable.isDisposed()) {
            bluetoothStateDisposable.dispose();
        }
        realm.close();
        super.onDestroy();
    }
    class BeaconInfo {

        public BeaconInfo(String address, double distance) {
            this.address = address;
            this.distance = distance;
        }
        String address;
        double distance;

        public String getAddress() {
            return address;
        }

        public double getDistance() {
            return distance;
        }

        public void setUuid(String newAddress){
            address = newAddress;
        }

        public void setDistance(double newDistance){
            distance = newDistance;
        }
    }
}