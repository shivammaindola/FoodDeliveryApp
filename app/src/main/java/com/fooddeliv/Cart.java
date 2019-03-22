package com.fooddeliv;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fooddeliv.Common.Common;
import com.fooddeliv.Database.Database;
import com.fooddeliv.Helper.RecyclerItemTouchHelper;
import com.fooddeliv.Interface.RecyclerItemTouchHelperListener;
import com.fooddeliv.Model.DataMessage;
import com.fooddeliv.Model.MyResponse;
import com.fooddeliv.Model.Order;
import com.fooddeliv.Model.Request;
import com.fooddeliv.Model.Token;
import com.fooddeliv.Retrofit.APIService;
import com.fooddeliv.Retrofit.IGoogleService;
import com.fooddeliv.ViewHolder.CartAdapter;
import com.fooddeliv.ViewHolder.CartViewHolder;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.rengwuxian.materialedittext.MaterialEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class Cart extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
GoogleApiClient.OnConnectionFailedListener, LocationListener, RecyclerItemTouchHelperListener {

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

    FirebaseDatabase database;
    DatabaseReference requests;

    public TextView txtTotalPrice;
    Button btnPlace;
    String address;

    List<Order> cart=new ArrayList<>();

    CartAdapter adapter;

    APIService mService;
    Place shippingAddress;

    //declare google map api
    IGoogleService mGoogleMapService;

    RelativeLayout rootLayout;

    //location
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;

    private static final int UPDATE_INTERVAL=5000;
    private static final int FATEST_INTERVAL=3000;
    private static final int DISPLACEMENT=10;

    private static final int LOCATION_REQUEST_CODE=9999;
    private static final int PLAY_SERVICES_REQUEST=9999;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        //init
        mGoogleMapService=Common.getGoogleMapApi();



        //init
        rootLayout=(RelativeLayout)findViewById(R.id.rootLayout);
        //request permissision
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
        {
           ActivityCompat.requestPermissions(this, new String[]
                   {
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION

        },LOCATION_REQUEST_CODE);
        }
        else
        {
            if(checkPlayServices())
            {
                buildGoogleApiClient();
                createLocationRequest();
            }
        }

        //init Service
        mService=Common.getFCMClient();

        //firebase
        database=FirebaseDatabase.getInstance();
        requests=database.getReference("Requests");

        //init
        recyclerView=(RecyclerView)findViewById(R.id.listCart);
        recyclerView.setHasFixedSize(true);
        layoutManager= new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        txtTotalPrice=(TextView)findViewById(R.id.total);
        btnPlace=(Button)findViewById(R.id.btnPlaceOrder);

        //swipe to delete
        ItemTouchHelper.SimpleCallback itemTouchHelperCallback= new RecyclerItemTouchHelper(0, ItemTouchHelper.LEFT,this);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);

        btnPlace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(cart.size()>0)
                    showAlertDialog();
                else
                    Toast.makeText(Cart.this, "Your Cart is Empty!", Toast.LENGTH_SHORT).show();
            }
        });

        loadListFood();
    }


    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if(viewHolder instanceof CartViewHolder)
        {
            String name=((CartAdapter)recyclerView.getAdapter()).getItem(position).getProductName();

            final Order deletitem=((CartAdapter)recyclerView.getAdapter()).getItem(viewHolder.getAdapterPosition());
            final int deleteIndex= viewHolder.getAdapterPosition();

            adapter.removeItem(deleteIndex);
            new Database(getBaseContext()).removeFromCart(deletitem.getProductId(),Common.currentUser.getPhone());

            //Update txttotal
            int total=0;
            List<Order> orders= new Database(getBaseContext()).getCarts(Common.currentUser.getPhone());
            for(Order item: orders)
                total+=(Integer.parseInt(item.getPrice()))*(Integer.parseInt(item.getQuantity()));
            Locale locale=new Locale("hi","IN");
            NumberFormat fmt=NumberFormat.getCurrencyInstance(locale);

            txtTotalPrice.setText(fmt.format(total));

            //Make Snackbar
            Snackbar snackbar= Snackbar.make(rootLayout, name+" removed from cart",Snackbar.LENGTH_LONG);
            snackbar.setAction("UNDO", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    adapter.restoreItem(deletitem,deleteIndex);
                    new Database(getBaseContext()).addToCart(deletitem);

                    //update txttotal
                    //Update txttotal
                    int total=0;
                    List<Order> orders= new Database(getBaseContext()).getCarts(Common.currentUser.getPhone());
                    for(Order item: orders)
                        total+=(Integer.parseInt(item.getPrice()))*(Integer.parseInt(item.getQuantity()));
                    Locale locale=new Locale("hi","IN");
                    NumberFormat fmt=NumberFormat.getCurrencyInstance(locale);

                    txtTotalPrice.setText(fmt.format(total));

                }
            });
            snackbar.setActionTextColor(Color.YELLOW);
            snackbar.show();
        }
    }


    private void createLocationRequest() {

        mLocationRequest= new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);

    }

    private synchronized void buildGoogleApiClient() {

        mGoogleApiClient= new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
        mGoogleApiClient.connect();

    }

    private boolean checkPlayServices() {
        int resultCode= GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(resultCode!= ConnectionResult.SUCCESS)
        {
            if(GooglePlayServicesUtil.isUserRecoverableError(resultCode))
                 GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_REQUEST).show();
            else
            {
                Toast.makeText(this, "This device is not supported", Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }
        return true;
    }


    private void showAlertDialog() {
        AlertDialog.Builder alertDialog=new AlertDialog.Builder(Cart.this);
        alertDialog.setTitle("One more step!");
        alertDialog.setTitle("Enter your address and comment too ");

        LayoutInflater inflater= this.getLayoutInflater();
        View order_address_comment= inflater.inflate(R.layout.order_address_comment,null);

        final PlaceAutocompleteFragment edtAddress= (PlaceAutocompleteFragment)getFragmentManager().findFragmentById(R.id.place_autocomplete);
        edtAddress.getView().findViewById(R.id.place_autocomplete_search_button).setVisibility(View.GONE);
        ((EditText)edtAddress.getView().findViewById(R.id.place_autocomplete_search_input)).setHint("Enter your Address");

        ((EditText)edtAddress.getView().findViewById(R.id.place_autocomplete_search_input))
                .setTextSize(18);

        edtAddress.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
            shippingAddress=place;
            }

            @Override
            public void onError(Status status) {

            }
        });

         //Radio
        final RadioButton shipToAddress=(RadioButton)order_address_comment.findViewById(R.id.shiptoaddress);
        final RadioButton HomeAddress=(RadioButton)order_address_comment.findViewById(R.id.Homeaddress);

        //Event radio
        HomeAddress.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                if (b) {
                    if(Common.currentUser.getHomeAddress()!=null||!TextUtils.isEmpty(Common.currentUser.getHomeAddress()))
                    {
                        address=Common.currentUser.getHomeAddress();
                        ((EditText)edtAddress.getView().findViewById(R.id.place_autocomplete_search_input)).setText(address);

                    }
                    else
                        Toast.makeText(Cart.this, "Please update your home address", Toast.LENGTH_SHORT).show();
                }
            }

        });
        shipToAddress.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                //ship to this address
                if(b)
                {
                   mGoogleMapService.getAddressName(String.format("https://maps.googleapis.com/maps/api/geocode/json?latlng=%f,%f&sensor=false&key=AIzaSyC3PmFURd0TliXOf0hWs2jkMKVoEBt5Vnk",
                           mLastLocation.getLatitude(),
                           mLastLocation.getLongitude()))
                           .enqueue(new Callback<String>() {
                               @Override
                               public void onResponse(Call<String> call, Response<String> response) {
                                   //if fetch api
                                   try{
                                       JSONObject jsonObject= new JSONObject(response.body().toString());

                                       JSONArray resultArray=jsonObject.getJSONArray("results");

                                       JSONObject firstObject= resultArray.getJSONObject(0);

                                       address=firstObject.getString("formatted_address");
                                       //set this address
                                       ((EditText)edtAddress.getView().findViewById(R.id.place_autocomplete_search_input)).setText(address);



                                   } catch (JSONException e){
                                       e.printStackTrace();
                                   }
                               }

                               @Override
                               public void onFailure(Call<String> call, Throwable t) {

                               }
                           });
                }
            }
        });

        //final MaterialEditText edtAddress=(MaterialEditText)order_address_comment.findViewById(R.id.edtAddress);

        final MaterialEditText edtComment=(MaterialEditText)order_address_comment.findViewById(R.id.edtComment);

        alertDialog.setView(order_address_comment);
        alertDialog.setIcon(R.drawable.ic_shopping_cart_black_24dp);

        //Enter Phone no.
        /*final EditText edtPhone = new EditText(Cart.this);
        alertDialog.setTitle("Enter your Phone no. ");
        edtPhone.setLayoutParams(lp);
        alertDialog.setView(edtPhone);// Add edit phone no. to alert dialog */


        alertDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {

                //check condition of address
                if(!shipToAddress.isChecked()&&!HomeAddress.isChecked()){
                    if(shippingAddress!=null)
                        address=shippingAddress.getAddress().toString();
                    else
                    {
                        Toast.makeText(Cart.this, "Please enter address or select option", Toast.LENGTH_SHORT).show();

                    //fix crash fragment
                        getFragmentManager().beginTransaction()
                                .remove(getFragmentManager().findFragmentById(R.id.place_autocomplete))
                                .commit();

                        return;
                    }
                }

                if(TextUtils.isEmpty(address))
                {
                    Toast.makeText(Cart.this, "Please enter address or select option address", Toast.LENGTH_SHORT).show();
                    //fix crash fragment
                    getFragmentManager().beginTransaction()
                            .remove(getFragmentManager().findFragmentById(R.id.place_autocomplete))
                            .commit();

                    return;
                }
                Request request= new Request(
                        Common.currentUser.getPhone(),
                        Common.currentUser.getName(),
                        address,
                        txtTotalPrice.getText().toString(),
                        "0",
                        edtComment.getText().toString(),
                        String.format("%s,%s", mLastLocation.getLatitude(),mLastLocation.getLongitude()),
                        cart
                );
                //submit to firebase
                //we will use System.CurrentMilli to key
                String order_number= String.valueOf(System.currentTimeMillis());
                
                requests.child(order_number)
                        .setValue(request);
                //Delete Cart
                new Database(getBaseContext()).cleanCart(Common.currentUser.getPhone());
                sendNotificationOrder(order_number);
                Toast.makeText(Cart.this, "Thank you, Order Placed", Toast.LENGTH_SHORT).show();


                finish();

                //Intent payment= new Intent(Cart.this, Payment.class);
                //startActivity(payment);

            }
        });

        alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                dialog.dismiss();

                //remove fragment
                getFragmentManager().beginTransaction()
                        .remove(getFragmentManager().findFragmentById(R.id.place_autocomplete))
                        .commit();

            }
        });

        alertDialog.show();
    }

    private void sendNotificationOrder(final String order_number) {
        DatabaseReference tokens=FirebaseDatabase.getInstance().getReference("Tokens");
        tokens.orderByChild("isServerToken").equalTo(true)
        .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(dataSnapshot.exists())
                {
                Token serverToken = dataSnapshot.getValue(Token.class);
                //cretae raw payload to send
                Map<String, String> dataSend = new HashMap<>();
                dataSend.put("title", "To Server");
                dataSend.put("message", "you have new order" + order_number);
                DataMessage dataMessage = new DataMessage(serverToken.getToken(), dataSend);

                //  String test= new Gson().toJson(dataMessage);
                Log.d("Content", "Chal bhi rha hai ya nhi");

                mService.sendNotification(dataMessage)
                        .enqueue(new Callback<MyResponse>() {
                            @Override
                            public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                                if (response.code() == 200) {
                                    //only run when get result
                                    if (response.body().success== 1) {
                                        Toast.makeText(Cart.this, "Thank you, Order Place", Toast.LENGTH_SHORT).show();
                                        finish();
                                    } else {
                                        Toast.makeText(Cart.this, "Failed", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }

                            @Override
                            public void onFailure(Call<MyResponse> call, Throwable t) {
                                Log.e("ERROR", t.getMessage());
                            }
                        });
            }
        }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void loadListFood() {
        cart= new Database(this).getCarts(Common.currentUser.getPhone());
        adapter= new CartAdapter(cart,this);
        adapter.notifyDataSetChanged();
        recyclerView.setAdapter(adapter);

        //calculate total price
        int total=0;
        for(Order order:cart)
            total+=(Integer.parseInt(order.getPrice()))*(Integer.parseInt(order.getQuantity()));
        Locale locale=new Locale("hi","IN");
        NumberFormat fmt=NumberFormat.getCurrencyInstance(locale);

        txtTotalPrice.setText(fmt.format(total));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if(item.getTitle().equals(Common.DELETE))
        deletCart(item.getOrder());
        return true;
    }

    private void deletCart(int order) {
        // we will remove item at list<Order> by position
        cart.remove(order);
        //after that we will delete all old data from sqllite
        new Database(this).cleanCart(Common.currentUser.getPhone());
        //and final, we will update item from list<order> to sqlite
        for(Order item:cart)
            new Database(this).addToCart(item);
        //refresh
        loadListFood();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
         displayLocation();
         startLocationUpdate();
    }

    private void startLocationUpdate() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest,this);
    }

    private void displayLocation() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        mLastLocation= LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(mLastLocation!=null){
            Log.d("LOCATION", "YOUR LOCATION : "+mLastLocation.getLatitude()+" "+mLastLocation.getLongitude());
        }
        else
            Log.d("LOCATION", " could not get your location");


    }


    @Override
    public void onConnectionSuspended(int i) {
  mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        displayLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
     switch (requestCode)
     {
         case LOCATION_REQUEST_CODE:
         {
             if(grantResults.length>0&& grantResults[0]==PackageManager.PERMISSION_GRANTED)
             {
                 if(checkPlayServices())
                 {
                     buildGoogleApiClient();
                     createLocationRequest();
                 }
             }
         }
     }
    }

}

