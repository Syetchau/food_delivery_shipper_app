package com.example.liew.ideliveryshipper;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.liew.ideliveryshipper.Common.Common;
import com.example.liew.ideliveryshipper.Common.DirectionJSONParser;
import com.example.liew.ideliveryshipper.Model.Request;
import com.example.liew.ideliveryshipper.Remote.IGeoCoordinates;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.FirebaseDatabase;
import com.jakewharton.threetenabp.AndroidThreeTen;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dmax.dialog.SpotsDialog;
import info.hoang8f.widget.FButton;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class TrackingOrder extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    FusedLocationProviderClient fusedLocationProviderClient;
    LocationCallback locationCallback;
    LocationRequest locationRequest;
    Location mLastLocation;
    Request currentOrder;

    Marker mCurrentMarker;
    IGeoCoordinates mService;
    Polyline polyline;

    FButton btn_call, btn_shipped;
    TextView distance,duration,time;


    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //add calligraphy
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/restaurant_font.otf")
                .setFontAttrId(R.attr.fontPath)
                .build());

        setContentView(R.layout.activity_tracking_order);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        btn_call = (FButton) findViewById(R.id.btnCall);
        btn_shipped = (FButton) findViewById(R.id.btnShipped);
        distance = (TextView)findViewById(R.id.display_distance);
        duration = (TextView)findViewById(R.id.display_duration);
        time = (TextView)findViewById(R.id.display_expected_hour);


        btn_call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:" + Common.currentRequest.getPhone()));
                if (ActivityCompat.checkSelfPermission(TrackingOrder.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                startActivity(intent);
            }
        });

        btn_shipped.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ConfirmDialog();
                
            }
        });


        mService = Common.getGeoCodeService();

        buildLocationRequest();
        buildLocationCallBack();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
    }

    private void ConfirmDialog() {

        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(TrackingOrder.this, R.style.Theme_AppCompat_DayNight_Dialog_Alert);
        alertDialog.setTitle("Confirm Shipped?");

        LayoutInflater inflater = this.getLayoutInflater();
        View confirm_delete_layout = inflater.inflate(R.layout.confirm_layout,null);
        alertDialog.setView(confirm_delete_layout);
        alertDialog.setIcon(R.drawable.ic_local_shipping_black_24dp);

        alertDialog.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //we will delete order in the table
                //OrderNeedShip or ShippingOrder
                //And udate status of order to Shipped
                FirebaseDatabase.getInstance()
                        .getReference(Common.ORDER_NEED_SHIP_TABLE)
                        .child(Common.currentShipper.getPhone())
                        .child(Common.currentKey)
                        .removeValue()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                //Update status on Request table
                                Map<String, Object> update_status = new HashMap<>();
                                update_status.put("status", "03");

                                FirebaseDatabase.getInstance()
                                        .getReference("Requests")
                                        .child(Common.currentKey)
                                        .updateChildren(update_status)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                //Delete from shippingOrder
                                                FirebaseDatabase.getInstance()
                                                        .getReference(Common.SHIPPER_INFO_TABLE)
                                                        .child(Common.currentKey)
                                                        .removeValue()
                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                Toast.makeText(TrackingOrder.this, "Delivered!", Toast.LENGTH_SHORT).show();
                                                                finish();
                                                            }
                                                        });
                                            }
                                        });
                            }
                        });
            }
        });
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        alertDialog.show();
    }


    private void buildLocationCallBack() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                mLastLocation = locationResult.getLastLocation();
                if (mCurrentMarker != null)
                    mCurrentMarker.setPosition(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude())); //update location for marker

                //update location on firebase
                Common.updateShippingInformation(Common.currentKey, mLastLocation);
                mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(mLastLocation.getLatitude(),
                        mLastLocation.getLongitude())));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(16.0f));

                drawRoute(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), Common.currentRequest);

            }
        };
    }

    private void drawRoute(final LatLng yourLocation, final Request request) {

        //clear all polyline
        if (polyline != null)
            polyline.remove();

        if (request.getAddress() != null && !request.getAddress().isEmpty()) {
            mService.getGeoCode(request.getAddress()).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                        try {
                        JSONObject jsonObject = new JSONObject(response.body().toString());

                        String lat = ((JSONArray) jsonObject.get("results"))
                                .getJSONObject(0)
                                .getJSONObject("geometry")
                                .getJSONObject("location")
                                .get("lat").toString();

                         String lng = ((JSONArray) jsonObject.get("results"))
                                .getJSONObject(0)
                                .getJSONObject("geometry")
                                .getJSONObject("location")
                                .get("lng").toString();

                        final LatLng orderLocation = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));

                        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.deliverybox);
                        bitmap = Common.scaleBitmap(bitmap, 70, 70);

                        MarkerOptions marker = new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                                .title("Order of " + Common.currentRequest.getPhone())
                                .position(orderLocation);

                        mMap.addMarker(marker);

                        //draw route

                        mService.getDirections(yourLocation.latitude + "," + yourLocation.longitude,
                                orderLocation.latitude + "," + orderLocation.longitude)
                                .enqueue(new Callback<String>() {
                                    @Override
                                    public void onResponse(Call<String> call, Response<String> response) {

                                        new ParserTask().execute(response.body().toString());

                                    }

                                    @Override
                                    public void onFailure(Call<String> call, Throwable t) {

                                    }
                                });

                    } catch (JSONException e) {

                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {

                }
            });
        } else {
            if (request.getLatLng() != null && !request.getLatLng().isEmpty()) {
                String[] latLng = request.getLatLng().split(",");
                LatLng orderLocation = new LatLng(Double.parseDouble(latLng[0]), Double.parseDouble(latLng[1]));
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.deliverybox);
                bitmap = Common.scaleBitmap(bitmap, 70, 70);

                MarkerOptions marker = new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                        .title("Order of " + Common.currentRequest.getPhone())
                        .position(orderLocation);

                mMap.addMarker(marker);
                mService.getDirections(mLastLocation.getLatitude() + "," + mLastLocation.getLongitude(),
                        orderLocation.latitude + "," + orderLocation.longitude)
                        .enqueue(new Callback<String>() {
                            @Override
                            public void onResponse(Call<String> call, Response<String> response) {
                                new ParserTask().execute(response.body().toString());

                            }

                            @Override
                            public void onFailure(Call<String> call, Throwable t) {

                            }
                        });
            }
        }
    }


    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setSmallestDisplacement(10f);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
    }

    @Override
    protected void onStop() {
        if (fusedLocationProviderClient != null)
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        super.onStop();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker and move the camera
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                mLastLocation = location;
                final LatLng yourLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mCurrentMarker = mMap.addMarker(new MarkerOptions().position(yourLocation).title("Your location"));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(yourLocation));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(16.0f));
                mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        showGoogleMapDirection(new LatLng(mLastLocation.getLatitude(),
                                mLastLocation.getLongitude()), Common.currentRequest);
                        return false;
                    }
                });
            }
        });
    }

    private void showGoogleMapDirection(final LatLng yourLocation, final Request request) {
        if (request.getAddress() != null && !request.getAddress().isEmpty()) {
            mService.getGeoCode(request.getAddress()).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().toString());

                        String lat = ((JSONArray) jsonObject.get("results"))
                                .getJSONObject(0)
                                .getJSONObject("geometry")
                                .getJSONObject("location")
                                .get("lat").toString();

                        String lng = ((JSONArray) jsonObject.get("results"))
                                .getJSONObject(0)
                                .getJSONObject("geometry")
                                .getJSONObject("location")
                                .get("lng").toString();

                        final LatLng orderLocation = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));

                        String uri = "http://maps.google.com/maps?saddr=" + yourLocation.latitude + "," + yourLocation.longitude + "&daddr=" + orderLocation.latitude + "," + orderLocation.longitude;
                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                        mapIntent.setPackage("com.google.android.apps.maps");
                        startActivity(mapIntent);


                    } catch (JSONException e) {

                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {

                }
            });
        } else {
            if (request.getLatLng() != null && !request.getLatLng().isEmpty()) {
                String[] latLng = request.getLatLng().split(",");
                LatLng orderLocation = new LatLng(Double.parseDouble(latLng[0]), Double.parseDouble(latLng[1]));

                String uri = "http://maps.google.com/maps?saddr=" + yourLocation.latitude + "," + yourLocation.longitude + "&daddr=" + orderLocation.latitude + "," + orderLocation.longitude;
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);

            }
        }
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        android.app.AlertDialog mDialog = new SpotsDialog( TrackingOrder.this);

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mDialog.show();
        mDialog.setMessage("Please waiting...");

    }

    @Override
    protected List<List<HashMap<String, String>>> doInBackground(String... strings) {

        JSONObject jsonObject;

        List<List<HashMap<String, String>>> routes = null;

        try {
            jsonObject = new JSONObject(strings[0]);

            DirectionJSONParser parser = new DirectionJSONParser();
            routes = parser.parse(jsonObject);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return routes;
    }

    @Override
    protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
        mDialog.dismiss();
        distance.setText(Common.DISTANCE);
        duration.setText(Common.DURATION);
        time.setText(Common.ESTIMATED_TIME);

        ArrayList points = null;
        PolylineOptions lineOptions = null;

        for (int i = 0; i < lists.size(); i++) {

            points = new ArrayList();
            lineOptions = new PolylineOptions();

            List<HashMap<String, String>> path = lists.get(i);

            for (int j = 0; j < path.size(); j++) {

                HashMap<String, String> point = path.get(j);

                double lat = Double.parseDouble(point.get("lat"));
                double lng = Double.parseDouble(point.get("lng"));

                LatLng position = new LatLng(lat, lng);

                points.add(position);
            }

            lineOptions.addAll(points);
            lineOptions.width(8);
            lineOptions.color(Color.RED);
            lineOptions.geodesic(true);
        }

        mMap.addPolyline(lineOptions);
    }
}
}