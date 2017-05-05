package com.example.feu.beaconscanner.dagger.components;

import com.example.feu.beaconscanner.dagger.PerActivity;
import com.example.feu.beaconscanner.dagger.modules.AnimationModule;
import com.example.feu.beaconscanner.dagger.modules.BluetoothModule;
import com.example.feu.beaconscanner.features.beaconList.MainActivity;
import com.example.feu.beaconscanner.features.settings.SettingsActivity;

import dagger.Component;

@PerActivity
@Component(
    dependencies = AppComponent.class,
    modules = {
            BluetoothModule.class,
            AnimationModule.class
    })
public interface ActivityComponent {
    void inject(MainActivity activity);
    void inject(SettingsActivity activity);
}
