package com.example.dell.project11;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import net.frakbot.jumpingbeans.JumpingBeans;

import java.util.HashMap;
import java.util.Map;

import DbSchema.ExpenseStructure;
import DbSchema.ListStructure;
import DbSchema.SharedListStructure;
import DbSchema.SharedMemberStructure;
import DbSchema.TransactionStructure;

public class AddExpense extends AppCompatActivity {
    private String listId, listName, OWNER_DISPLAY_NAME, OWNER_EMAIL;
    private ListView listExpense;
    private TextView tvJumpingDots;
    private DatabaseReference mUserRef, mActiveLists, mExpense, mMembers, mSharedWith, mTransactions;
    private static final String LOG_TAG = AddExpense.class.getSimpleName();
    private ValueEventListener mRefreshExpenseListener, mRefreshMemberListener;
    final private String[] allMem = {""};
    private JumpingBeans mJumpingBeans;
    private ArrayAdapter<String> mListAdapter;
    private FloatingActionButton fab;
    private Map<String, String> memberList;
    private AlertDialog mAddMemberBuilder;
    private Dialog mGenerateBillDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        mUserRef = FirebaseDatabase.getInstance().getReference();
        mActiveLists = mUserRef.child("activeLists");
        mExpense = mUserRef.child("expenses");
        mMembers = mUserRef.child("members");
        mSharedWith = mUserRef.child("sharedWith");
        mTransactions = mUserRef.child("transactions");

        listId = getIntent().getStringExtra("listId");
        listName = getIntent().getStringExtra("listName");
        OWNER_EMAIL = getIntent().getStringExtra("ownerEmail");
        OWNER_DISPLAY_NAME = getIntent().getStringExtra("ownerDisplayName");

        listExpense = (ListView) findViewById(R.id.LvExpense);
        tvJumpingDots = (TextView) findViewById(R.id.tvJumpingDots);

        memberList = new HashMap<>();

        mJumpingBeans = JumpingBeans.with(tvJumpingDots)
                .makeTextJump(0, tvJumpingDots.getText().toString().indexOf(' '))
                .setIsWave(true)
                .setLoopDuration(1000)
                .build();

        mListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1);
        listExpense.setAdapter(mListAdapter);

        fab = (FloatingActionButton) findViewById(R.id.fabAddExpense);
        fab.setImageResource(R.drawable.ic_fab_add);
        fab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFA07A")));
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startExpenseDetailActivity();
            }
        });

        fab.setVisibility(View.GONE);
        listExpense.setVisibility(View.GONE);

        mRefreshExpenseListener = mExpense.child(listId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mListAdapter.clear();
                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                    try {
                        ExpenseStructure es = childSnapshot.getValue(ExpenseStructure.class);
                        assert es != null;
                        mListAdapter.add(es.getCategory());
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "inside catch");
                        e.printStackTrace();
                    }
                }

                listExpense.setVisibility(View.VISIBLE);
                mJumpingBeans.stopJumping();
                tvJumpingDots.setVisibility(View.GONE);
                fab.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mRefreshMemberListener = mSharedWith.child(listId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                memberList.clear();
                allMem[0] = "";
                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                    try {
                        SharedMemberStructure ms = childSnapshot.getValue(SharedMemberStructure.class);
                        assert ms != null;
                        String name = ms.getName().equals(OWNER_DISPLAY_NAME)?"You":ms.getName();
                        String email = ms.getEmail();

                        allMem[0] += name + ',';
                        memberList.put(name, email);

                        //Log.e(LOG_TAG, "Adding members");
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "inside catch");
                        e.printStackTrace();
                    }
                }
                updateNames();
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        listExpense.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int i, long l) {
                mActiveLists.child(listId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        ListStructure ls = dataSnapshot.getValue(ListStructure.class);
                        assert ls != null;
                        if(ls.getOwner().equals(OWNER_EMAIL)) editExpense(i);
                        else Toast.makeText(AddExpense.this, "Only list owner can edit the list.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
                return true;
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(listName);


    }

    private void editExpense(int position){
        final String expenseSelected = mListAdapter.getItem(position);

        final AlertDialog alert = new AlertDialog.Builder(AddExpense.this).create();
        final ListView del = new ListView(AddExpense.this);
        del.setAdapter(new ArrayAdapter<>(AddExpense.this, android.R.layout.simple_list_item_1, new String[]{"Delete", "Edit"}));
        alert.setView(del);
        del.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    mExpense.child(listId)
                            .orderByChild("category").equalTo(expenseSelected)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    for(DataSnapshot snapshot: dataSnapshot.getChildren()){
                                        snapshot.getRef().removeValue();
                                    }
                                }
                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                    Toast.makeText(AddExpense.this, expenseSelected + " Deleted", Toast.LENGTH_SHORT).show();
                    alert.dismiss();
                } else {
                    //TODO: Crete logic to edit expense
                }
            }
        });
        alert.show();
    }

    private void startExpenseDetailActivity() {
        if(!allMem[0].equals("You,")){
            Intent i = new Intent(AddExpense.this, ExpenseDetail.class);
            i.putExtra("listName", listName);
            i.putExtra("listId", listId);
            i.putExtra("memberList", allMem[0].substring(0,allMem[0].length()-1));
            i.putExtra("ownerEmail", OWNER_EMAIL);
            i.putExtra("ownerDisplayName", OWNER_DISPLAY_NAME);
            startActivity(i);
        }
        else
            Toast.makeText(AddExpense.this, "Add Members.", Toast.LENGTH_SHORT).show();
    }


    public void updateNames(){
        LinearLayout llMemberView = (LinearLayout) findViewById(R.id.displayMemberLayout);
        llMemberView.removeAllViews();
        if(!memberList.isEmpty()){
            Log.e(LOG_TAG, "Adding members");
            for(String member: memberList.keySet()){
                Log.e(LOG_TAG, member);

//                }

                TextView tvMemberName = new TextView(AddExpense.this);
                tvMemberName.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT));
                tvMemberName.setText(member);
                tvMemberName.setPadding(10,10,10,10);
                llMemberView.addView(tvMemberName);
            }
        }
    }

    public void addNewMem(){

        LinearLayout layout = new LinearLayout(AddExpense.this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText memName = new EditText(AddExpense.this);
        memName.setHint("Name");
        memName.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        layout.addView(memName);

        final EditText memEmail = new EditText(AddExpense.this);
        memEmail.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS |InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        memEmail.setHint("Email");
        layout.addView(memEmail);

        if(mAddMemberBuilder !=null) mAddMemberBuilder.dismiss();
        if(mGenerateBillDialog !=null) mGenerateBillDialog.dismiss();
        mAddMemberBuilder = new AlertDialog.Builder(AddExpense.this)
                .setTitle("Enter member details")
                .setView(layout)
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        final String name = memName.getText().toString();
                        final String email = memEmail.getText().toString();

                        if(!TextUtils.isEmpty(name) && isValid(email) && !email.equals(OWNER_EMAIL)){
                            Query mQuery = mSharedWith.child(listId).orderByChild("email").equalTo(email);
                            mQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.hasChildren()) Toast.makeText(AddExpense.this, "The person already exists.", Toast.LENGTH_SHORT).show();
                                    else pushMember(name, email);
                                }
                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                        }else Toast.makeText(AddExpense.this,"Name or email incorrect",Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                }).show();
    }

    private void pushMember(final String name, final String email) {
        Query mQuery = mUserRef.child("users").orderByKey().equalTo(LoginActivity.convertEmail(email));
        mQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChildren()){
                    mMembers.child(LoginActivity.convertEmail(email)).push().setValue(new SharedListStructure(listName, listId));
                    mSharedWith.child(listId).push().setValue(new SharedMemberStructure(name, email));
                    Map<String, Object> updateRecentTime = new HashMap<>();
                    updateRecentTime.put("timestampLastUpdated", System.currentTimeMillis());
                    mActiveLists.child(listId).updateChildren(updateRecentTime);
                }else{
                    Toast.makeText(AddExpense.this,"This email is not registered with us. Make sure the " +
                            "email you entered is correct and try again.",Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void generateBill() {

        mTransactions.child(listId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String log="";
                float taken=0, given=0;
                for(DataSnapshot childSnapshot: dataSnapshot.getChildren()){
                    for(DataSnapshot grandChildSnapshot: childSnapshot.getChildren()){
                        TransactionStructure ts = grandChildSnapshot.getValue(TransactionStructure.class);
                        if(LoginActivity.convertEmail(OWNER_EMAIL).equals(childSnapshot.getKey())){
                            Log.e(LOG_TAG, "₹ " + ts.getAmount() + " from " + ts.getFrom());
                            System.out.println("₹ " + ts.getAmount() + " from " + ts.getFrom());
                            log += "₹ " + ts.getAmount() + " from " + ts.getFrom() + "\n";
                            taken += ts.getAmount();
                        }
                        else if(ts.getFrom().equals(OWNER_EMAIL)){
                            Log.e(LOG_TAG, "₹ " + ts.getAmount() + " to " + ts.getFrom());
                            System.out.println("₹ " + ts.getAmount() + " to " + ts.getFrom());
                            log += "₹ " + ts.getAmount() + " to " + childSnapshot.getKey() + "\n";
                            given += ts.getAmount();
                        }
                    }
                }
                String total = "Money to be taken ₹ " + taken +"\nMoney to be given ₹ " + given;
                if(!TextUtils.isEmpty(log)){
                    if(mAddMemberBuilder !=null) mAddMemberBuilder.dismiss();
                    if(mGenerateBillDialog !=null) mGenerateBillDialog.dismiss();

                    final Dialog mGenerateBillDialog = new Dialog((AddExpense.this));
                    mGenerateBillDialog.setContentView(R.layout.layout_bill);
                    mGenerateBillDialog.setTitle(listName+" Bill");

                    TextView tvED = (TextView) mGenerateBillDialog.findViewById(R.id.tvExpDetails);
                    tvED.setText(log);
                    TextView tvFE = (TextView) mGenerateBillDialog.findViewById(R.id.tvFinalExp);
                    tvFE.setText(total);

                    Button btn = (Button) mGenerateBillDialog.findViewById(R.id.btnDone);
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mGenerateBillDialog.dismiss();
                        }
                    });

                    mGenerateBillDialog.show();
                }
                else Toast.makeText(AddExpense.this, "Nothing to show. Add some expense.", Toast.LENGTH_SHORT).show();


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        //TODO: Generate Bill
    }

    private boolean isValid(String email) {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.action_addMemberButton:
                addNewMem();
                break;
            case R.id.action_generateBill:
                Toast.makeText(AddExpense.this, "Generating Bill...", Toast.LENGTH_SHORT).show();
                generateBill();
                break;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mAddMemberBuilder !=null) mAddMemberBuilder.dismiss();
        if(mGenerateBillDialog !=null) mGenerateBillDialog.dismiss();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mExpense.removeEventListener(mRefreshExpenseListener);
        mExpense.removeEventListener(mRefreshMemberListener);
    }
}

