package com.example.feu.beaconscanner.dagger.modules;

import com.example.feu.beaconscanner.events.RxBus;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;


@Module
public class EventModule {
    @Provides @Singleton
    public RxBus providesRxBus() {
        return new RxBus();
    }
}
