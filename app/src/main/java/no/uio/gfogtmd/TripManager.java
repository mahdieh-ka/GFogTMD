package no.uio.gfogtmd;

import android.content.Context;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.util.Log;
import android.os.Looper;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * Class responsible for managing and collecting trip data.
 */
public class TripManager {

    /**
     *
     *
     *  When in trip, there are two possible states: Trip, Waiting Event.
     *
     *  A trip starts if the user commutes to place at least 100 meters away (goes from Still state to
     *  the Trip state)
     *  Throughout a trip, if the user stay within a 100 meters radius for more than 5 minutes, user
     *  go to the WaitingEvent state. If the user leaves a 100 meters radius again,
     *  goes back to Trip state. If the the user stays within a 100 meters radius for another 25
     *  minutes, the trip is ended (goes to still state, the last waiting event is removed - because the
     *  trip has ended and the trip is saved).
     *
     *
     *
     */

    // Constants
    /**
     * minimum radius(meters)  - more than 100 meters to account for the false positives
     */
    final static int tripDistanceLimit = 115;

    /**
     * time constant - 5 minutes in milliseconds - time within a tripDistance limit to go from trip
     * to waiting event
     */
    int tripTimeLimit = 5 * 60;

    /**
     * time constant - 25 minutes in milliseconds - time within a tripDistance limit to go from
     * waiting event state to still state (end of trip)
     */
    int fullTripTimeLimit = 25 * 60 * 1000;

    /**
     * time constant - 30 minutes in milliseconds - time interval which a recovered trip snapshot is
     * considered to be valid.
     */
    final int tripSnapshotFreshTime = 30 * 60 * 1000;

    private static String TAG = "TripManager";
    private SensorManager sensorManager;
    private AccListener accelerationListener;
    private Sensor accelerometer, magnetometer;
    private MagListener magnetometerListener;
    private FusedLocationProviderClient locationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    public static Trip currentTrip;
    private static final long ACCEL_SAMPLING_PERIOD = TimeUnit.SECONDS.toMicros(1);
    private static final long GPS_SAMPLING_INTERVAL = TimeUnit.SECONDS.toMillis(10); // 10 seconds in MILIseconds
    private boolean tripInProgress = false;
    public static Integer tripId;
    private long startTime;
    float predictedDistance;
    private MLPClassifier classifier;
    private List<Float> classificationResults =new ArrayList<>();



    TripManager(Context context) {
        // setup accelerometer
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        accelerationListener = new AccListener();

        //setup magnetometer
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        magnetometerListener = new MagListener();

        // setup location params
        locationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        locationRequest = new LocationRequest();
        locationRequest.setInterval(GPS_SAMPLING_INTERVAL);
        locationRequest.setMaxWaitTime(GPS_SAMPLING_INTERVAL);
        locationRequest.setPriority(locationRequest.PRIORITY_HIGH_ACCURACY);
        // callback to receive location updates
        locationCallback = new LocationListener();
    }


    /**
     * Starts recording a trip
     */
    void startTrip() {
        // Initialize the MLPClassifier
        classifier = MLPClassifier.getInstance();
        sensorManager.registerListener(accelerationListener, accelerometer, (int) ACCEL_SAMPLING_PERIOD, (int) ACCEL_SAMPLING_PERIOD);
        Log.d(TAG, "Started accelerometer");
        sensorManager.registerListener(magnetometerListener, magnetometer, (int) ACCEL_SAMPLING_PERIOD, (int) ACCEL_SAMPLING_PERIOD);
        Log.d(TAG, "Started magnetometer");
        locationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        Log.d(TAG, "Started GPS");
        tripInProgress = true;
        currentTrip = new Trip();
        currentTrip.start();



        //if there is any previously stored trip in the database get the last tripId
        Cursor cursor = MainActivity.rawDataDB.QueryTripIdAndMode();
        if (cursor.getCount()!= 0) {
            cursor.moveToLast();
            tripId = Integer.valueOf(cursor.getString(0));
        }
        else{ tripId =0; }
        //increase the tripId to store the new data with the new Id
        ++tripId;
        cursor.close();

    }


    /**
     * Stops recording a trip
     */
    void stopTrip() {
        // stop sensors
        sensorManager.unregisterListener(accelerationListener, accelerometer);
        Log.d(TAG, "Stopped accelerometer");
        sensorManager.unregisterListener(magnetometerListener, magnetometer);
        Log.d(TAG, "Stopped magnetometer ");
        locationProviderClient.removeLocationUpdates(locationCallback);
        Log.d(TAG, "Stopped GPS");

        // stop trip
        if (tripInProgress && currentTrip != null) {
            tripInProgress = false;
            currentTrip.finish();
            long startTime = System.nanoTime();
            // Perform classification using the MLPClassifier
            int transportModeId = classifier.classify();
            long endTime = System.nanoTime();
            long classificationTime = (endTime - startTime) / 1_000_000; // Convert to milliseconds
            Log.d(TAG, "Total classification time is " + classificationTime + " ms");
            Log.d(TAG, "-------------------------------------------------------------------");
            // Convert the time to milliseconds
            long tripTime = (currentTrip.getEndDate().getTime() - currentTrip.getStartDate().getTime());
            long tripTimeinSeconds = (tripTime/1000) ;
            Log.d(TAG, "What is trip time?: "+tripTimeinSeconds);


            String transportMode = Utility.transformModes(transportModeId);
            Log.d(TAG, "transportMode: " +transportMode);
            Log.d(TAG, "-------------------------------------------------------------------");
            // Android side
            LocalDateTime sendTimestamp = LocalDateTime.now();
            String formattedTimestamp = DateTimeFormatter.ISO_DATE_TIME.format(sendTimestamp);

            SendResult sendResult = new SendResult();
            if (transportMode != null ) {
                Log.d(TAG, "Sending classification results...: ");
                sendResult.postRequest(transportMode, tripId.toString(), String.valueOf(classificationTime), currentTrip.getStartDate().toString(), currentTrip.getEndDate().toString(),formattedTimestamp );
            }

            currentTrip.setModeId(transportModeId);
            currentTrip.setTimeToClassify(classificationTime);
            currentTrip.setDistance(predictedDistance);
            currentTrip.setTripId(tripId);



            // save trip
            TripRepository.save(currentTrip);
            currentTrip = null;

        }
    }






}
