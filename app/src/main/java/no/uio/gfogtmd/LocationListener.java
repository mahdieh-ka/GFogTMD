package no.uio.gfogtmd;
import android.location.Location;
import android.util.Log;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import java.time.Instant;


/**
 * Receives location readings from the smartphone and sends them to the classifier.
 */
public class LocationListener extends LocationCallback{

    public static Location location_result;
    private static String TAG = "LocationListener";


    @Override
    public void onLocationResult(LocationResult locationResult) {
        for (Location location : locationResult.getLocations()) {
            Log.d(TAG, "Got location update:" + location + "    time: " +Utility.getTime(location.getTime()));
            Log.d(TAG, "Got location update:" + location + "    time: " +Utility.getTime(Instant.now().toEpochMilli()));
            // Measure the start time for waiting time.
            long startTime = System.nanoTime();

            location_result = location;
            // Measure the end time for waiting time.
            long endTime = System.nanoTime();
            // Calculate and log the waiting time in milliseconds.
            long waitingTime = (endTime - startTime) / 1_000_000; // Convert to milliseconds

            // save data on classifier
            MLPClassifier.getInstance().addLocation(location);

        }
    }


}
