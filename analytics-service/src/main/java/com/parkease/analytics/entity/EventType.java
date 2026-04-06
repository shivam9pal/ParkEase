package com.parkease.analytics.entity;

public final class EventType {
    public static final String BOOKING_CREATED = "BOOKING_CREATED";
    public static final String CHECKIN         = "CHECKIN";
    public static final String CHECKOUT        = "CHECKOUT";
    public static final String CANCELLED       = "CANCELLED";
    public static final String SCHEDULED       = "SCHEDULED";

    private EventType() {}
}