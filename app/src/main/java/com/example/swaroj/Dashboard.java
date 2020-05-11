package com.example.swaroj;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Dashboard extends AppCompatActivity {

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;
    String USER_ID, user_type;
    TextView name, phone, type;
    TextView coordinates_textview;
    int count = 0;
    double lat, lon;

    Button near_me;
    double sin_lat, sin_lon;
    ArrayList<Double> latitudes = new ArrayList<>();
    ArrayList<Double> longitudes = new ArrayList<>();

    double user_latitude = 0;
    double user_longitude = 0;
    double latitude_distance = 0;
    double longitude_distance = 0;
    final int radius = 6371;

    double a = 0;
    double c = 0;
    double dist = 0;
    ArrayList<Double> migrants_near = new ArrayList<>();


    private static final int PERMISSION_ID = 44;
    FusedLocationProviderClient mFusedLocationClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);



        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        name = findViewById(R.id.textView_name_dash);
        phone = findViewById(R.id.textView_phone_dash);
        type = findViewById(R.id.textView_type_dash);
        coordinates_textview = findViewById(R.id.textView_coordinate_dash);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(Dashboard.this);
        getLastLocation();
        
        near_me = findViewById(R.id.imageButton1);
        near_me.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                getLastLocation();
               // coordinates_textview.setText("\n My Location \n" );


                Intent intent = new Intent(getApplicationContext(),Near_Me.class);
                intent.putExtra("dash_lat", lat);
                intent.putExtra("dash_lon", lon);
               // intent.putExtra("dash_migrants_near", migrants_near);
                intent.putExtra("dash_count", migrants_near.size());
                startActivity(intent);
               // startActivity(new Intent(getApplicationContext(), Near_Me.class));
            }
        });

        USER_ID = firebaseAuth.getCurrentUser().getUid();
        final DocumentReference documentReference = firestore.collection("USERS").document(USER_ID);
        final DocumentReference documentReference2 = firestore.collection("MIGRANTS").document(USER_ID);


        documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if(documentSnapshot.exists())
                {
                    name.setText(documentSnapshot.getString("NAME_"));
                    phone.setText(documentSnapshot.getString("EMAIL_"));
                    type.setText(documentSnapshot.getString("TYPE_"));
                    user_type = (documentSnapshot.getString("TYPE_"));
                }
                else {
                    Toast.makeText(getApplicationContext(), "Profile data not found", Toast.LENGTH_SHORT);
                    startActivity(new Intent(getApplicationContext(), Home.class));
                    finish();
                }
            }
        });
        isVulnerable();
    }

    private void isVulnerable() {

        String[] questions_array = new String[]{
                "Cough, Fever, Difficulty in breathing",
                "Travelled anywhere recently internationally in the last 14 days",
                "Interacted with someone tested COVID-19 positive",
                "are a healthcare worker"
        };
        final List<String> questions = Arrays.asList(questions_array);
        final boolean checked[] = new boolean[]{false, false, false, false, false};
        final List<Integer> itemsSelected = new ArrayList<>();

        final AlertDialog.Builder builder = new AlertDialog.Builder(Dashboard.this);
        builder.setMultiChoiceItems(questions_array, checked, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                checked[which] = isChecked;
                String current_item = questions.get(which);
                count++;

            }
        });
        builder.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (count < 2) {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(Dashboard.this);
                    View builder1_view = getLayoutInflater().inflate(R.layout.custom_dialog2, null);
                    builder1.setView(builder1_view);
                    builder1.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
                    builder1.create().show();
                    Toast.makeText(getApplicationContext(), "Your infection risk is low. Stay at home", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    AlertDialog.Builder builder2 = new AlertDialog.Builder(Dashboard.this);
                    View builder2_view = getLayoutInflater().inflate(R.layout.custom_dialog1, null);
                    builder2.setView(builder2_view);
                    builder2.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //add to vulnerable list
                        }
                    });
                    builder2.create().show();
                    Toast.makeText(getApplicationContext(), "TEST IMMEDIATELY", Toast.LENGTH_SHORT).show();

                }

            }
        });
        builder.setTitle("Which of the following is/are true?");
        builder.create().show();
    }

    private void is_near_me() {

        firestore.collection("MIGRANTS").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful())
                {
                    migrants_near.clear();
                    latitudes.clear();
                    longitudes.clear();
                    for(QueryDocumentSnapshot document:task.getResult())
                    {
                        sin_lat = (double) document.get("latitude");
                        sin_lon = (double) document.get("longitude");
                        if(sin_lon==user_latitude && sin_lon==user_longitude)
                        {
                            continue;
                        }
                        else
                        {
                            latitudes.add(sin_lat);
                            longitudes.add(sin_lon);
                        }
                    }

                    is_near_me();
                }


            }
        });
        int i;
        for (i = 0; i<latitudes.size(); i++)
        {
            //System.out.println("For" + latitudes.get(i) + ", " + longitudes.get(i) + "\n");
            latitude_distance = (Math.toRadians(user_latitude - latitudes.get(i)));
            System.out.println(latitude_distance);
            longitude_distance = Math.toRadians(user_longitude - longitudes.get(i));
            System.out.println(longitude_distance);

            a = Math.abs(Math.pow(Math.sin(latitude_distance / 2), 2)
                    + Math.cos(user_latitude) * Math.cos(latitudes.get(i))
                    * Math.pow(Math.sin(longitude_distance / 2),2));
            System.out.println("a = " + a + "\n");

            c = 2 * Math.asin(Math.sqrt(a));
            System.out.println("c = " + c + "\n");
            //height = e2 - e1;
            dist = c * radius;
            dist = dist* 1000;//converts to meters

            System.out.println("dist = "+ dist + "\n");
            if (dist > 500) {
                //do nothing

            } else if (dist < 500) {
                if(dist!=0)
                    migrants_near.add(dist);

            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.custom_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.logout_menu)
        {
            firebaseAuth.getInstance().signOut();
            finish();
            startActivity(new Intent(getApplicationContext(), Home.class));
            Toast.makeText(getApplicationContext(), "Successfully signed out", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @SuppressLint("MissingPermission")
    private void getLastLocation(){
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.getLastLocation().addOnCompleteListener(
                        new OnCompleteListener<Location>() {
                            @Override
                            public void onComplete(@NonNull Task<Location> task) {
                                Location location = task.getResult();
                                if (location == null) {
                                    requestNewLocationData();
                                } else {
                                    lat = location.getLatitude();
                                    lon = location.getLongitude();
                                    coordinates_textview.setText("\n My Location \n" + location.getLatitude()+", "+location.getLongitude()+"");

                                }
                            }
                        }
                );
            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            requestPermissions();
        }
    }


    @SuppressLint("MissingPermission")
    private void requestNewLocationData(){

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(0);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setNumUpdates(1);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.requestLocationUpdates(
                mLocationRequest, mLocationCallback,
                Looper.myLooper()
        );

    }

    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
            lat = mLastLocation.getLatitude();
            lon = mLastLocation.getLongitude();
            coordinates_textview.setText(mLastLocation.getLatitude()+", "+mLastLocation.getLongitude()+"");
        }
    };

    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_ID
        );
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            }
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        if (checkPermissions()) {
            getLastLocation();
        }

    }


}
