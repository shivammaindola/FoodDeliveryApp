package com.fooddeliv;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.fooddeliv.Common.Common;
import com.fooddeliv.Helper.DirectionJSONParser;
import com.fooddeliv.Model.Request;
import com.fooddeliv.Model.ShippingInformation;
import com.fooddeliv.Retrofit.IGoogleService;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import dmax.dialog.SpotsDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrackingOrder extends FragmentActivity implements OnMapReadyCallback,ValueEventListener {

    private GoogleMap mMap;

    FirebaseDatabase database;
    DatabaseReference requests, shippingOrder;

    IGoogleService mService;

    Request currentOrder;

    Marker shippingMarker;
    Polyline polyline;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking_order);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        database=FirebaseDatabase.getInstance();
        requests=database.getReference("Requests");
        shippingOrder=database.getReference("ShippingOrders");

        mService=Common.getGoogleMapApi();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        
        trackingLocation();
        }

    private void trackingLocation() {
        requests.child(Common.currentKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        currentOrder=dataSnapshot.getValue(Request.class);
                        //if order has address
                        if(currentOrder.getAddress()!=null&&!currentOrder.getAddress().isEmpty())
                        {
                            mService.getLocationFromAddress(new StringBuilder("https://maps.googleapis.com/maps/api/geocode/json?address=")
                            .append(currentOrder.getAddress()).toString())
                                    .enqueue(new Callback<String>() {
                                        @Override
                                        public void onResponse(Call<String> call, Response<String> response) {
                                        try{
                                            JSONObject jsonObject= new JSONObject(response.body());

                                            String lat= ((JSONArray)jsonObject.get("results"))
                                                    .getJSONObject(0)
                                                    .getJSONObject("geometry")
                                                    .getJSONObject("location")
                                                    .get("lat").toString();

                                            String lng= ((JSONArray)jsonObject.get("results"))
                                                    .getJSONObject(0)
                                                    .getJSONObject("geometry")
                                                    .getJSONObject("location")
                                                    .get("lng").toString();

                                            LatLng location= new LatLng(Double.parseDouble(lat),
                                                    Double.parseDouble(lng));

                                            mMap.addMarker(new MarkerOptions().position(location)
                                            .title("Order destination")
                                            .icon(BitmapDescriptorFactory.defaultMarker()));

                                            //set Shipper location
                                            shippingOrder.child(Common.currentKey)
                                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                            ShippingInformation shippingInformation = dataSnapshot.getValue(ShippingInformation.class);

                                                            LatLng shipperLocation = new LatLng(shippingInformation.getLat(), shippingInformation.getLng());
                                                            if (shippingMarker == null) {
                                                                shippingMarker = mMap.addMarker(
                                                                        new MarkerOptions()
                                                                                .position(shipperLocation)
                                                                                .title("Shipper #" + shippingInformation.getOrderId())
                                                                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
                                                            } else {
                                                                shippingMarker.setPosition(shipperLocation);

                                                            }

                                                            //update camera

                                                            CameraPosition cameraPosition= new CameraPosition.Builder()
                                                                    .target(shipperLocation)
                                                                    .zoom(16)
                                                                    .bearing(0)
                                                                    .tilt(45)
                                                                    .build();
                                                            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                                                            //draw routes
                                                            if(polyline!=null)
                                                                polyline.remove();

                                                                    mService.getDirections(shipperLocation.latitude+","+shipperLocation.longitude,
                                                                            currentOrder.getAddress())
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



                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError databaseError) {

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
                        }

                        //if order has latlng
                        else if(currentOrder.getLatlng()!=null && !currentOrder.getLatlng().isEmpty())
                        {
                            mService.getLocationFromAddress(new StringBuilder("https://maps.googleapis.com/maps/api/geocode/json?address=")
                                    .append(currentOrder.getLatlng()).toString())
                                    .enqueue(new Callback<String>() {
                                        @Override
                                        public void onResponse(Call<String> call, Response<String> response) {
                                            try{
                                                JSONObject jsonObject= new JSONObject(response.body());

                                                String lat= ((JSONArray)jsonObject.get("results"))
                                                        .getJSONObject(0)
                                                        .getJSONObject("geometry")
                                                        .getJSONObject("location")
                                                        .get("lat").toString();

                                                String lng= ((JSONArray)jsonObject.get("results"))
                                                        .getJSONObject(0)
                                                        .getJSONObject("geometry")
                                                        .getJSONObject("location")
                                                        .get("lng").toString();

                                                LatLng location= new LatLng(Double.parseDouble(lat),
                                                        Double.parseDouble(lng));

                                                mMap.addMarker(new MarkerOptions().position(location)
                                                        .title("Order destination")
                                                        .icon(BitmapDescriptorFactory.defaultMarker()));

                                                //set Shipper location
                                                shippingOrder.child(Common.currentKey)
                                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                ShippingInformation shippingInformation = dataSnapshot.getValue(ShippingInformation.class);

                                                                LatLng shipperLocation = new LatLng(shippingInformation.getLat(), shippingInformation.getLng());
                                                                if (shippingMarker == null) {
                                                                    shippingMarker = mMap.addMarker(
                                                                            new MarkerOptions()
                                                                                    .position(shipperLocation)
                                                                                    .title("Shipper #" + shippingInformation.getOrderId())
                                                                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
                                                                } else {
                                                                    shippingMarker.setPosition(shipperLocation);

                                                                }

                                                                //update camera

                                                                CameraPosition cameraPosition= new CameraPosition.Builder()
                                                                        .target(shipperLocation)
                                                                        .zoom(16)
                                                                        .bearing(0)
                                                                        .tilt(45)
                                                                        .build();
                                                                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                                                                //draw routes
                                                                if(polyline!=null)
                                                                    polyline.remove();

                                                                mService.getDirections(shipperLocation.latitude+","+shipperLocation.longitude,
                                                                        currentOrder.getLatlng())
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



                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError databaseError) {

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

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        trackingLocation();
    }

    @Override
    protected void onStop() {
        shippingOrder.removeEventListener(this);
        super.onStop();
    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {

    }

    private class ParserTask extends AsyncTask<String,Integer,List<List<HashMap<String,String>>>> {
        AlertDialog mDialog= new SpotsDialog.Builder().setContext(TrackingOrder.this).build();

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            mDialog.show();
            mDialog.setMessage("Please wait");
        }
        @Override
        protected List<List<HashMap<String, String >>> doInBackground(String... strings){
            JSONObject jObject;
            List<List<HashMap<String,String>>> routes=null;
            try{
                jObject= new JSONObject(strings[0]);
                DirectionJSONParser parser= new DirectionJSONParser();

                routes= parser.parse(jObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }


            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            mDialog.dismiss();

            ArrayList points=null;
            PolylineOptions lineOptions=null;

            for(int i=0;i<lists.size();i++){
                points= new ArrayList();
                lineOptions= new PolylineOptions();

                List<HashMap<String ,String >> path=lists.get(i);

                for(int j=0;j<path.size();j++){
                    HashMap<String,String> point= path.get(j);

                    double lat= Double.parseDouble(point.get("lat"));
                    double lng= Double.parseDouble(point.get("lng"));
                    LatLng position= new LatLng(lat,lng);

                    points.add(position);

                }
                lineOptions.addAll(points);
                lineOptions.width(12);
                lineOptions.color(Color.BLUE);
                lineOptions.geodesic(true);
            }
           polyline= mMap.addPolyline(lineOptions);

        }
    }

}
