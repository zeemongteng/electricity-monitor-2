package com.electricitymonitor.model;

public enum NotificationKind {
    SPIKE,        // one reading is far above the recent average
    HOURLY_HIGH,  // this hour is far above the usual for this hour of day
    BUDGET        // today's total passed the daily budget
}
