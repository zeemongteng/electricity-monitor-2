package com.electricitymonitor.model;

public enum ReadingSource {
    AUTO,     // auto simulator
    MANUAL,   // simulate button
    HISTORY,  // generated history
    DEVICE    // real device posting to /api/readings
}
