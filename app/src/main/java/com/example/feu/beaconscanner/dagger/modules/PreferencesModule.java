package com.example.feu.beaconscanner.dagger.modules;

import android.content.Context;

import com.example.feu.beaconscanner.utils.PreferencesHelper;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;


@Module
public class PreferencesModule {

    @Provides @Singleton
    public PreferencesHelper providesPreferencesHelper(Context ctx) {
        return new PreferencesHelper(ctx);
    }
}
