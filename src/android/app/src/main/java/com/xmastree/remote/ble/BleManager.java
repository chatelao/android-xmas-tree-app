package com.xmastree.remote.ble;

import com.xmastree.remote.protocol.ProtocolSpec.AudioControlFrame;
import com.xmastree.remote.protocol.ProtocolSpec.LightingControlFrame;
import com.xmastree.remote.protocol.ProtocolSpec.TelemetryFrame;

public class BleManager {
    public enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING
    }

    private ConnectionState state = ConnectionState.DISCONNECTED;
    private TelemetryFrame latestTelemetry = null;
    private boolean scanning = false;

    public ConnectionState getConnectionState() {
        return state;
    }

    public TelemetryFrame getLatestTelemetry() {
        return latestTelemetry;
    }

    public boolean isScanning() {
        return scanning;
    }

    public void startScan() {
        this.scanning = true;
    }

    public void stopScan() {
        this.scanning = false;
    }

    public boolean connect(String deviceAddress) {
        if (deviceAddress != null && !deviceAddress.isEmpty()) {
            this.state = ConnectionState.CONNECTED;
            return true;
        }
        return false;
    }

    public void disconnect() {
        this.state = ConnectionState.DISCONNECTED;
    }

    public boolean sendLightingCommand(LightingControlFrame frame) {
        return state == ConnectionState.CONNECTED && frame != null;
    }

    public boolean sendAudioCommand(AudioControlFrame frame) {
        return state == ConnectionState.CONNECTED && frame != null;
    }

    public void updateTelemetry(TelemetryFrame telemetry) {
        this.latestTelemetry = telemetry;
    }
}
