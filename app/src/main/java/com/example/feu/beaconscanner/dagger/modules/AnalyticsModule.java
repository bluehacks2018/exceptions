package com.example.feu.beaconscanner.dagger.modules;

import android.content.Context;

import com.google.firebase.analytics.FirebaseAnalytics;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;


@Module
public class AnalyticsModule {

    @Provides @Singleton
    public FirebaseAnalytics providesFirebaseAnalytics(Context ctx) {
        return FirebaseAnalytics.getInstance(ctx);
    }
}
