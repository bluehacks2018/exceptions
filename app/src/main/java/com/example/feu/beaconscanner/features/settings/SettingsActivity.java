package  com.example.feu.beaconscanner.features.settings;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.MenuItem;
import android.widget.ScrollView;

import com.example.feu.beaconscanner.AppSingleton;
import com.example.feu.beaconscanner.R;
import com.example.feu.beaconscanner.utils.PreferencesHelper;
import com.google.firebase.analytics.FirebaseAnalytics;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;

public class SettingsActivity extends AppCompatActivity {

    @Inject PreferencesHelper prefs;
    @Inject FirebaseAnalytics tracker;

    @BindView(R.id.content) ScrollView content;
    @BindView(R.id.scan_open) SwitchCompat scanOpen;

    private ActionBar ab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);
        AppSingleton.activityComponent().inject(this);

        if ((ab = getSupportActionBar()) != null) {
            ab.setTitle(getString(R.string.settings));
            ab.setDisplayHomeAsUpEnabled(true);
        }

        scanOpen.setChecked(prefs.isScanOnOpen());
    }

    @OnCheckedChanged(R.id.scan_open)
    public void onScanOpenChanged(boolean status) {
        Bundle b = new Bundle();

        b.putBoolean("status", status);
        tracker.logEvent("scan_open_changed", b);
        prefs.setScanOnOpen(status);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
