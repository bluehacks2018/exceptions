package  com.example.feu.beaconscanner.utils;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.Nullable;

import com.example.feu.beaconscanner.events.Events;

import javax.inject.Inject;

import io.reactivex.Flowable;
import io.reactivex.processors.BehaviorProcessor;

public class BluetoothManager {

    private final BluetoothAdapter adapter;

    private final BehaviorProcessor<Object> subject;

    @Inject
    public BluetoothManager(@Nullable BluetoothAdapter adapter, Context context) {
        this.adapter = adapter;
        this.subject = BehaviorProcessor.createDefault(new Events.BluetoothState(adapter != null ? adapter.getState() : BluetoothAdapter.STATE_OFF));

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                    subject.onNext(new Events.BluetoothState(
                            intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                                    BluetoothAdapter.ERROR)));
                }
            }
        };
        context.registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    public void disable() {
        if (adapter != null) {
            adapter.disable();
        }
    }

    public void enable() {
        if (adapter!= null) {
            adapter.enable();
        }
    }

    public boolean isEnabled() {
        return (adapter != null) && adapter.isEnabled();
    }

    public Flowable<Object> asFlowable() {
        return subject;
    }

    public void toggle() {
        if (isEnabled()) {
            disable();
        } else {
            enable();
        }
    }
}
