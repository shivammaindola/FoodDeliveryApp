package com.fooddeliv.Retrofit;
import com.fooddeliv.Model.DataMessage;
import com.fooddeliv.Model.MyResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {

   @Headers(
           {
                   "Content-Type:application/json",
                   "Authorization:key=AAAAUUNt6JQ:APA91bFRoyfXl5cvbEGKnsBhKoHg_aoP_A-vIYWofttlDfav2JZtA8kN43y-p3zsaID62Xs4wNEmXi_yocGY_w0_usJJ0kwahXDgvQKg6XDUtdn0E_RrBcBJVUb1hh6y38dXQrjeYLp-"
           }
   )
@POST("fcm/send")
   Call<MyResponse> sendNotification(@Body DataMessage body);
}
