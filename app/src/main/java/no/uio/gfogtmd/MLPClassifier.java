package no.uio.gfogtmd;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MLPClassifier {
   private static String TAG = MLPClassifier.class.getSimpleName();
   private float accX, accY, accZ, magX, magY, magZ, accMagnitude, magMagnitude, lat, lon, acc;
   private static MLPClassifier instance;
   private static String modelName = "model_GFogTMD.tflite";
   private static Interpreter tflite;

   private static Context context;
   private static boolean modelLoaded = false;




   private MLPClassifier() {
      loadModelAsync();
      initializeValues();
   }

   static MLPClassifier getInstance() {
      if (instance == null) {
         instance = new MLPClassifier();
      }
      return instance;
   }

   static void setContext(Context ctx) {
      context =ctx;
   }
   public void addAcceleration(float accX, float accY, float accZ, float accMagnitude) {
      this.accX = accX;
      this.accY = accY;
      this.accZ = accZ;
      this.accMagnitude = accMagnitude;
   }

   public void addMagnetic(float magX, float magY, float magZ, float magMagnitude) {
      this.magX = magX;
      this.magY = magY;
      this.magZ = magZ;
      this.magMagnitude = magMagnitude;
   }

   public void addLocation(Location location) {
      this.lat = (float) location.getLatitude();
      this.lon = (float) location.getLongitude();
      this.acc = location.getAccuracy();
   }



   int classify() {
      if (!modelLoaded) {
         loadModel();
         createInterpreter();
      }
      Log.d(TAG, "Classifying...");

      // Measure the start time for classification.
      long startTime = System.nanoTime();
      float[] listOfProbabilities;
      if (tflite != null) { // Check if the interpreter is not null
         listOfProbabilities = doInference(calculateFeatures());

      } else {
         Log.e(TAG, "TFLite interpreter is null. Ensure it is properly initialized.");
         return 0; // Handle the case when the interpreter is not initialized
      }

      // Measure the end time for classification.
      long endTime = System.nanoTime();
      // Calculate and log the classification time in milliseconds.
      long classificationTime = (endTime - startTime) / 1_000_000; // Convert to milliseconds
      Log.d(TAG, "Classification took " + classificationTime + " ms");

      int transportModeId = convertProbabilitiesToMode(listOfProbabilities);

      return transportModeId;
   }

   static void loadModelAsync() {
      if (modelLoaded) {
         return;
      }
      // Load the model asynchronously
      try {
         AsyncTask.execute(() -> {
            try {
               createInterpreter();
               modelLoaded = true;
               Log.d(TAG, "Model loaded successfully");
            } catch (Exception e) {
               Log.e(TAG, "Failed loading model: " + e.getMessage());
               e.printStackTrace();
            }
         });
      } catch (Exception e) {
         Log.e(TAG, "Failed to load model asynchronously: " + e.getMessage());
         throw new RuntimeException(e);
      }
   }

   static void loadModel() {
      if (modelLoaded) {
         return;
      }
      try {
         createInterpreter();
         modelLoaded = true;
         Log.d(TAG, "Model loaded successfully");
      } catch (Exception e) {
         Log.e(TAG, "Failed loading model: " + e.getMessage());
         e.printStackTrace();
      }
   }





   private float[] calculateFeatures() {
      float[] features = {accX, accY, accZ, accMagnitude, magX, magY, magZ, magMagnitude, lat, lon, acc};
      return features;
   }



   private static void createInterpreter() {
      Log.d(TAG, "Creating TFLite interpreter..."); // Add this log message
      try {
         tflite = new Interpreter(loadModelFile());
      } catch (IOException ex) {
         Log.e(TAG, "Failed to create the TFLite interpreter: " + ex.getMessage());
         ex.printStackTrace();
      }
   }

   private static MappedByteBuffer loadModelFile() throws IOException {

      AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelName);
      FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
      FileChannel fileChannel = inputStream.getChannel();
      long startOffset = fileDescriptor.getStartOffset();
      long declaredLength = fileDescriptor.getDeclaredLength();
      return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
   }

   private float[] doInference(float[] features) {
      float[][] outputs = new float[1][9];
      float [] output = new float[9];
      tflite.run(features, outputs);
      for (int i=0 ; i <9 ; i++){
         output[i] = outputs[0][i];
      }
      return output;
   }

   private void initializeValues() {
      // You can add any necessary initialization here.


   }
   public int convertProbabilitiesToMode(float[] probabilities) {
      int maxIndex = 200;
      float maxProbability = 0;

      for (int i = 0; i < probabilities.length; i++) {
         if (probabilities[i] > maxProbability) {
            maxProbability = probabilities[i];
            maxIndex = i;
         }
      }
      return maxIndex;
   }


}
