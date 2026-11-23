package com.example.assignment11;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.assignment11.databinding.FragmentNewAcctBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class NewAcctFragment extends Fragment {

    FragmentNewAcctBinding binding;
    AccountFragListener mListener;

    private FirebaseAuth mAuth;

    public NewAcctFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentNewAcctBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActivity().setTitle("Create New Account");
        mAuth = FirebaseAuth.getInstance();

        // Submit button
        binding.buttonSubmit1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get inputs
                String email = binding.editTextSignUpEmail.getText().toString();
                String password = binding.editTextSignUpPass.getText().toString();

                // check if info is empty
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(getContext(), "Please enter all fields", Toast.LENGTH_SHORT).show();
                } else {
                    // Firebase account creation
                    mAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(getActivity(), task -> {
                                if (task.isSuccessful()) {
                                    mListener.goBackToLogin();
                                } else {
                                    Toast.makeText(getContext(), "Authentication failed.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }
        });

        // Cancel button
        binding.buttonCancel1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.goBackToLogin();
            }
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mListener = (AccountFragListener) context;
    }

    public interface AccountFragListener{
        void goBackToLogin();
        void goBackToMap();
    }
}