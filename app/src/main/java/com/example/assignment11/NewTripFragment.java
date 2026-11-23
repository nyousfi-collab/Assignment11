package com.example.assignment11;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Start + destination search using the Places Autocomplete (New) web API,
 * shows each selected address, and lets the user pick a departure date/time.
 */
public class NewTripFragment extends Fragment {

    public interface NewTripFragListener {
        void onTripPlanned(Address start, Address end, Date startTime);
    }

    private static final String TAG = "NewTripFragment";

    private NewTripFragListener listener;
    private final OkHttpClient client = new OkHttpClient();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;

    private Address start, end;
    private boolean editingStart = true;   // which box the suggestions are for
    private boolean ignoreText = false;    // true while we set text from code

    private final Calendar startTime = Calendar.getInstance();
    private final SimpleDateFormat format =
            new SimpleDateFormat("MM/dd/yyyy - h:mma", Locale.US);

    private EditText startInput, endInput;
    private TextView startAddress, endAddress;
    private Button pickTimeButton, findRouteButton;

    private final ArrayList<String> labels = new ArrayList<>();
    private final ArrayList<String> placeIds = new ArrayList<>();
    private ArrayAdapter<String> suggestionsAdapter;

    public NewTripFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof NewTripFragListener) {
            listener = (NewTripFragListener) context;
        } else {
            throw new RuntimeException(context + " must implement NewTripFragListener");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_trip, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        startInput = view.findViewById(R.id.startInput);
        endInput = view.findViewById(R.id.endInput);
        startAddress = view.findViewById(R.id.startAddress);
        endAddress = view.findViewById(R.id.endAddress);
        pickTimeButton = view.findViewById(R.id.pickTimeButton);
        findRouteButton = view.findViewById(R.id.findRouteButton);

        ListView suggestionsList = view.findViewById(R.id.suggestionsList);
        suggestionsAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, labels);
        suggestionsList.setAdapter(suggestionsAdapter);
        suggestionsList.setOnItemClickListener((parent, v, position, id) ->
                fetchPlace(placeIds.get(position), editingStart));

        watch(startInput, true);
        watch(endInput, false);

        pickTimeButton.setText(format.format(startTime.getTime()));
        pickTimeButton.setOnClickListener(v -> pickDateThenTime());

        findRouteButton.setOnClickListener(v -> {
            if (listener != null && start != null && end != null) {
                listener.onTripPlanned(start, end, startTime.getTime());
            }
        });
    }

    // ---------------- Typing → suggestions ----------------

    private void watch(EditText input, boolean isStart) {
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (ignoreText) return;
                editingStart = isStart;

                // Typing again clears the earlier selection for this box
                if (isStart) { start = null; startAddress.setText(""); }
                else { end = null; endAddress.setText(""); }
                updateFindButton();

                // Wait until the user pauses typing before calling the API
                if (pendingSearch != null) handler.removeCallbacks(pendingSearch);
                String query = s.toString().trim();
                if (query.length() < 3) {
                    showSuggestions(new ArrayList<>(), new ArrayList<>());
                    return;
                }
                pendingSearch = () -> autocomplete(query);
                handler.postDelayed(pendingSearch, 300);
            }
        });
    }

    private void autocomplete(String query) {
        JSONObject body = new JSONObject();
        try {
            body.put("input", query);
        } catch (JSONException e) {
            return;
        }

        Request request = new Request.Builder()
                .url("https://places.googleapis.com/v1/places:autocomplete")
                .addHeader("X-Goog-Api-Key", BuildConfig.MAPS_API_KEY)
                .post(RequestBody.create(body.toString(), MediaType.get("application/json")))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Autocomplete failed", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String text = response.body().string();
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Autocomplete error: " + text);
                    return;
                }
                List<String> ids = new ArrayList<>();
                List<String> ls = new ArrayList<>();
                try {
                    JSONArray suggestions = new JSONObject(text).optJSONArray("suggestions");
                    if (suggestions != null) {
                        for (int i = 0; i < suggestions.length(); i++) {
                            JSONObject p = suggestions.getJSONObject(i).optJSONObject("placePrediction");
                            if (p == null) continue;
                            ids.add(p.getString("placeId"));
                            ls.add(p.getJSONObject("text").getString("text"));
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Bad autocomplete JSON", e);
                }
                onUi(() -> showSuggestions(ids, ls));
            }
        });
    }

    private void showSuggestions(List<String> ids, List<String> ls) {
        placeIds.clear();
        placeIds.addAll(ids);
        labels.clear();
        labels.addAll(ls);
        suggestionsAdapter.notifyDataSetChanged();
    }

    // ---------------- Tap a suggestion → get its address + location ----------------

    private void fetchPlace(String placeId, boolean isStart) {
        Request request = new Request.Builder()
                .url("https://places.googleapis.com/v1/places/" + placeId)
                .addHeader("X-Goog-Api-Key", BuildConfig.MAPS_API_KEY)
                .addHeader("X-Goog-FieldMask", "displayName,formattedAddress,location")
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Place details failed", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String text = response.body().string();
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Place details error: " + text);
                    return;
                }
                try {
                    JSONObject p = new JSONObject(text);
                    String name = p.getJSONObject("displayName").getString("text");
                    String address = p.getString("formattedAddress");
                    JSONObject loc = p.getJSONObject("location");
                    Address a = new Address(name, address,
                            loc.getDouble("latitude"), loc.getDouble("longitude"));

                    onUi(() -> {
                        ignoreText = true;
                        if (isStart) {
                            start = a;
                            startInput.setText(name);
                            startAddress.setText(address);
                        } else {
                            end = a;
                            endInput.setText(name);
                            endAddress.setText(address);
                        }
                        ignoreText = false;
                        showSuggestions(new ArrayList<>(), new ArrayList<>());
                        updateFindButton();
                    });
                } catch (JSONException e) {
                    Log.e(TAG, "Bad place JSON", e);
                }
            }
        });
    }

    // ---------------- Helpers ----------------

    private void updateFindButton() {
        findRouteButton.setEnabled(start != null && end != null);
    }

    /** Runs on the main thread, only if the screen is still showing. */
    private void onUi(Runnable r) {
        handler.post(() -> {
            if (isAdded() && getView() != null) r.run();
        });
    }

    private void pickDateThenTime() {
        new DatePickerDialog(requireContext(), (dp, year, month, day) -> {
            startTime.set(year, month, day);

            new TimePickerDialog(requireContext(), (tp, hour, minute) -> {
                startTime.set(Calendar.HOUR_OF_DAY, hour);
                startTime.set(Calendar.MINUTE, minute);
                pickTimeButton.setText(format.format(startTime.getTime()));
            }, startTime.get(Calendar.HOUR_OF_DAY), startTime.get(Calendar.MINUTE), false).show();

        }, startTime.get(Calendar.YEAR), startTime.get(Calendar.MONTH),
                startTime.get(Calendar.DAY_OF_MONTH)).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }
}
