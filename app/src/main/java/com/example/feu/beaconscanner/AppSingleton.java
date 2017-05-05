package  com.example.feu.beaconscanner;

import android.app.Application;

import com.example.feu.beaconscanner.dagger.components.ActivityComponent;
import com.example.feu.beaconscanner.dagger.components.AppComponent;
import com.example.feu.beaconscanner.dagger.components.DaggerActivityComponent;
import com.example.feu.beaconscanner.dagger.components.DaggerAppComponent;
import com.example.feu.beaconscanner.dagger.modules.AnimationModule;
import com.example.feu.beaconscanner.dagger.modules.BluetoothModule;
import com.example.feu.beaconscanner.dagger.modules.ContextModule;
import com.example.feu.beaconscanner.dagger.modules.DatabaseModule;
import com.example.feu.beaconscanner.dagger.modules.EventModule;

import io.realm.Realm;
import io.realm.RealmConfiguration;


public class AppSingleton extends Application {

    private static AppComponent appComponent;
    private static ActivityComponent activityComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        Realm.init(this);
        RealmConfiguration config = new RealmConfiguration.Builder().build();
        Realm.setDefaultConfiguration(config);

        appComponent = DaggerAppComponent.builder()
                        .contextModule(new ContextModule(this))
                        .databaseModule(new DatabaseModule())
                        .eventModule(new EventModule())
                        .build();

        activityComponent = DaggerActivityComponent.builder()
                .appComponent(appComponent)
                .bluetoothModule(new BluetoothModule())
                .animationModule(new AnimationModule())
                .build();
    }

    public static AppComponent appComponent() {
        return appComponent;
    }

    public static ActivityComponent activityComponent() {
        return activityComponent;
    }
}
