package com.example.ispeed.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.ispeed.FunctionMethod.FunctionMethod;
import com.example.ispeed.Model.InternetDataModel;
import com.example.ispeed.R;
import com.example.ispeed.service.AutoMaticTrackingActivity;
import com.example.ispeed.service.MyService;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardMainActivity extends AppCompatActivity {
    BarChart bc_downloadSpeed,bc_ping,bc_uploadSpeed;
    ArrayList<InternetDataModel> mDataModel;
    FirebaseFirestore db;
    FirebaseAuth mFirebaseAuth;
    FirebaseUser firebaseUser;
    TextView tv_ds_label,tv_us_label,tv_ping_label;
    CardView cv_uploadSpeed,cv_downloadSpeed,cv_ping;
    private static final String TAG = "DashboardMainActivity";
    Button btn_settings,btn_export;
    String filterDay = "";

    String filterStartingHour = "";
    String filterEndingHour = "";

    Button btn_startTracking,btn_stopTracking;
    String currentLocation;
    FunctionMethod functionMethod;
    GoogleApiClient googleApiClient;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int REQUEST_LOCATION = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_main);

        mFirebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = mFirebaseAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        bc_ping = findViewById(R.id.bc_ping);
        bc_uploadSpeed = findViewById(R.id.bc_uploadSpeed);
        bc_downloadSpeed = findViewById(R.id.bc_downloadSpeed);
        tv_ds_label = findViewById(R.id.tv_ds_label);
        tv_ping_label = findViewById(R.id.tv_ping_label);
        tv_us_label = findViewById(R.id.tv_us_label);
        btn_settings = findViewById(R.id.btn_settings);
        btn_export = findViewById(R.id.btn_export);

        cv_downloadSpeed = findViewById(R.id.cv_downloadSpeed);
        cv_ping = findViewById(R.id.cv_ping);
        cv_uploadSpeed = findViewById(R.id.cv_uploadSpeed);

        btn_startTracking = findViewById(R.id.btn_startTracking);
        btn_stopTracking = findViewById(R.id.btn_stopTracking);

        functionMethod = new FunctionMethod();
        if(googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) { }
                    @Override
                    public void onConnectionSuspended(int i) {
                        googleApiClient.connect();
                    }
                })
                .addOnConnectionFailedListener(connectionResult -> { }).build();
            googleApiClient.connect();
        }
        db = FirebaseFirestore.getInstance();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation()
        .addOnSuccessListener(this, location -> {
            // Got last known location. In some rare situations this can be null.
            if (location != null) {
                Log.d(TAG, "onSuccess: " + location.getLatitude());
                try {
                    if (ActivityCompat.checkSelfPermission(
                            getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions( (Activity) getApplicationContext() , new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
                    } else {
                        double lat = location.getLatitude();
                        double longi = location.getLongitude();

                        Geocoder geo = new Geocoder(getBaseContext(), Locale.getDefault());
                        List<Address> addresses = geo.getFromLocation(lat, longi, 1);

                        currentLocation = addresses.get(0).getLocality();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        btn_startTracking.setOnClickListener(view -> {
            Intent startServiceIntent = new Intent(this, MyService.class);
            startServiceIntent.putExtra("loc",currentLocation);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(startServiceIntent);
            } else {
                startService(startServiceIntent);
            }
        });

        btn_stopTracking.setOnClickListener(view -> {
            Intent stopServiceIntent = new Intent(this, MyService.class);
            stopServiceIntent.putExtra("loc",currentLocation);
            stopService(stopServiceIntent);
        });

        btn_settings.setOnClickListener(view -> startActivity(new Intent(getApplicationContext(), SettingsActivity.class)));

        btn_export.setOnClickListener(view -> {
            Intent intent = new Intent(getBaseContext(), ExportActivity.class);
            intent.putExtra("date",filterDay);
            intent.putExtra("start",filterStartingHour);
            intent.putExtra("end",filterEndingHour);
            startActivity(intent);
        });

        bc_downloadSpeed.setOnClickListener(view -> {
            Intent intent = new Intent(getBaseContext(), ViewDashboardContentActivity.class);
            intent.putExtra("type", "download");
            intent.putExtra("date",filterDay);
            intent.putExtra("start",filterStartingHour);
            intent.putExtra("end",filterEndingHour);
            startActivity(intent);
        });


        bc_uploadSpeed.setOnClickListener(view -> {
            Intent intent = new Intent(getBaseContext(), ViewDashboardContentActivity.class);
            intent.putExtra("type", "upload");
            intent.putExtra("date",filterDay);
            intent.putExtra("start",filterStartingHour);
            intent.putExtra("end",filterEndingHour);
            startActivity(intent);

        });

        bc_ping.setOnClickListener(view -> {
            Intent intent = new Intent(getBaseContext(), ViewDashboardContentActivity.class);
            intent.putExtra("type", "ping");
            intent.putExtra("date",filterDay);
            intent.putExtra("start",filterStartingHour);
            intent.putExtra("end",filterEndingHour);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if ((getIntent().getStringExtra("date") !=null &&
                getIntent().getStringExtra("start") !=null &&
                getIntent().getStringExtra("end") !=null)
        ){
            filterDay = getIntent().getStringExtra("date");
            filterStartingHour = getIntent().getStringExtra("start");
            filterEndingHour = getIntent().getStringExtra("end");

            Log.d(TAG, "filterDay: " + filterDay);

            getDataWithFilters();

        } else {
            getData();
        }
    }

    public void getData(){
        db.collection(getString(R.string.COLLECTION_INTERNET_SPEED_DATA))
                .whereEqualTo("user_id",firebaseUser.getUid())
                .orderBy("time", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable  QuerySnapshot value, FirebaseFirestoreException error) {
                        mDataModel = new ArrayList<>();

                        if (error != null){
                            Log.d(TAG, "onEvent: " + error.getMessage());
                        }else{
                            for (DocumentSnapshot document : value.getDocuments())
                            {
                                InternetDataModel dataModel = document.toObject(InternetDataModel.class);
                                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");

                                if (filterDay.equals("")){
                                    filterDay =  formatter.format(new Date());

                                }

                                if (filterDay.equals(formatter.format(dataModel.getTime()))){
                                    mDataModel.add(dataModel);
                                }


                            }

                            ArrayList<BarEntry> barEntries = new ArrayList<>();
                            ArrayList<BarEntry> barEntriesUS = new ArrayList<>();
                            ArrayList<BarEntry> barEntriesPing = new ArrayList<>();

                            ArrayList<String> theTimes = new ArrayList<>();
                            barEntriesUS.add(new BarEntry(0,0));
                            barEntries.add(new BarEntry(0,0));
                            barEntriesPing.add(new BarEntry(0,0));
                            theTimes.add("");

                            for (int i = 0; i < mDataModel.size(); i++){
                                Float val = Float.valueOf(mDataModel.get(i).getDownLoadSpeed());
                                Float valUS = Float.valueOf(mDataModel.get(i).getUploadSpeed());
                                Float valPing = Float.valueOf(mDataModel.get(i).getPing());

                                barEntries.add(new BarEntry(val,1+i));
                                barEntriesUS.add(new BarEntry(valUS,1+i));
                                barEntriesPing.add(new BarEntry(valPing,1+i));
                                Date date = mDataModel.get(i).getTime();
                                @SuppressLint("SimpleDateFormat")
                                SimpleDateFormat formatter = new SimpleDateFormat("HH:mm aa");

                                Log.d(TAG, "timeOfData: " + formatter.format(date));
                                theTimes.add(formatter.format(date));

                            }

                            Date date = new Date();
                            @SuppressLint("SimpleDateFormat")
                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMMM dd, yyyy");
                            tv_ds_label.setText("Download Speed (" + simpleDateFormat.format(date) + ")" );
                            tv_ping_label.setText("Ping (" + simpleDateFormat.format(date) + ")" );
                            tv_us_label.setText("Upload Speed (" + simpleDateFormat.format(date) + ")" );


                            BarDataSet  dataSet = new BarDataSet(barEntries, "Mbps");

                            Log.d(TAG, "onEvent: " + theTimes.size());
                            BarData data = new BarData(theTimes,dataSet );
                            bc_downloadSpeed.setData(data);
                            bc_downloadSpeed.setTouchEnabled(true);
                            bc_downloadSpeed.setDragEnabled(true);
                            bc_downloadSpeed.setScaleEnabled(true);
                            // set the data and list of labels into chart
                            bc_downloadSpeed.setDescription("");  // set the description
                            bc_downloadSpeed.animateY(2000);


                            BarDataSet  dataSetUS = new BarDataSet(barEntriesUS, "Mbps");
                            BarData dataUS = new BarData(theTimes,dataSetUS );
                            bc_uploadSpeed.setData(dataUS);
                            bc_uploadSpeed.setTouchEnabled(true);
                            bc_uploadSpeed.setDragEnabled(true);
                            bc_uploadSpeed.setScaleEnabled(true);
                            // set the data and list of labels into chart
                            bc_uploadSpeed.setDescription("");  // set the description
                            bc_uploadSpeed.animateY(2000);



                            BarDataSet  dataSetPing = new BarDataSet(barEntriesPing, "Ms");
                            BarData dataPing= new BarData(theTimes,dataSetPing );
                            bc_ping.setData(dataPing);
                            bc_ping.setTouchEnabled(true);
                            bc_ping.setDragEnabled(true);
                            bc_ping.setScaleEnabled(true);
                            // set the data and list of labels into chart
                            bc_ping.setDescription("");  // set the description
                            bc_ping.animateY(2000);

                        }



                    }
                });
    }

    public void getDataWithFilters(){
        Timestamp timestamp;
        Date dateStart, dateEnd;
        String  parsers = filterDay + " " + filterStartingHour + ":00";
        String  parserEnd = filterDay + " " + filterEndingHour + ":00";
        try {
            dateStart = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse(parsers);
            dateEnd = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse(parserEnd);
            db.collection(getString(R.string.COLLECTION_INTERNET_SPEED_DATA))
                    .whereEqualTo("user_id",firebaseUser.getUid())
                    .whereGreaterThanOrEqualTo("time",dateStart)
                    .whereLessThanOrEqualTo("time",dateEnd)
                    .orderBy("time", Query.Direction.ASCENDING)
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable  QuerySnapshot value, FirebaseFirestoreException error) {
                            mDataModel = new ArrayList<>();

                            if (error != null){
                                Log.d(TAG, "onEvent: " + error.getMessage());
                            }else{
                                for (DocumentSnapshot document : value.getDocuments())
                                {
                                    InternetDataModel dataModel = document.toObject(InternetDataModel.class);
                                    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");

                                    if (filterDay.equals("")){
                                        filterDay =  formatter.format(new Date());

                                    }


                                    Log.d(TAG, "onEvent: " + filterDay );
                                    Log.d(TAG, "onEvent: " + formatter.format(dataModel.getTime()));
                                    mDataModel.add(dataModel);

                                }

                                ArrayList<BarEntry> barEntries = new ArrayList<>();
                                ArrayList<BarEntry> barEntriesUS = new ArrayList<>();
                                ArrayList<BarEntry> barEntriesPing = new ArrayList<>();

                                ArrayList<String> theTimes = new ArrayList<>();
                                barEntriesUS.add(new BarEntry(0,0));
                                barEntries.add(new BarEntry(0,0));
                                barEntriesPing.add(new BarEntry(0,0));
                                theTimes.add("");

                                for (int i = 0; i < mDataModel.size(); i++){
                                    Float val = Float.valueOf(mDataModel.get(i).getDownLoadSpeed());
                                    Float valUS = Float.valueOf(mDataModel.get(i).getUploadSpeed());
                                    Float valPing = Float.valueOf(mDataModel.get(i).getPing());

                                    barEntries.add(new BarEntry(val,1+i));
                                    barEntriesUS.add(new BarEntry(valUS,1+i));
                                    barEntriesPing.add(new BarEntry(valPing,1+i));
                                    Date date = mDataModel.get(i).getTime();
                                    @SuppressLint("SimpleDateFormat")
                                    SimpleDateFormat formatter = new SimpleDateFormat("HH:mm aa");

                                    Log.d(TAG, "timeOfData: " + formatter.format(date));
                                    theTimes.add(formatter.format(date));

                                }

                                Date date = mDataModel.get(0).getTime();
                                @SuppressLint("SimpleDateFormat")
                                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMMM dd, yyyy");
                                tv_ds_label.setText("Download Speed (" + simpleDateFormat.format(date) + ")" );
                                tv_ping_label.setText("Ping (" + simpleDateFormat.format(date) + ")" );
                                tv_us_label.setText("Upload Speed (" + simpleDateFormat.format(date) + ")" );


                                BarDataSet  dataSet = new BarDataSet(barEntries, "Mbps");
                                BarData data = new BarData(theTimes,dataSet );
                                bc_downloadSpeed.setData(data);
                                bc_downloadSpeed.setTouchEnabled(true);
                                bc_downloadSpeed.setDragEnabled(true);
                                bc_downloadSpeed.setScaleEnabled(true);
                                // set the data and list of labels into chart
                                bc_downloadSpeed.setDescription("");  // set the description
                                bc_downloadSpeed.animateY(2000);


                                BarDataSet  dataSetUS = new BarDataSet(barEntriesUS, "Mbps");
                                BarData dataUS = new BarData(theTimes,dataSetUS );
                                bc_uploadSpeed.setData(dataUS);
                                bc_uploadSpeed.setTouchEnabled(true);
                                bc_uploadSpeed.setDragEnabled(true);
                                bc_uploadSpeed.setScaleEnabled(true);
                                // set the data and list of labels into chart
                                bc_uploadSpeed.setDescription("");  // set the description
                                bc_uploadSpeed.animateY(2000);



                                BarDataSet  dataSetPing = new BarDataSet(barEntriesPing, "Ms");
                                BarData dataPing= new BarData(theTimes,dataSetPing );
                                bc_ping.setData(dataPing);
                                bc_ping.setTouchEnabled(true);
                                bc_ping.setDragEnabled(true);
                                bc_ping.setScaleEnabled(true);
                                // set the data and list of labels into chart
                                bc_ping.setDescription("");  // set the description
                                bc_ping.animateY(2000);

                            }



                        }
                    });



        } catch (ParseException e) {
            e.printStackTrace();
        }


//        Date date = null;
//        try {
//            date = new Date(String.valueOf(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(parsers)));
//            Timestamp timestamp = new Timestamp(date);
//            Log.d(TAG, "getData: " + timestamp);
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }


    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();

        startActivity( new Intent(getBaseContext(),HomeActivity.class));
    }
}