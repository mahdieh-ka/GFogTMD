package no.uio.gfogtmd;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import java.util.ArrayList;

public class ClassifierResults extends AppCompatActivity {


    private static final String TAG = ClassifierResults.class.getSimpleName();
    TextView txtStart, txtEnd, txtTripId, txtDistance, txtLisOfProbabilities , txtListOfLegs , txtActualModes;
    String start, end, distance, tripId, tripProbs, tripLegs;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classifier_results);


        txtStart = findViewById(R.id.txtStart);
        txtEnd = findViewById(R.id.txtEnd);
        txtDistance = findViewById(R.id.txtDistance);
        txtTripId = findViewById(R.id.txtTripId);
        txtListOfLegs = findViewById(R.id.txtListOfLegs);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        start = extras.getString("START");
        end = extras.getString("END");
        distance = extras.getString("DISTANCE");
        tripId = extras.getString("TRIP_ID");
        tripLegs = extras.getString("LEGS");




        //set Start date and time
        txtStart.setText(start);

        //set End date and time
        txtEnd.setText(end);

        //set the final Distance
        txtDistance.setText(distance);

        //set the TripId
        txtTripId.setText(tripId);

        //set the legs info
        //showLegs();
        String[] legs = tripLegs.split("\n");
        ArrayList<Integer> legIds = new ArrayList<>();
        for (int i = 0 ; i < legs.length ; i++) {
            txtListOfLegs.setText(legs[i]);

        }

        txtListOfLegs.setText(tripLegs);



    }





}