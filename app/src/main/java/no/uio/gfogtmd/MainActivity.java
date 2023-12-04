package no.uio.gfogtmd;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;


public class MainActivity extends AppCompatActivity {
    private static String TAG = "MainActivity";
    public static DatabaseHelper rawDataDB;
    Uri dbFilePath;
    private BatteryMonitor batteryMonitor;
    private Button btnLinkYourAcc;
    private TextView authFeedback;
    private EditText email, password;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseUser mCurrentUser;



    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //monitor battery status
        batteryMonitor = new BatteryMonitor();
        registerReceiver(batteryMonitor,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //creating the local database
        rawDataDB = new DatabaseHelper(this);
        //todo: debug purposes
        //testDropTable();
        //testUpgradeTable();

        //local db path to Uri
        File dbFile = new File( "//data/data//"+getPackageName()+"//databases//"+rawDataDB.getDatabaseName());
        dbFilePath = Uri.fromFile(dbFile);

        btnLinkYourAcc = findViewById(R.id.btnLinkYourAccount);
        authFeedback = findViewById(R.id.txtFeedback);
        email = findViewById(R.id.editTxtEmailAddress);
        password = findViewById(R.id.editTxtPassword);
        progressBar = findViewById(R.id.progressBar);
        mAuth = FirebaseAuth.getInstance();


        if (mCurrentUser == null){
            progressBar.setVisibility(View.VISIBLE);
            mAuth.signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    authFeedback.setVisibility(View.VISIBLE);
                    if (task.isSuccessful()){
                        authFeedback.setText("Signed in Anonymously");
                        Toast.makeText(MainActivity.this , R.string.txtDescription , Toast.LENGTH_LONG).show();


                    }
                    else {
                        authFeedback.setText("There is an error signing in");
                    }
                    progressBar.setVisibility(View.INVISIBLE);
                }
            });
        }
        linkUserAccount();
        locationStatusCheck();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        mCurrentUser = mAuth.getCurrentUser();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        switch (id){
        case R.id.action_uploadDB:
            //create the intent and pass the user input(tripId)
            Intent intent = new Intent(MainActivity.this, UploadDb.class);
            intent.putExtra("db-Uri", dbFilePath.toString());
            startActivity(intent);
            break;
        case  R.id.action_showMyTrips:
            Intent intent2 = new Intent(MainActivity.this , DetectionActivity.class);
            startActivity(intent2);
            break;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void linkUserAccount(){
        btnLinkYourAcc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String emailText = email.getText().toString();
                String passwordText = password.getText().toString();
                if (mCurrentUser != null){
                    if (!emailText.isEmpty() || !passwordText.isEmpty()){
                        progressBar.setVisibility(View.VISIBLE);
                        AuthCredential credential = EmailAuthProvider.getCredential(emailText, passwordText);
                        mCurrentUser.linkWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                authFeedback.setVisibility(View.VISIBLE);
                                if (task.isSuccessful()){
                                    authFeedback.setText("New account linked!");
                                }
                                else {
                                    authFeedback.setText("There is an error signing in!");
                                }
                            }
                        });

                    }
                    else {
                        authFeedback.setText("Enter an email and password!");
                    }
                }
                else {
                    authFeedback.setText("No user account is available to be linked!");

                }
                progressBar.setVisibility(View.INVISIBLE);

            }
        });
    }



    public void locationStatusCheck() {
        final LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();

        }
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                        Toast.makeText(MainActivity.this, "Your data is not valid without GPS!", Toast.LENGTH_LONG).show();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    //drop table (for testing purposes)
    public void testDropTable(){
        rawDataDB.deleterows();
    }
    //upgrade the schema of the database (for testing purposes)
    public void testUpgradeTable(){
        rawDataDB.upgrade();
    }


}