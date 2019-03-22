package com.fooddeliv;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.transition.Slide;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.andremion.counterfab.CounterFab;
import com.fooddeliv.Common.Common;
import com.fooddeliv.Interface.ItemClickListener;
import com.fooddeliv.Model.Banner;
import com.fooddeliv.Model.Category;
import com.fooddeliv.Model.User;
import com.fooddeliv.ViewHolder.MenuViewHolder;
import com.daimajia.slider.library.Animations.DescriptionAnimation;
import com.daimajia.slider.library.SliderLayout;
import com.daimajia.slider.library.SliderTypes.BaseSliderView;
import com.daimajia.slider.library.SliderTypes.TextSliderView;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.common.internal.StringResourceValueReader;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.squareup.picasso.Picasso;

import com.fooddeliv.Model.Token;

import java.util.HashMap;
import java.util.Map;

import dmax.dialog.SpotsDialog;
import io.paperdb.Paper;

public class Home extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    FirebaseDatabase database;
    DatabaseReference category;
    TextView txtFullName;
    RecyclerView recycler_menu;
    RecyclerView.LayoutManager layoutManager;

    SwipeRefreshLayout swipeRefreshLayout;
    CounterFab fab;

    //slider
    HashMap<String,String> image_list;
    SliderLayout mslider;
    ImageView search_img;

    FirebaseRecyclerAdapter<Category, MenuViewHolder> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Log.i("ERROR", "at starting ");
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Menu");

        //Init Firebase
        database = FirebaseDatabase.getInstance();
        category = database.getReference("Category");
        Log.i("Error", "after category: ");
        Paper.init(this);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //Set Name for user
        View headerView = navigationView.getHeaderView(0);
        txtFullName = headerView.findViewById(R.id.txtFullName);
        txtFullName.setText(Common.currentUser.getName());

        //Load menu
        recycler_menu = findViewById(R.id.recycler_menu);
        recycler_menu.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        //recycler_menu.setLayoutManager(layoutManager);
         recycler_menu.setLayoutManager(new GridLayoutManager(this,2));
        // loadMenu();
        updateToken(FirebaseInstanceId.getInstance().getToken());


        search_img=(ImageView)findViewById(R.id.searchimg);
        search_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Home.this, SearchActivity.class));
            }
        });

        //swipe
        swipeRefreshLayout=(SwipeRefreshLayout)findViewById(R.id.swipe_layout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                if(Common.isConnectionToInternet(getBaseContext()))
                    loadMenu();
                else
                {

                    Toast.makeText(Home.this, "Please check your connection!!!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        });

        //Default

        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {

                if(Common.isConnectionToInternet(getBaseContext())) {
                    Log.i("Error", "inside swipe: ");
                    loadMenu();

                }else
                {

                    Toast.makeText(Home.this, "Please check your connection!!!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        });




        CounterFab fab = (CounterFab) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent cartIntent = new Intent(Home.this,Cart.class);
                startActivity(cartIntent);

            }
        });

       // fab.setCount(new Database(this).getCountCart());


      //setup slider
        setupslider();
    }

    private void setupslider() {
        mslider=(SliderLayout)findViewById(R.id.slider);
        image_list= new HashMap<>();
        final DatabaseReference banners= database.getReference("Banner");
        banners.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for(DataSnapshot postSnapshot:dataSnapshot.getChildren())
                {
                    Banner banner=postSnapshot.getValue(Banner.class);
                    image_list.put(banner.getName()+"@@@"+banner.getId(), banner.getImage());

                }
                for(String key:image_list.keySet())
                {
                    String[] keySplit=key.split("@@@");
                    String nameoffood=keySplit[0];
                    String idoffood= keySplit[1];

                    //create slider
                    final TextSliderView textSliderView= new TextSliderView(getBaseContext());
                    textSliderView.description(nameoffood)
                            .image(image_list.get(key))
                            .setScaleType(BaseSliderView.ScaleType.Fit)
                            .setOnSliderClickListener(new BaseSliderView.OnSliderClickListener() {
                                @Override
                                public void onSliderClick(BaseSliderView slider) {
                               Intent intent= new Intent(Home.this, FoodDetail.class);
                                intent.putExtras(textSliderView.getBundle());
                               startActivity(intent);
                                }
                            });

                    //add extra bundle
                    textSliderView.bundle(new Bundle());
                    textSliderView.getBundle().putString("FoodId", idoffood);

                    mslider.addSlider(textSliderView);
                    //remove
                    banners.removeEventListener(this);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        mslider.setPresetTransformer(SliderLayout.Transformer.Background2Foreground);
        mslider.setPresetIndicator(SliderLayout.PresetIndicators.Center_Bottom);
        mslider.setCustomAnimation(new DescriptionAnimation());
        mslider.setDuration(4000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("ERROR", "inside onResume: ");
        //fab.setCount(new Database(this).getCountCart());

        //fix click back button
        if(adapter!=null)
            adapter.startListening();
    }

    private void updateToken(String token) {
         FirebaseDatabase db = FirebaseDatabase.getInstance();
         DatabaseReference tokens = db.getReference("Tokens");
         Token data = new Token(token, false);
         tokens.child(Common.currentUser.getPhone()).setValue(data);

     }

    private void loadMenu() {
        Log.i("ERROR", "inside loadmenu: ");

        FirebaseRecyclerOptions<Category> options= new FirebaseRecyclerOptions.Builder<Category>()
                .setQuery(category, Category.class)
                .build();
        Log.i("ERROR", "after options: ");
        adapter= new FirebaseRecyclerAdapter<Category, MenuViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull MenuViewHolder viewHolder, int position, @NonNull Category model) {
                viewHolder.txtMenuName.setText(model.getName());
                Picasso.with(getBaseContext()).load(model.getImage()).into(viewHolder.imageView);
                final Category clickItem = model;
                viewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        //Get CategoryId and send to new Activity
                        Intent foodList = new Intent(Home.this, FoodList.class);
                        //Because CategoryId is key, so we just get the key of this item
                        foodList.putExtra("CategoryId", adapter.getRef(position).getKey());
                        startActivity(foodList);
                    }
                });
            }

            @NonNull
            @Override
            public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                Log.i("ERROR", "after oncreate: ");
                View itemView= LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.menu_item, viewGroup,false);
                return new MenuViewHolder(itemView);
            }
        };

        adapter.startListening();
        recycler_menu.setAdapter(adapter);
        swipeRefreshLayout.setRefreshing(false);
        //adapter.notifyDataSetChanged();
    }
    @Override
    protected void onStop(){
        super.onStop();
        Log.i("ERROR", "inside onstop: ");
        adapter.stopListening();
        mslider.stopAutoCycle();
    }

    @Override
    protected void onStart() {
        super.onStart();
      //  adapter.startListening();
        mslider.startAutoCycle();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      if(item.getItemId()==R.id.menu_search)
          startActivity(new Intent(Home.this, SearchActivity.class));
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_menu) {
            return true;
        }else if(id == R.id.nav_cart){
            Intent cartIntent = new Intent(Home.this, Cart.class);
            startActivity(cartIntent);

        }else if(id == R.id.nav_orders){
            Intent orderIntent = new Intent(Home.this, OrderStatus.class);
            startActivity(orderIntent);

        }else if(id == R.id.nav_update_name){
             updatename();

        }else if(id == R.id.nav_settings){
            showsettingsdialog();

        }else if(id == R.id.nav_fav){
            startActivity(new Intent(Home.this,FavoritesActivity.class));

        }
        else if(id == R.id.nav_log_out){
            //delete remember
            Paper.book().destroy();
            Intent signIn = new Intent(Home.this, MainActivity.class);
            signIn.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(signIn);
        } else if(id==R.id.nav_home_address){
            showcustomeaddressdialog();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showsettingsdialog() {
        AlertDialog.Builder builder= new AlertDialog.Builder(this);
        builder.setTitle("Settings");
       // builder.setMessage("Fill all the information");

        LayoutInflater inflater= this.getLayoutInflater();
        View layout_name= inflater.inflate(R.layout.setting_layout, null);

        builder.setView(layout_name);
        //  builder.setIcon(R.drawable.ic_security_black_24dp);

        //checkbox
        final CheckBox ckb_subscribe_new=(CheckBox)layout_name.findViewById(R.id.chk_sub_new);

        //add code remember state of checkbox
        Paper.init(this);
        String isSubscribe=Paper.book().read("sub_nav");
        if(isSubscribe==null|| TextUtils.isEmpty(isSubscribe)||isSubscribe.equals("false"))
            ckb_subscribe_new.setChecked(false);
        else
            ckb_subscribe_new.setChecked(true);

        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();

                if(ckb_subscribe_new.isChecked())
                {
                    FirebaseMessaging.getInstance().subscribeToTopic(Common.topicName);
                    //write value
                    Paper.book().write("sub_new", "true");

                }
                else
                {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(Common.topicName);
                    //write value
                    Paper.book().write("sub_new","false");
                }

            }
        });

        builder.show();
    }

    private void updatename() {
        AlertDialog.Builder builder= new AlertDialog.Builder(this);
        builder.setTitle("UPDATE NAME");
        builder.setMessage("Fill all the information");

        LayoutInflater inflater= this.getLayoutInflater();
        View layout_name= inflater.inflate(R.layout.update_name, null);

        builder.setView(layout_name);
      //  builder.setIcon(R.drawable.ic_security_black_24dp);

        final MaterialEditText edtName=(MaterialEditText)layout_name.findViewById(R.id.update_name);
        builder.setPositiveButton("Change", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        final android.app.AlertDialog waitingDialog = new SpotsDialog.Builder()
                                .setContext(Home.this)
                                .setMessage("Please wait")
                                .setCancelable(false)
                                .build();

                        //update name

                        Map<String, Object> update_name = new HashMap<>();
                        update_name.put("name", edtName.getText().toString());
                        txtFullName.setText(edtName.getText().toString());

                        FirebaseDatabase.getInstance()
                                .getReference("User")
                                .child(Common.currentUser.getPhone())
                                .updateChildren(update_name)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {

                                        waitingDialog.dismiss();
                                        if (task.isSuccessful())
                                            Toast.makeText(Home.this, "Name was updated", Toast.LENGTH_SHORT).show();
                                    }
                                });

                    }
                });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        builder.show();
    }

    private void showcustomeaddressdialog() {
        AlertDialog.Builder alertDialog=new AlertDialog.Builder(Home.this);
        alertDialog.setTitle("Change Home Address");
        alertDialog.setTitle("Please fill all information");
        LayoutInflater inflater= this.getLayoutInflater();
        View layout_home= inflater.inflate(R.layout.home_address,null);

        final MaterialEditText edtHomeaddress=(MaterialEditText)layout_home.findViewById(R.id.custom_address);

        alertDialog.setView(layout_home);

        alertDialog.setPositiveButton("UPDATE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();

                //set new home address
                Common.currentUser.setHomeAddress(edtHomeaddress.getText().toString());
                //set new address
                FirebaseDatabase.getInstance().getReference("User")
                        .child(Common.currentUser.getPhone())
                        .setValue(Common.currentUser)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Toast.makeText(Home.this, "Update Address succesfully", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
        alertDialog.show();

    }

}


