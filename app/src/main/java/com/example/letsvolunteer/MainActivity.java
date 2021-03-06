package com.example.letsvolunteer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.letsvolunteer.notifications.Token;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity implements SetActionBarTitle {



    FirebaseAuth firebaseAuth;
    ActionBar actionBar;
    TextView profileTv;
    MenuItem menuItemNotification;
    TextView textNotificationCount;
    String mUID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        actionBar = getSupportActionBar();
        actionBar.setTitle("Profile");


        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Log.d("TAG", "onNavigationItemSelected: "+ item.getItemId());
                switch(item.getItemId()) {
                    case R.id.page_1 :
                        actionBar.setTitle("Users");
                        // code
                        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_view_tag,
                                new ChatListFragment()).commit();
                        return true;

                    case R.id.page_2:
                        actionBar.setTitle("Events");
                        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_view_tag,
                                new EventListFragment()).commit();
                        return true;

                    case R.id.page_3:
                        actionBar.setTitle("Events on Map");
                        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_view_tag,
                                new EventsShowonMapsFragment()).commit();
                        return true;

                    case R.id.page_4:
                        actionBar.setTitle("Favourites");
                        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_view_tag,
                                new FavouritesPageFragment()).commit();
                        return true;

                    case R.id.page_5:
                        actionBar.setTitle("User");
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        DocumentReference documentReference = db.collection("Volunteer").document(user.getUid());
                        documentReference.addSnapshotListener(MainActivity.this, new EventListener<DocumentSnapshot>() {
                            @Override
                            public void onEvent(@Nullable DocumentSnapshot documentSnapshot,
                                                @Nullable FirebaseFirestoreException error) {

                                if (documentSnapshot != null && documentSnapshot.exists()) {
                                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_view_tag,
                                            new vAccountInfoFragment()).commit();
                                }
                                else {
                                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_view_tag,
                                            new oAccountInfoFragment()).commit();
                                }
                            }
                        });
                   //     getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_view_tag,
                            //    new vAccountInfoFragment()).commit();
                        // code block
                        return true;

                    default:
                        // code block
                        break;
                }


                return true;
            }
        });

//        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_view_tag,
//                new BlankFragment()).commit();
        bottomNavigation.setSelectedItemId(R.id.page_2);

        updateToken(String.valueOf(FirebaseMessaging.getInstance().getToken()));


    }

    public void updateToken(String token) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        mUID = user.getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Tokens");
        Token mToken = new Token(token);
        ref.child(mUID).setValue(mToken);
    }

    public void ChangeActionBarTitle(String title){
        getSupportActionBar().setTitle(title);
    }

    private void checkUserStatus() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            // user is signed in, stay in this page
           // profileTv.setText(user.getEmail());
            mUID = user.getUid();

            SharedPreferences sp =  getSharedPreferences("SP_USER", MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("Current_USERID", mUID);
            editor.apply();

        }
        else {
            // user not signed in, goto main activity
            startActivity(new Intent(MainActivity.this, Authentication.class));
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        Log.d("Overwriting the back button functionaloity", "onBackPressed: ");
        Log.d("-->>>", "onBackPressed: " + getSupportFragmentManager());
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            // no code
        }
    }

    //
//    @Override
//    protected void onStart() {
//        checkUserStatus();
//        super.onStart();
//    }
//
//    /

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // inflate menu
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menuItemNotification = menu.findItem(R.id.page_6);
        View actionView = menuItemNotification.getActionView();
        textNotificationCount = (TextView) actionView.findViewById(R.id.cart_badge);

        actionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOptionsItemSelected(menuItemNotification);
            }
        });

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = firebaseAuth.getCurrentUser();

        db.collection("Volunteer").document(user.getUid()).get().addOnSuccessListener(documentSnapshot -> {

        int manageNotifications = 0;
            final int[] notificationsList = {0};
            if (documentSnapshot.getData() != null && documentSnapshot.getData().get("MyInterestCategories") != null) {
                if (documentSnapshot.getData().get("MyManageNotifications") != null) {
                    manageNotifications = ((ArrayList) documentSnapshot.getData().get("MyManageNotifications")).size();
                }
                ArrayList<String> arrayListCate = (ArrayList<String>) documentSnapshot.getData().get("MyInterestCategories");
                Query dbref =  db.collection("Notification").whereIn("eventCategory", arrayListCate);
                int finalManageNotifications = manageNotifications;
                dbref.get().addOnSuccessListener(queryDocumentSnapshots -> {
                    notificationsList[0] = queryDocumentSnapshots.getDocuments().size();
                    textNotificationCount.setText("" + (notificationsList[0] - finalManageNotifications));
                });



            }
        });



        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // get the id
        int id = item.getItemId();
        if(id == R.id.action_logout) {
            firebaseAuth.signOut();
            checkUserStatus();
        }
        if (id == R.id.page_6){
            actionBar.setTitle("Events");
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_view_tag,
                    new NotificationsFragment()).commit();
        }


        return super.onOptionsItemSelected(item);
    }
}