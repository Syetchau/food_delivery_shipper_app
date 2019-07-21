package com.example.liew.ideliveryshipper.ViewHolder;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.liew.ideliveryshipper.R;

import info.hoang8f.widget.FButton;

public class OrderViewHolder extends RecyclerView.ViewHolder{

    public TextView txtOrderId, txtOrderStatus, txtOrderPhone, txtOrderAddress, txtOrderDate, txtOrderName, txtOrderPrice;
    public Button btnShipping;

    public OrderViewHolder(View itemView) {
        super(itemView);

        txtOrderId = (TextView)itemView.findViewById(R.id.order_id);
        txtOrderStatus = (TextView)itemView.findViewById(R.id.order_status);
        txtOrderPhone = (TextView)itemView.findViewById(R.id.order_phone);
        txtOrderAddress = (TextView)itemView.findViewById(R.id.order_address);
        txtOrderDate = (TextView)itemView.findViewById(R.id.order_date);
        txtOrderName = (TextView)itemView.findViewById(R.id.order_name);
        txtOrderPrice = (TextView)itemView.findViewById(R.id.order_price);

        btnShipping = (Button)itemView.findViewById(R.id.btnShipping);

    }
}
