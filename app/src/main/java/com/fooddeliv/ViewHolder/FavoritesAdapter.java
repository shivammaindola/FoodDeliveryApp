package com.fooddeliv.ViewHolder;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.fooddeliv.Common.Common;
import com.fooddeliv.Database.Database;
import com.fooddeliv.FoodDetail;
import com.fooddeliv.FoodList;
import com.fooddeliv.Interface.ItemClickListener;
import com.fooddeliv.Model.Favorites;
import com.fooddeliv.Model.Food;
import com.fooddeliv.Model.Order;
import com.fooddeliv.R;
import com.squareup.picasso.Picasso;

import java.util.List;

public class FavoritesAdapter extends RecyclerView.Adapter<Favorite_view_holder> {

    private Context context;
    private List<Favorites> favoritesList;

    public FavoritesAdapter(Context context, List<Favorites> favoritesList) {
        this.context = context;
        this.favoritesList= favoritesList;
    }

    @NonNull
    @Override
    public Favorite_view_holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
       View itemView= LayoutInflater.from(context)
               .inflate(R.layout.favorites_item,parent,false);
       return new Favorite_view_holder(itemView);

    }

    @Override
    public void onBindViewHolder(@NonNull Favorite_view_holder viewHolder, final int position) {
        viewHolder.food_name.setText(favoritesList.get(position).getFoodName());
        viewHolder.pricetag.setText(favoritesList.get(position).getFoodPrice().toString());
        Picasso.with(context).load(favoritesList.get(position).getFoodImage())
        .into(viewHolder.food_image);

        //Quick cart
        viewHolder.food_cart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isExists=new Database(context).checkfoodexists(favoritesList.get(position).getFoodId(), Common.currentUser.getPhone());

                if (!isExists) {
                    new Database(context).addToCart(new Order(
                            Common.currentUser.getPhone(),
                            favoritesList.get(position).getFoodId(),
                            favoritesList.get(position).getFoodName(),
                            "1",
                            favoritesList.get(position).getFoodPrice(),
                            favoritesList.get(position).getFoodDiscount(),
                            favoritesList.get(position).getFoodImage()
                    ));


                } else {
                    new Database(context).increaseCart(Common.currentUser.getPhone(), favoritesList.get(position).getFoodId());

                }
                Toast.makeText(context, "Added to cart", Toast.LENGTH_SHORT).show();

            }

        });

        final Favorites local = favoritesList.get(position);
        viewHolder.setItemClickListener(new ItemClickListener() {
            @Override
            public void onClick(View view, int position, boolean isLongClick) {
                //start new activity
                Intent foodDetail = new Intent(context, FoodDetail.class);
                foodDetail.putExtra("FoodId", favoritesList.get(position).getFoodId());
                context.startActivity(foodDetail);
            }
        });
    }

    @Override
    public int getItemCount() {
        return favoritesList.size();
    }

    public void removeItem(int position)
    {
        favoritesList.remove(position);
        notifyItemRemoved(position);
    }
    public void restoreItem(Favorites item, int position)
    {
        favoritesList.add(position,item);
        notifyItemInserted(position);
    }

    public Favorites getItem(int position)
    {
        return favoritesList.get(position);
    }
}
