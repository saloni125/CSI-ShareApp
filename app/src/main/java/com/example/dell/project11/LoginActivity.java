package com.example.dell.project11;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import DbSchema.UserStructure;
public class LoginActivity  extends AppCompatActivity {

    private GoogleApiClient mGoogleApiClient;
    private final int RC_SIGN_IN = 1;
    private FirebaseAuth mAuth;
    private DatabaseReference mUserRef, mUsers;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private final String TAG = "Login_Activity";
    private ProgressDialog mProgressDialog;
    private FirebaseUser mFirebaseUser;
    private boolean IS_CONNECTED = false;
    private ConnectivityManager mConnectivityManager;
    private EditText etEmailLogin, etPasswordLogin;
    //private TextView tvInfo, tvInfoExtra;
    private static final String LOG_TAG = LoginActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmailLogin = (EditText) findViewById(R.id.eTEmailLogin);
        etPasswordLogin = (EditText) findViewById(R.id.eTPasswordLogin);
        Button btnLogin = (Button) findViewById(R.id.btnLogin);
        Button btnRegister = (Button) findViewById(R.id.btnRegisterActivity);
        //tvInfo = (TextView) findViewById(R.id.tVInfo);
        //tvInfoExtra = (TextView) findViewById(R.id.tVInfoNext);

        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }

        mConnectivityManager = (ConnectivityManager)getSystemService(this.CONNECTIVITY_SERVICE);
        if(mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            IS_CONNECTED = true;
        }
        else
            IS_CONNECTED = false;

        mAuth = FirebaseAuth.getInstance();
        mUserRef = FirebaseDatabase.getInstance().getReference();
        mUsers = mUserRef.child("users");

        mFirebaseUser = mAuth.getCurrentUser();

//        if (mAuth.getCurrentUser() != null) {
//            startActivity(new Intent(LoginActivity.this, MainActivity.class));
//            finish();
//        }

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if(firebaseAuth.getCurrentUser()!=null){
                    if (mProgressDialog != null && mProgressDialog.isShowing()) {
                        mProgressDialog.dismiss();
                    }
                    if(IS_CONNECTED) {
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        Log.e(TAG, "Starting MainActivity");
                        finish();
                    }else{
                        new AlertDialog.Builder(LoginActivity.this)
                                .setTitle("No Internet Service")
                                .setMessage("Check your internet connection and Try Again.")
                                .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        onStart();
                                    }
                                }).setCancelable(false)
                                .show();
                    }

                }
            }
        };

        SignInButton btnSignIn = (SignInButton) findViewById(R.id.btnSignIn);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .enableAutoManage(LoginActivity.this, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Toast.makeText(getApplicationContext(), "Some Error", Toast.LENGTH_SHORT).show();
                    }
                })
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterUserActivity.class));
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logUserIn();
            }
        });
    }

    private void logUserIn() {
        String email = etEmailLogin.getText().toString();
        String password = etPasswordLogin.getText().toString();

        etEmailLogin.setError(null);
        etPasswordLogin.setError(null);

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the mFirebaseUser entered one.
        if (!isPasswordValid(password)) {
            etPasswordLogin.setError("Invalid Password");
            focusView = etPasswordLogin;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            etEmailLogin.setError("Enter an email");
            focusView = etEmailLogin;
            cancel = true;
        } else if (!isEmailValid(email)) {
            etEmailLogin.setError("Invalid email");
            focusView = etEmailLogin;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt btnLogin and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the mFirebaseUser btnLogin attempt.
            Log.e(LOG_TAG, "email pass qualified");
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            // If sign in fails, display a message to the mFirebaseUser. If sign in succeeds
                            // the auth state listener will be notified and logic to handle the
                            // signed in mFirebaseUser can be handled in the listener.
                            if (task.isSuccessful()) {
                                FirebaseUser owner = mAuth.getCurrentUser();

//                                String username = TextUtils.isEmpty(mOwner.getDisplayName())?"Anonymous":mOwner.getDisplayName();
//                                String photo = mOwner.getPhotoUrl()==null?null:mOwner.getPhotoUrl().toString();
//
//                                mUserRef.child("users").push().setValue(new UserStructure(username,
//                                        convertEmail(mOwner.getEmail()), photo));

                                Log.e(LOG_TAG, owner + " signed in successfully.");
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                finish();
                            } else {
                                Log.e(LOG_TAG, "Signing in failed. Reason: ", (Throwable) task.getResult());
                            }
                        }
                    });
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);

        mConnectivityManager = (ConnectivityManager)getSystemService(this.CONNECTIVITY_SERVICE);
        if(mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            IS_CONNECTED = true;
        }
        else
            IS_CONNECTED = false;
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.e(TAG, "Inside onActivityResult");
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Google Sign In was successful, authenticate with Firebase
                Log.e(TAG, "result.isSuccess()");
                GoogleSignInAccount account = result.getSignInAccount();
                assert account != null;
                Log.e(TAG, "Email: " + account.getEmail());
                Log.e(TAG, "Name: " + account.getDisplayName());
                Log.e(TAG, "Photo URL: " + account.getPhotoUrl());
                Log.e(TAG, "Last Name: " + account.getFamilyName());
                firebaseAuthWithGoogle(account);
            } else {
                Log.e(TAG, "Google Sign In failed");
                // Google Sign In failed, update UI appropriately
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.e(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        mProgressDialog = ProgressDialog.show(LoginActivity.this, "Logging in", "Just a minute...", false,
                true, new DialogInterface.OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {
                        if(mFirebaseUser!=null)
                            Toast.makeText(LoginActivity.this, "Logging failed.", Toast.LENGTH_SHORT).show();
                    }
                });
        mProgressDialog.setCancelable(false);

        Runnable progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog != null && mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
            }
        };
        Handler pdCanceller = new Handler();
        pdCanceller.postDelayed(progressRunnable, 15000);

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in mFirebaseUser's information

                            Log.d(TAG, "signInWithCredential:success");
                            final FirebaseUser owner = mAuth.getCurrentUser();

                            assert owner != null;
                            final String ownerEmail = owner.getEmail();
                            Query mQuery = mUsers.orderByKey().equalTo(LoginActivity.convertEmail(ownerEmail));
                            mQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if(dataSnapshot==null) {
                                        String username = TextUtils.isEmpty(owner.getDisplayName()) ? "Anonymous" : owner.getDisplayName();
                                        String photo = owner.getPhotoUrl() == null ? "" : owner.getPhotoUrl().toString();

                                        mUsers.child(LoginActivity.convertEmail(ownerEmail)).setValue(new UserStructure(username, ownerEmail, photo));
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });

                            Toast.makeText(LoginActivity.this, "Authentication successful: "+owner, Toast.LENGTH_SHORT).show();
                            //finish();
                        } else {
                            // If sign in fails, display a message to the mFirebaseUser.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }

                    }
                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        ActivityCompat.finishAffinity(LoginActivity.this);
    }

    private boolean isEmailValid(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }

    public static String convertEmail(String email){
        return email.replace(".", ",");
    }
}


