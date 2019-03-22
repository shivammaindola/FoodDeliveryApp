package com.fooddeliv.Common;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.fooddeliv.Model.User;
import com.fooddeliv.Retrofit.APIService;
import com.fooddeliv.Retrofit.FCMRetrofitClient;
import com.fooddeliv.Retrofit.IGoogleService;
import com.fooddeliv.Retrofit.RetrofitClient;

/**
 * Created by 123456 on 2017/11/17.
 */

public class Common {
    public static User currentUser;

    public static String topicName="News";

    public static String PHONE_TEXT= "userPhone";

    public static final String BASE_URL= "https://fcm.googleapis.com/";

    public static final String GOOGLE_API_URL= "https://maps.googleapis.com/";

    public static final String INTENT_FOOD_ID="FoodId";
    public static String currentKey;

    public static APIService getFCMClient(){
        return FCMRetrofitClient.getClient(BASE_URL).create(APIService.class);
    }

        public static IGoogleService getGoogleMapApi()
    {
        return RetrofitClient.getClient(GOOGLE_API_URL).create(IGoogleService.class);

    }
    public static final String DELETE = "Delete";
    public static final String USER_KEY = "User";
    public static final String PWD_KEY = "Password";

    public static String convertCodeToStatus(String code)
    {
        if(code.equals("0"))
            return "Placed";
        else if(code.equals("1"))
            return "On my way";
        else if(code.equals("2"))
            return "Shipping";
        else
            return "Shipped";
    }

    public static boolean isConnectionToInternet(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkInfo[] networkInfo = connectivityManager.getAllNetworkInfo();
            if (networkInfo != null) {
                for (int i = 0; i < networkInfo.length; i++) {
                    if (networkInfo[i].getState() == NetworkInfo.State.CONNECTED)
                        return true;
                }
            }

        }
        return false;
    }
}
