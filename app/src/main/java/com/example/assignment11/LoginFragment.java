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

import com.example.assignment11.databinding.FragmentLoginBinding;
import com.google.firebase.auth.FirebaseAuth;


public class LoginFragment extends Fragment {

    FragmentLoginBinding binding;
    LoginFragListener mListener;

    FirebaseAuth mAuth;

    public LoginFragment() {
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
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActivity().setTitle("Login");
        mAuth = FirebaseAuth.getInstance();

        // Login button
        binding.buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = binding.editTextEmailAddress.getText().toString();
                String password = binding.editTextPassword.getText().toString();

                // check if email and password are empty
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(getContext(), "Please enter email and password", Toast.LENGTH_SHORT).show();
                } else {
                    // Firebase authentication
                    mAuth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(getActivity(), task -> {
                                if (task.isSuccessful()) {
                                    mListener.goBackToMap();
                                } else {
                                    Toast.makeText(getContext(), "Authentication failed."+task.getResult(),
                                            Toast.LENGTH_SHORT).show();

                                }
                            });
                }
            }
        });

        // Create New Account button
        binding.buttonCreateNewAcct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.goToNewAcct();
            }
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mListener = (LoginFragListener) context;
    }

    public interface LoginFragListener {
        void goToNewAcct();
        void goBackToMap();
    }
}