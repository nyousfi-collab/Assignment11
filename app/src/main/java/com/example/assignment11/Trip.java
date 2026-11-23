package com.example.assignment11;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Trip {
    @DocumentId
    private String id;                 // filled in by Firestore, not stored as a field

    private Address start;
    private Address end;
    private Address mid;               // midway point of the route

    private String encodedPolyline;    // from Routes API, used to redraw the route
    private int distanceMeters;
    private long durationSeconds;

    private List<Address> startStations = new ArrayList<>();
    private List<Address> midStations = new ArrayList<>();
    private List<Address> endStations = new ArrayList<>();

    @ServerTimestamp
    private Date createdAt;            // set by Firestore when saved

    public Trip() { } // required by Firestore

    public Trip(Address start, Address end) {
        this.start = start;
        this.end = end;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Address getStart() { return start; }
    public void setStart(Address start) { this.start = start; }

    public Address getEnd() { return end; }
    public void setEnd(Address end) { this.end = end; }

    public Address getMid() { return mid; }
    public void setMid(Address mid) { this.mid = mid; }

    public String getEncodedPolyline() { return encodedPolyline; }
    public void setEncodedPolyline(String encodedPolyline) { this.encodedPolyline = encodedPolyline; }

    public int getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(int distanceMeters) { this.distanceMeters = distanceMeters; }

    public long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(long durationSeconds) { this.durationSeconds = durationSeconds; }

    public List<Address> getStartStations() { return startStations; }
    public void setStartStations(List<Address> startStations) { this.startStations = startStations; }

    public List<Address> getMidStations() { return midStations; }
    public void setMidStations(List<Address> midStations) { this.midStations = midStations; }

    public List<Address> getEndStations() { return endStations; }
    public void setEndStations(List<Address> endStations) { this.endStations = endStations; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}