package com.himusharier.auth.dto.response;

import java.time.Instant;
import java.util.UUID;

public class DeviceResponse {
    private UUID id;
    private String deviceInfo;
    private String ipAddress;
    private Instant lastUsed;
    private boolean current;

    public DeviceResponse() {
    }

    public DeviceResponse(UUID id, String deviceInfo, String ipAddress, Instant lastUsed, boolean current) {
        this.id = id;
        this.deviceInfo = deviceInfo;
        this.ipAddress = ipAddress;
        this.lastUsed = lastUsed;
        this.current = current;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Instant getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(Instant lastUsed) {
        this.lastUsed = lastUsed;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }
}

