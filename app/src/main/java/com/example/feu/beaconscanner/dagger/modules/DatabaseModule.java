package com.example.feu.beaconscanner.dagger.modules;

import dagger.Module;
import dagger.Provides;
import io.realm.Realm;

@Module
public class DatabaseModule {
    @Provides
    public Realm providesRealm() {
        return Realm.getDefaultInstance();
    }
}
