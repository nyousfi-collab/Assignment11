package com.example.assignment11;

import static android.view.View.INVISIBLE;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.assignment11.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnPolylineClickListener, GoogleMap.OnPolygonClickListener,
        LoginFragment.LoginFragListener, NewAcctFragment.AccountFragListener {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;

    private FirebaseAuth mAuth;

    private final OkHttpClient client = new OkHttpClient();
    ArrayList<LatLng> coords = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        mAuth = FirebaseAuth.getInstance();

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Define a variable to hold the Places API key.
        String apiKey = BuildConfig.MAPS_API_KEY;

        // Log an error if apiKey is not set.
        if (TextUtils.isEmpty(apiKey) || apiKey.equals("DEFAULT_API_KEY")) {
            Log.e("Places test", "No api key");
            finish();
            return;
        }

        // Initialize the SDK
        Places.initializeWithNewPlacesApiEnabled(getApplicationContext(), apiKey);

        // Create a new PlacesClient instance
        PlacesClient placesClient = Places.createClient(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        findViewById(R.id.mapContainer).setVisibility(View.GONE);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new LoginFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        // Set listeners for click events.
        googleMap.setOnPolylineClickListener(this);
        googleMap.setOnPolygonClickListener(this);

        LatLng org = new LatLng(35.2271, -80.8431);
        LatLng dest = new LatLng(35.7796, -78.6382);
        getRoute(org, dest);


    }

    /// Whatever you get from the Search function just put that lat log in.
    /// @param origin
    /// @param dest
    void getRoute(LatLng origin, LatLng dest)
    {

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
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String text = response.body().string();
                if(response.isSuccessful())
                {
                    try {
                        String encoded = new JSONObject(text)
                                .getJSONArray("routes").getJSONObject(0)
                                .getJSONObject("polyline").getString("encodedPolyline");

                        ArrayList<LatLng> path = new ArrayList<>(PolyUtil.decode(encoded));

                        runOnUiThread(() -> drawRoute(path));
                        getGasStations(path.get(0));
                        getGasStations(path.get(path.size() / 2));
                        getGasStations(path.get(path.size() - 1));

                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
                else
                {
                    Log.d("TAG", "Error: " + text);
                }

            }
        });
    }

    void getGasStations(LatLng center)
    {
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
        }
        catch (JSONException e)
        {
            e.printStackTrace();
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
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException
            {
                String text = response.body().string();
                if(response.isSuccessful())
                {
                    try {
                        JSONArray places = new JSONObject(text).optJSONArray("places");
                        if (places == null) return;
                        for (int i = 0; i < places.length(); i++)
                        {
                            JSONObject p = places.getJSONObject(i);
                            String name = p.getJSONObject("displayName").getString("text");
                            JSONObject loc = p.getJSONObject("location");
                            LatLng pos = new LatLng(loc.getDouble("latitude"), loc.getDouble("longitude"));
                            runOnUiThread(() -> mMap.addMarker(new MarkerOptions().position(pos).title(name)));

                        }

                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
                else
                {
                    Log.d("TAG", "Error: " + text);
                }
            }
        });

    }

    void drawRoute(ArrayList<LatLng> path) {
        mMap.addPolyline(new PolylineOptions().addAll(path).width(10).color(Color.GREEN));
        LatLngBounds.Builder b = new LatLngBounds.Builder();
        for (LatLng p : path) b.include(p);
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(b.build(), 100));
    }
    @Override
    public void onPolygonClick(@NonNull Polygon polygon) {

    }

    @Override
    public void onPolylineClick(@NonNull Polyline polyline) {

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

    @Override
    public void goBackToMap() {
        findViewById(R.id.mapContainer).setVisibility(View.VISIBLE);
        getSupportFragmentManager().popBackStack();
    }
}