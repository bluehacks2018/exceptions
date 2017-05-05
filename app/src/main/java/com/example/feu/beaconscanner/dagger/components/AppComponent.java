package com.example.feu.beaconscanner.dagger.components;

import android.content.Context;

import com.example.feu.beaconscanner.dagger.modules.AnalyticsModule;
import com.example.feu.beaconscanner.dagger.modules.ContextModule;
import com.example.feu.beaconscanner.dagger.modules.DatabaseModule;
import com.example.feu.beaconscanner.dagger.modules.EventModule;
import com.example.feu.beaconscanner.dagger.modules.PreferencesModule;
import com.example.feu.beaconscanner.events.RxBus;
import com.example.feu.beaconscanner.utils.PreferencesHelper;
import com.google.firebase.analytics.FirebaseAnalytics;

import javax.inject.Singleton;

import dagger.Component;
import io.realm.Realm;

@Singleton
@Component(modules = {
        ContextModule.class,
        DatabaseModule.class,
        EventModule.class,
        PreferencesModule.class,
        AnalyticsModule.class
})
public interface AppComponent {
    Context context();
    Realm realm();
    RxBus rxBus();
    PreferencesHelper prefs();
    FirebaseAnalytics tracker();
}
