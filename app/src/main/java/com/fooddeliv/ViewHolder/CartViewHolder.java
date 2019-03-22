package com.fooddeliv.ViewHolder;


import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fooddeliv.Common.Common;
import com.fooddeliv.Interface.ItemClickListener;
import com.fooddeliv.R;
import com.cepheuen.elegantnumberbutton.view.ElegantNumberButton;

public class CartViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {

    public TextView txt_cart_name, txt_price;
    public ImageView img_cart_count;
    public ElegantNumberButton btn_quantity;
    public RelativeLayout view_background;
    public LinearLayout view_foreground;


    public void setTxt_cart_name(TextView txt_cart_name) {
        this.txt_cart_name = txt_cart_name;
    }

    public CartViewHolder(View itemView){
        super(itemView);
        txt_cart_name = itemView.findViewById(R.id.cart_item_name);
        txt_price = itemView.findViewById(R.id.cart_item_Price);
        btn_quantity= itemView.findViewById(R.id.btn_quantity);
        view_background=(RelativeLayout)itemView.findViewById(R.id.view_background);
        view_foreground=(LinearLayout)itemView.findViewById(R.id.view_foreground);
        // img_cart_count = itemView.findViewById(R.id.cart_item_count);
        itemView.setOnCreateContextMenuListener(this);
        //itemView.setOnClickListener(this);
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.setHeaderTitle("Select your action");

        menu.add(0,0,getAdapterPosition(), Common.DELETE);
    }
}
