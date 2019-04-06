package com.example.dell.project11;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import DbSchema.ExpenseStructure;
import DbSchema.SharedMemberStructure;
import DbSchema.TransactionStructure;

public class ExpenseDetail extends AppCompatActivity {
    private Spinner spinnerCategory;
    private Button btnPaidBy;
    private String listName, listMembers[], listId, OWNER_DISPLAY_NAME, OWNER_EMAIL, paidByEmail;
    private EditText etAmount;
    private TextView tvMemberSelected;
    private List<String> mSelectedMembers;
    private Map<String, String> mSelectedMemberDetails;
    private DatabaseReference mUserRef, mExpenses, mActiveLists, mTransaction, mSharedWith;
    private static final String LOG_TAG = ExpenseDetail.class.getSimpleName();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_detail);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Expense Details");

        mSelectedMembers = new ArrayList<>();
        mSelectedMemberDetails = new HashMap<>();

        final String categories[] = new String[]{"Games","Movies","Sports","Food","Groceries","Household Supplies","Maintenance",
                "Mortgage","Rent","Services","Clothing","Gift","Insurance","Medicine","Taxes","Travelling","Gas/Fuel","Hotel","Parking",
                "Cleaning","Electricity","Water","Phone/TV/Internet","Miscellaneous"};

        listName = getIntent().getStringExtra("listName");
        listMembers = getIntent().getStringExtra("memberList").split(",");
        OWNER_EMAIL = getIntent().getStringExtra("ownerEmail");
        OWNER_DISPLAY_NAME = getIntent().getStringExtra("ownerDisplayName");
        listId = getIntent().getStringExtra("listId");

        paidByEmail = OWNER_EMAIL;

        etAmount = (EditText) findViewById(R.id.EtAmount);
        Button btnSplit = (Button) findViewById(R.id.btnSplit);
        //tvSymbol = (TextView) findViewById(R.id.TvSymbol);
        tvMemberSelected = (TextView) findViewById(R.id.TvMem);
        spinnerCategory = (Spinner) findViewById(R.id.spinnerCategory);
        //tvPaidBy = (TextView) findViewById(R.id.tvPaidByInfo);
        btnPaidBy = (Button) findViewById(R.id.btnPaidBy);

        mUserRef = FirebaseDatabase.getInstance().getReference();
        mActiveLists = mUserRef.child("activeLists");
        mExpenses = mUserRef.child("expenses");
        mTransaction = mUserRef.child("transactions");
        mSharedWith = mUserRef.child("sharedWith");

        spinnerCategory.setAdapter( new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, categories));

        setUpDialog();

        btnPaidBy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final AlertDialog alert = new AlertDialog.Builder(ExpenseDetail.this).create();
                final ListView mem = new ListView(ExpenseDetail.this);
                mem.setAdapter(new ArrayAdapter<>(ExpenseDetail.this, android.R.layout.simple_list_item_1, mSelectedMembers));
                alert.setView(mem);
                mem.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        String memPaid = mSelectedMembers.get(i);
                        btnPaidBy.setText(memPaid);
                        if(!memPaid.equals("You")) paidByEmail = mSelectedMemberDetails.get(mSelectedMembers.get(i));
                        Log.e(LOG_TAG, "paid By: "+paidByEmail);
                        alert.dismiss();
                    }
                });
                alert.show();
            }
        });

        btnSplit.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                split();
            }
        });
    }

    private void setUpDialog(){
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Select Members")
                .setMultiChoiceItems(listMembers, null, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int indexSelected, boolean isChecked) {
                        if (isChecked) {
                            // If the user checked the item, ic_fab_add it to the selected items
                            mSelectedMembers.add(listMembers[indexSelected]);
                        } else if (mSelectedMembers.contains(listMembers[indexSelected])) {
                            // Else, if the item is already in the array, remove it
                            mSelectedMembers.remove(listMembers[indexSelected]);
                        }
                    }
                }).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if(mSelectedMembers.size()!=0) {
                            final String displayText = "Members: " + TextUtils.join(", ", mSelectedMembers);
                            tvMemberSelected.setText(displayText);
                            tvMemberSelected.setTextSize(24);
                            dialog.dismiss();

                            mSharedWith.child(listId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    for(DataSnapshot snapshot: dataSnapshot.getChildren()){
                                        SharedMemberStructure sms = snapshot.getValue(SharedMemberStructure.class);
                                        assert sms != null;
                                        if(mSelectedMembers.contains(sms.getName()))
                                            mSelectedMemberDetails.put(sms.getName(), sms.getEmail());
                                    }
                                }
                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                        }
                        else {
                            Toast.makeText(ExpenseDetail.this, "You need to select a member.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                }).create();
        dialog.setCancelable(false);
        dialog.show();
    }

    private void split() {
        //String members = tvMemberSelected.getText().toString();
        String finalAmount = etAmount.getText().toString();
        String categorySelected = spinnerCategory.getSelectedItem().toString();
        String members = "";

        for(String email : mSelectedMemberDetails.values()) {
            members += email + ", ";
        }

        if(TextUtils.isEmpty(members)){
            Toast.makeText(ExpenseDetail.this, "You need to select a member.", Toast.LENGTH_SHORT).show();
            finish();
        }

        if(!TextUtils.isEmpty(finalAmount)){
            float amt = Float.parseFloat(finalAmount);
            float share = (float) (Math.round(amt/mSelectedMembers.size() * 100.0)/100.0);

            //TODO: expense with unique ID
            mExpenses.child(listId).push().setValue(new ExpenseStructure(categorySelected,paidByEmail,members.substring(0,members.length()-2),amt));

            Query mQuery = mTransaction.child(listId).child(LoginActivity.convertEmail(paidByEmail));
            for(String email : mSelectedMemberDetails.values()){
                if(!paidByEmail.equals(email)) mQuery.getRef().push().setValue(new TransactionStructure(email, share));
            }

            Map<String, Object> updateRecentTime = new HashMap<>();
            updateRecentTime.put("timestampLastUpdated", System.currentTimeMillis());
            mActiveLists.child(listId).updateChildren(updateRecentTime);

            finish();
        }
        else{
            Toast.makeText(ExpenseDetail.this, "Enter a valid Amount.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
        }
        return true;
    }

}
