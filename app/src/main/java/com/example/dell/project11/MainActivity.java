package com.example.dell.project11;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import DbSchema.ListStructure;
import DbSchema.SharedListStructure;
import DbSchema.SharedMemberStructure;
import DbSchema.UserStructure;


public class MainActivity extends AppCompatActivity


   implements NavigationView.OnNavigationItemSelectedListener

    {

        private TextView labelList, userEmail, userName;
        private ImageView userImage;
        private ProgressDialog progressDialog;
        private DatabaseReference mActiveLists, mMembers, mUsers, mSharedWith, mExpenses;
        private static final String LOG_TAG = MainActivity.class.getSimpleName();
        private ValueEventListener mRefreshListListener;
        private FirebaseAuth mAuth;
        private FirebaseAuth.AuthStateListener mAuthListener;
        private FirebaseUser mOwner;
        private ArrayAdapter<String> listAdapter;
        private String OWNER_DISPLAY_NAME = "Anonymous", OWNER_EMAIL;
        private Uri OWNER_PHOTO_URL;
        private List<String> listKey;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            Toolbar toolbar = findViewById(R.id.toolbar);
           //etSupportActionBar(toolbar);

           FloatingActionButton fab = findViewById(R.id.fab);
          fab.setImageResource(R.drawable.ic_fab_add);
           fab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFA07A")));
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    addListItem();
                }
            });

            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.addDrawerListener(toggle);
            toggle.syncState();

            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            navigationView.setNavigationItemSelectedListener(this);

            mAuth = FirebaseAuth.getInstance();
            mOwner = mAuth.getCurrentUser();

            DatabaseReference mUserRef = FirebaseDatabase.getInstance().getReference();
            mActiveLists = mUserRef.child("activeLists");
            mMembers = mUserRef.child("members");
            mUsers = mUserRef.child("users");
            mSharedWith = mUserRef.child("sharedWith");
            mExpenses = mUserRef.child("expenses");

            ListView listViewMain = (ListView) findViewById(R.id.listMain);
            labelList = (TextView) findViewById(R.id.textView4);

            listKey = new ArrayList<>();
            listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1);
            listViewMain.setAdapter(listAdapter);



            View navHeaderView = navigationView.inflateHeaderView(R.layout.nav_header_main);
            userName = (TextView) navHeaderView.findViewById(R.id.tVUserName);
            userEmail = (TextView) navHeaderView.findViewById(R.id.tvUserEmail);
            userImage = (ImageView) navHeaderView.findViewById(R.id.imageViewUserImage);

            userImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //TODO: Logic for changing profile photo
                    Toast.makeText(MainActivity.this, "Wanna change photo?", Toast.LENGTH_SHORT).show();
                }
            });

            if (mOwner != null) {
                OWNER_EMAIL = mOwner.getEmail();
                getUserInfo();
            }

            mAuthListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    if(firebaseAuth.getCurrentUser()==null){
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        finish();
                    }
                }
            };

            startProgressDialog();
            makeList();

            listViewMain.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    final String listSelected = listAdapter.getItem(position);
                    final String listKeySelected = listKey.get(position);

                    Intent intent = new Intent(MainActivity.this, AddExpense.class);
                    intent.putExtra("listId", listKeySelected);
                    intent.putExtra("listName", listSelected);
                    intent.putExtra("ownerEmail", OWNER_EMAIL);
                    intent.putExtra("ownerDisplayName", OWNER_DISPLAY_NAME);
                    startActivity(intent);
                }
            });


            listViewMain.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long arg3) {
                    final String listKeySelected = listKey.get(position);
                    mActiveLists.child(listKeySelected).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            ListStructure ls = dataSnapshot.getValue(ListStructure.class);
                            assert ls != null;
                            if(ls.getOwner().equals(OWNER_EMAIL)) deleteRenameList(position);
                            else Toast.makeText(MainActivity.this, "Only list owner can edit the list.", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                    return true;
                }
            });
        }

        private void makeList() {
            mRefreshListListener = mMembers.child(LoginActivity.convertEmail(OWNER_EMAIL)).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    listAdapter.clear();
                    listKey.clear();
                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                        SharedListStructure lms = childSnapshot.getValue(SharedListStructure.class);
                        assert lms != null;
                        listAdapter.add(lms.getListName());
                        listKey.add(lms.getListKey());
                    }
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    labelList.setVisibility(listAdapter.isEmpty() ? View.VISIBLE : View.GONE);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(LOG_TAG, "error while loading lists");
                }
            });
        }

        public void startProgressDialog(){
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            progressDialog = ProgressDialog.show(MainActivity.this, "Just a minute", "Populating the lists...", false,
                    true, new DialogInterface.OnCancelListener() {

                        @Override
                        public void onCancel(DialogInterface dialog) {
//                        if (listAdapter.isEmpty())
//                            Toast.makeText(MainActivity.this, "Start with a new List.", Toast.LENGTH_SHORT).show();
                        }
                    });
            progressDialog.setCancelable(false);

            Runnable progressRunnable = new Runnable() {
                @Override
                public void run() {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, "There might be some error...", Toast.LENGTH_SHORT).show();
                    }
                }
            };
            Handler pdCanceller = new Handler();
            pdCanceller.postDelayed(progressRunnable, 15000);
        }

        public void deleteRenameList(int position){
            final String listNameSelected = listAdapter.getItem(position);
            final String listKeySelected = listKey.get(position);

            final AlertDialog alert = new AlertDialog.Builder(MainActivity.this).create();
            final ListView del = new ListView(MainActivity.this);
            del.setAdapter(new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, new String[]{"Delete", "Rename"}));
            alert.setView(del);
            del.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0) {

                        mActiveLists.child(listKeySelected).removeValue();

                        Query myQuery = mMembers.orderByChild("listKey").equalTo(listKeySelected);
                        myQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                for(DataSnapshot childDataSnapshot : dataSnapshot.getChildren()){
                                    for(DataSnapshot grandChildDataSnapshot : childDataSnapshot.getChildren()){
                                        grandChildDataSnapshot.getRef().removeValue();
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

//                    mMembers.child(LoginActivity.convertEmail(OWNER_EMAIL))
//                            .orderByChild("listKey")
//                            .equalTo(listKeySelected)
//                            .addListenerForSingleValueEvent(new ValueEventListener() {
//                                @Override
//                                public void onDataChange(DataSnapshot dataSnapshot) {
//                                    for(DataSnapshot snapshot: dataSnapshot.getChildren()){
//                                        snapshot.getRef().removeValue();
//                                        Log.e(LOG_TAG, "deleted in mMembers");
//                                    }
//                                }
//
//                                @Override
//                                public void onCancelled(DatabaseError databaseError) {
//
//                                }
//                            });

                        mSharedWith.child(listKeySelected).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                for(DataSnapshot childDataSnapshot : dataSnapshot.getChildren()) {
                                    SharedMemberStructure sms = childDataSnapshot.getValue(SharedMemberStructure.class);
                                    assert sms != null;
                                    String emailToUpdate = LoginActivity.convertEmail(sms.getEmail());

                                    mMembers.child(emailToUpdate)
                                            .orderByChild("listKey")
                                            .equalTo(listKeySelected)
                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(DataSnapshot dataSnapshot) {
                                                    for(DataSnapshot snapshot: dataSnapshot.getChildren()){
                                                        snapshot.getRef().removeValue();
                                                        Log.e(LOG_TAG, "deleted in mMembers");
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(DatabaseError databaseError) {

                                                }
                                            });
                                }
                                mSharedWith.child(listKeySelected).removeValue();
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

                        mExpenses.child(listKeySelected).removeValue();

                        //TODO: remove from all database

                        Toast.makeText(MainActivity.this, listNameSelected + " Deleted", Toast.LENGTH_SHORT).show();
                        alert.dismiss();
                    } else {
                        alert.dismiss();
                        AlertDialog.Builder alert1 = new AlertDialog.Builder(MainActivity.this);
                        alert1.setMessage("Enter list name here");
                        final EditText inputText = new EditText(MainActivity.this);
                        inputText.setText(listNameSelected);
                        alert1.setView(inputText);
                        alert1.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                final String newName = inputText.getText().toString();
                                if (!newName.equals("")) {
                                    final Map<String, Object> newListName = new HashMap<>();
                                    newListName.put("listName", newName);
                                    newListName.put("timestampLastUpdated", System.currentTimeMillis());
                                    mActiveLists.child(listKeySelected).updateChildren(newListName);

                                    newListName.remove("timestampLastUpdated");
                                    mSharedWith.child(listKeySelected).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            for(DataSnapshot childDataSnapshot : dataSnapshot.getChildren()){
                                                SharedMemberStructure sms = childDataSnapshot.getValue(SharedMemberStructure.class);
                                                assert sms != null;
                                                String emailToUpdate = LoginActivity.convertEmail(sms.getEmail());
                                                Log.e(LOG_TAG, "Update "+emailToUpdate);

                                                mMembers.child(emailToUpdate)
                                                        .orderByChild("listKey")
                                                        .equalTo(listKeySelected)
                                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                                Log.e(LOG_TAG, "inside onDataChange");
                                                                for(DataSnapshot snapshot: dataSnapshot.getChildren()){
                                                                    snapshot.getRef().updateChildren(newListName);
                                                                    Log.e(LOG_TAG, "name updated");
                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(DatabaseError databaseError) {

                                                            }
                                                        });
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });


                                    //TODO: rename in all database
                                } else
                                    Toast.makeText(MainActivity.this, "Enter a name", Toast.LENGTH_SHORT).show();
                            }
                        });
                        alert1.show();
                    }
                }
            });
            alert.show();
        }

        public void getUserInfo(){
            mUsers.orderByKey().equalTo(LoginActivity.convertEmail(mOwner.getEmail()))
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                                UserStructure us = snapshot.getValue(UserStructure.class);
                                assert us != null;
                                OWNER_DISPLAY_NAME = us.getName();
                                OWNER_EMAIL = us.getEmail();
                                OWNER_PHOTO_URL = Uri.parse(us.getPhotoUrl());

                                Log.e(LOG_TAG, "Display Name: "+ OWNER_DISPLAY_NAME);
                                Log.e(LOG_TAG, "Display Email: "+ OWNER_EMAIL);
                                Log.e(LOG_TAG, "Display Photo: "+ OWNER_PHOTO_URL);

                                userName.setText(OWNER_DISPLAY_NAME);
                                userEmail.setText(OWNER_EMAIL);
                                if(OWNER_PHOTO_URL!=null) Picasso.with(MainActivity.this).load(OWNER_PHOTO_URL).resize(144,144).into(userImage);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

        }

        public void addListItem() {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Create new List");
            final EditText inputText = new EditText(this);
            alert.setView(inputText);
            alert.setPositiveButton("Create", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String name = inputText.getText().toString();
                    if (!name.equals("")) {
                        DatabaseReference newList = mActiveLists.push();
                        newList.setValue(new ListStructure(name, OWNER_EMAIL, System.currentTimeMillis(), System.currentTimeMillis()));
                        //mMembers.push().setValue(new MemberStructure(OWNER_DISPLAY_NAME,OWNER_EMAIL,name,newList.getKey()));
                        mMembers.child(LoginActivity.convertEmail(OWNER_EMAIL)).push().setValue(new SharedListStructure(name, newList.getKey()));
                        mSharedWith.child(newList.getKey()).push().setValue(new SharedMemberStructure(OWNER_DISPLAY_NAME, OWNER_EMAIL));
                    } else
                        Toast.makeText(MainActivity.this, "Enter a name", Toast.LENGTH_SHORT).show();
                }
            });
            alert.show();
        }

        @Override
        public void onBackPressed() {
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START);
            } else {
                super.onBackPressed();
                ActivityCompat.finishAffinity(MainActivity.this);
            }
        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            // Inflate the menu; this adds items to the action bar if it is present.
            //getMenuInflater().inflate(R.menu.main, menu);
            return true;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {


            return super.onOptionsItemSelected(item);
        }

        @SuppressWarnings("StatementWithEmptyBody")
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            // Handle navigation view item clicks here.
            int id = item.getItemId();

            if (id == R.id.nav_home) {

            } else if (id == R.id.nav_setting) {

            } else if (id == R.id.nav_logout) {
                mAuth.signOut();
            } else if (id == R.id.nav_about) {

            } else if (id == R.id.nav_feedback) {

            }

            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
            return true;
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
            mActiveLists.removeEventListener(mRefreshListListener);
        }

        @Override
        protected void onStart() {
            super.onStart();
            mAuth.addAuthStateListener(mAuthListener);
        }
    }
