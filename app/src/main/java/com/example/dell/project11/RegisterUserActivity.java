package com.example.dell.project11;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import DbSchema.UserStructure;


public class RegisterUserActivity extends AppCompatActivity {
    private EditText etEmail, etPassword, etUsername;
    private DatabaseReference mDatabaseReference;
    private String newEmail, newPassword, newUserName;
    private static final String LOG_TAG = RegisterUserActivity.class.getSimpleName();
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_user);

        etUsername = (EditText) findViewById(R.id.eTName);
        etEmail = (EditText) findViewById(R.id.eTEmail);
        etPassword = (EditText) findViewById(R.id.eTPassword);
        Button btnRegister = (Button) findViewById(R.id.btnRegister);

        mAuth = FirebaseAuth.getInstance();

        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                newEmail = etEmail.getText().toString();
                newPassword = etPassword.getText().toString();
                newUserName = etUsername.getText().toString();

                etEmail.setError(null);
                etPassword.setError(null);
                etUsername.setError(null);

                boolean cancel = false;
                View focusView = null;

                // Check for a valid etPassword, if the user entered one.
                if (!isPasswordValid(newPassword)) {
                    etPassword.setError("Invalid Password");
                    focusView = etPassword;
                    cancel = true;
                }

                if (TextUtils.isEmpty(newUserName)) {
                    etUsername.setError("Enter a etUsername");
                    focusView = etUsername;
                    cancel = true;
                }

                // Check for a valid etEmail address.
                if (TextUtils.isEmpty(newEmail)) {
                    etEmail.setError("Enter an etEmail");
                    focusView = etEmail;
                    cancel = true;
                } else if (!isEmailValid(newEmail)) {
                    etEmail.setError("Invalid etEmail");
                    focusView = etEmail;
                    cancel = true;
                }

                if (cancel) {
                    // There was an error; don't attempt login and focus the first
                    // form field with an error.
                    focusView.requestFocus();
                } else {
                    // Show a progress spinner, and kick off a background task to
                    // perform the user login attempt.
                    mAuth.createUserWithEmailAndPassword(newEmail, newPassword)
                            .addOnCompleteListener(RegisterUserActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        // Sign in success, update UI with the signed-in user's information
                                        FirebaseUser owner = mAuth.getCurrentUser();
                                        Log.e(LOG_TAG, owner + "registered successfully.");

                                        mDatabaseReference.child("users")
                                                .child(LoginActivity.convertEmail(newEmail))
                                                .setValue(new UserStructure(newUserName, newEmail, ""));
                                        //mAuth.signOut();
                                        //startActivity(new Intent(RegisterUserActivity.this, LoginActivity.class));
                                        finish();
                                    } else {
                                        // If sign in fails, display a message to the user.
                                        Log.e(LOG_TAG, "Registration failed. Reason: ", task.getException());
                                    }
                                }
                            });
                }
            }
        });
    }

    private boolean isEmailValid(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }
}


