package com.example.assignment11;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Shows the signed-in user's saved trips from Firestore (users/{uid}/trips).
 * Tapping a trip tells the activity to open it on the map.
 */
public class TripsFragment extends Fragment {

    public interface TripsFragListener {
        void openTrip(Trip trip);
        void goToNewTrip();

        void logout();
    }

    private TripsFragListener listener;
    private final ArrayList<Trip> trips = new ArrayList<>();
    private TripAdapter adapter;
    private ListenerRegistration registration;

    public TripsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof TripsFragListener) {
            listener = (TripsFragListener) context;
        } else {
            throw new RuntimeException(context + " must implement TripsFragListener");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trips, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.tripsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.addItemDecoration(
                new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        adapter = new TripAdapter(trips, trip -> {
            if (listener != null) listener.openTrip(trip);
        });
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.addTripButton).setOnClickListener(v -> {
            if (listener != null) listener.goToNewTrip();
        });

        view.findViewById(R.id.logoutButton).setOnClickListener(v -> {
            if (listener != null) listener.logout();
        });

        loadTrips();
    }

    private void loadTrips() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Live listener: the list updates by itself when a new trip is saved.
        registration = FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .collection("trips")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) {
                        Log.e("TripsFragment", "Failed to load trips", e);
                        return;
                    }
                    trips.clear();
                    trips.addAll(snapshot.toObjects(Trip.class));
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (registration != null) registration.remove();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    // ---------------- Adapter ----------------

    static class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {

        interface OnTripClick {
            void onClick(Trip trip);
        }

        private final List<Trip> trips;
        private final OnTripClick onTripClick;
        private final SimpleDateFormat format =
                new SimpleDateFormat("MM/dd/yyyy - h:mma", Locale.US);

        TripAdapter(List<Trip> trips, OnTripClick onTripClick) {
            this.trips = trips;
            this.onTripClick = onTripClick;
        }

        @NonNull
        @Override
        public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.trip_list_item, parent, false);
            return new TripViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
            Trip trip = trips.get(position);

            holder.start.setText(trip.getStart() != null ? trip.getStart().getName() : "");
            holder.end.setText(trip.getEnd() != null ? trip.getEnd().getName() : "");

            // Show departure time; fall back to when it was saved
            Date when = trip.getStartTime() != null ? trip.getStartTime() : trip.getCreatedAt();
            holder.date.setText(when != null ? format.format(when) : "");

            holder.itemView.setOnClickListener(v -> onTripClick.onClick(trip));

        }

        @Override
        public int getItemCount() {
            return trips.size();
        }

        static class TripViewHolder extends RecyclerView.ViewHolder {
            final TextView start, end, date;

            TripViewHolder(@NonNull View itemView) {
                super(itemView);
                start = itemView.findViewById(R.id.tripStart);
                end = itemView.findViewById(R.id.tripEnd);
                date = itemView.findViewById(R.id.tripStartDateTime);
            }
        }
    }
}