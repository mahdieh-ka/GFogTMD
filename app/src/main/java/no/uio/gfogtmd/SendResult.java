package no.uio.gfogtmd;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

class SendResult {
   private static Context context;
   private static String TAG = SendResult.class.getSimpleName();
   //if we connect to thr spring server through the same LAN it is i wifi connection
   //Otherwise we connect through 4G network using one smartphone as the hotspot hub
   private boolean wificonnection =true;
   private  String url;



   static void setContext(Context ctx) {
      context =ctx;
   }


   public void postRequest(String transportMode, String tripId, String timeToClassify, String startDate,
                           String endDate, String formattedTimestamp) {
      RequestQueue queue = Volley.newRequestQueue(context);
      if(wificonnection)
      {
         url = "http://192.168.10.113:8080/postSendResult";
      }
      else {
         url = "http://192.168.72.188:8080/postSendResult";
      }


      url = url + "?transportMode=" + transportMode +
              "&tripId=" + tripId +
              "&timeToClassify=" + timeToClassify +
              "&startDate=" + startDate +
              "&endDate=" + endDate +
              "&formattedTimestamp=" + formattedTimestamp;


      int connectionCheck = isConnectedToServer(url);
      if (connectionCheck == 1){
         Log.d(TAG, "Connected to the fog node");}
      else{
         Log.d(TAG, "No connection to the fog node!!!!!");}

      // Capture the timestamp when sending the data
      LocalDateTime sendTimestamp = LocalDateTime.now();

      // Create a StringRequest with POST method
      StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
         @Override
         public void onResponse(String response) {
            // Handle the server response here if needed

            // Capture the timestamp when receiving the response
            LocalDateTime receiveTimestamp = LocalDateTime.now();
            // apply until method of LocalDateTime class
            long latency = ChronoUnit.MILLIS.between(sendTimestamp, receiveTimestamp);

            // Log both timestamps
            Log.d("Latency measurement", "Data sent at: " + sendTimestamp);
            Log.d("Latency measurement", "Response received at: " + receiveTimestamp);
            Log.d("Latency measurement", "Latency is equalt: " + latency+ "ms");
         }
      }, new Response.ErrorListener() {
         @Override
         public void onErrorResponse(VolleyError error) {
            // Handle errors here if needed
            Log.e("RequestError", "Error: " + error.getMessage());
         }
      }) {

      };

      queue.add(stringRequest);
   }


   public Integer isConnectedToServer(String url) {
      try{
         URL myUrl = new URL(url);
         URLConnection connection = myUrl.openConnection();
         connection.connect();
         return 1;
      } catch (Exception e) {
         // Handle your exceptions
         return 0;
      }
   }



}
