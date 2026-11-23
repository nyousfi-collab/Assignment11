package com.example.assignment11;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.example.assignment11.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.firebase.auth.FirebaseAuth;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        LoginFragment.LoginFragListener, NewAcctFragment.AccountFragListener,
        TripsFragment.TripsFragListener , NewTripFragment.NewTripFragListener {

    private static final String TAG = "MapsActivity";

    private GoogleMap mMap;

    private Trip currentTrip;
    private ActivityMapsBinding binding;
    private final OkHttpClient client = new OkHttpClient();

    private View mapContainer;
    private View fragmentContainer;

    // Back button closes the map and returns to the trips list
    private OnBackPressedCallback closeMapCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String apiKey = BuildConfig.MAPS_API_KEY;
        if (TextUtils.isEmpty(apiKey) || apiKey.equals("DEFAULT_API_KEY")) {
            Log.e(TAG, "No API key");
            finish();
            return;
        }
        // Needed later by the Places Autocomplete screen
        Places.initializeWithNewPlacesApiEnabled(getApplicationContext(), apiKey);

        mapContainer = findViewById(R.id.mapContainer);
        fragmentContainer = findViewById(R.id.fragmentContainer);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        closeMapCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                showTripsList();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, closeMapCallback);

        if (savedInstanceState == null) {
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                showFragment(new LoginFragment());
            } else {
                showTrips();
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
    }

    // ---------------- Navigation ----------------

    /** Replaces the screen with a fragment, without adding to the back stack. */
    private void showFragment(Fragment fragment) {
        mapContainer.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.VISIBLE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    /** Clears login/register from the back stack and shows the trips list. */
    private void showTrips() {
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        showFragment(new TripsFragment());
    }

    private void showTripsList() {
        closeMapCallback.setEnabled(false);
        mapContainer.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public void goToNewAcct() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new NewAcctFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void goBackToLogin() {
        getSupportFragmentManager().popBackStack();
    }

    /** Called by Login/NewAcct after a successful sign-in: go to the trips list. */
    @Override
    public void goBackToMap() {
        showTrips();
    }

    /** A trip was tapped in the list: show it on the map, loaded from Firebase. */
    @Override
    public void openTrip(Trip trip) {
        fragmentContainer.setVisibility(View.GONE);
        mapContainer.setVisibility(View.VISIBLE);
        closeMapCallback.setEnabled(true);
        // Wait until the map has a size, or newLatLngBounds will crash
        mapContainer.post(() -> drawTrip(trip));
    }

    @Override
    public void goToNewTrip() {

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new NewTripFragment())
                .addToBackStack(null)
                .commit();
    }

    // ---------------- Saved trip ----------------

    private void drawTrip(Trip trip) {
        if (mMap == null) return;
        mMap.clear();

        if (trip.getEncodedPolyline() != null) {
            drawRoute(new ArrayList<>(PolyUtil.decode(trip.getEncodedPolyline())));
        }

        if (trip.getStart() != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(trip.getStart().getLat(), trip.getStart().getLng()))
                    .title("Start: " + trip.getStart().getName()));
        }
        if (trip.getEnd() != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(trip.getEnd().getLat(), trip.getEnd().getLng()))
                    .title("End: " + trip.getEnd().getName()));
        }

        addStationMarkers(trip.getStartStations(), BitmapDescriptorFactory.HUE_GREEN);
        addStationMarkers(trip.getMidStations(), BitmapDescriptorFactory.HUE_ORANGE);
        addStationMarkers(trip.getEndStations(), BitmapDescriptorFactory.HUE_VIOLET);
    }

    private void addStationMarkers(List<Address> stations, float hue) {
        if (stations == null) return;
        for (Address s : stations) {
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(s.getLat(), s.getLng()))
                    .title(s.getName())
                    .snippet(s.getAddress())
                    .icon(BitmapDescriptorFactory.defaultMarker(hue)));
        }
    }

    void drawRoute(ArrayList<LatLng> path) {
        if (path.isEmpty()) return;
        mMap.addPolyline(new PolylineOptions().addAll(path).width(10).color(Color.BLUE));
        LatLngBounds.Builder b = new LatLngBounds.Builder();
        for (LatLng p : path) b.include(p);
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(b.build(), 100));
    }

    // ---------------- New trip (Routes API + Places API) ----------------
    // Used by the "new trip" screen once start and end are picked.

    void getRoute(LatLng origin, LatLng dest) {
        String url = "https://routes.googleapis.com/directions/v2:computeRoutes";

        String body = "{"
                + "\"origin\":{\"location\":{\"latLng\":{\"latitude\":" + origin.latitude
                + ",\"longitude\":" + origin.longitude + "}}},"
                + "\"destination\":{\"location\":{\"latLng\":{\"latitude\":" + dest.latitude
                + ",\"longitude\":" + dest.longitude + "}}},"
                + "\"travelMode\":\"DRIVE\""
                + "}";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-Goog-Api-Key", BuildConfig.MAPS_API_KEY)
                .addHeader("X-Goog-FieldMask", "routes.polyline.encodedPolyline")
                .post(RequestBody.create(body, MediaType.get("application/json")))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Route request failed", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String text = response.body().string();
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Route error: " + text);
                    return;
                }
                try {
                    String encoded = new JSONObject(text)
                            .getJSONArray("routes").getJSONObject(0)
                            .getJSONObject("polyline").getString("encodedPolyline");

                    ArrayList<LatLng> path = new ArrayList<>(PolyUtil.decode(encoded));

                    runOnUiThread(() -> drawRoute(path));
                    getGasStations(path.get(0));                  // start
                    getGasStations(path.get(path.size() / 2));    // mid path
                    getGasStations(path.get(path.size() - 1));    // end
                } catch (JSONException e) {
                    Log.e(TAG, "Bad route JSON", e);
                }
            }
        });
    }

    void getGasStations(LatLng center) {
        String url = "https://places.googleapis.com/v1/places:searchNearby";

        JSONObject body = new JSONObject();
        try {
            JSONObject circle = new JSONObject()
                    .put("center", new JSONObject()
                            .put("latitude", center.latitude)
                            .put("longitude", center.longitude))
                    .put("radius", 5000);

            body.put("includedTypes", new JSONArray().put("gas_station"));
            body.put("maxResultCount", 5);
            body.put("locationRestriction", new JSONObject().put("circle", circle));
        } catch (JSONException e) {
            Log.e(TAG, "Bad request JSON", e);
            return;
        }

        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-Goog-Api-Key", BuildConfig.MAPS_API_KEY)
                .addHeader("X-Goog-FieldMask", "places.displayName,places.location,places.formattedAddress")
                .post(RequestBody.create(body.toString(), MediaType.get("application/json")))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Places request failed", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String text = response.body().string();
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Places error: " + text);
                    return;
                }
                try {
                    JSONArray places = new JSONObject(text).optJSONArray("places");
                    if (places == null) return;
                    for (int i = 0; i < places.length(); i++) {
                        JSONObject p = places.getJSONObject(i);
                        String name = p.getJSONObject("displayName").getString("text");
                        JSONObject loc = p.getJSONObject("location");
                        LatLng pos = new LatLng(loc.getDouble("latitude"), loc.getDouble("longitude"));
                        runOnUiThread(() -> mMap.addMarker(new MarkerOptions().position(pos).title(name)));
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Bad places JSON", e);
                }
            }
        });


    }

    @Override
    public void onTripPlanned(Address start, Address end, Date startTime) {
        currentTrip = new Trip(start, end);
        currentTrip.setStartTime(startTime);

        fragmentContainer.setVisibility(View.GONE);
        mapContainer.setVisibility(View.VISIBLE);
        closeMapCallback.setEnabled(true);

        mapContainer.post(() -> {
            mMap.clear();
            getRoute(new LatLng(start.getLat(), start.getLng()),
                    new LatLng(end.getLat(), end.getLng()));
        });
    }
}