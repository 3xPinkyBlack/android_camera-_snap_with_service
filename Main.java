package com.pinky.tuts;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.Toast;

public class Main extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Start the camera service to initiate the camera in the background
        startService(new Intent(this, camera_service.class));
        //close the activity to make sure we are doing the snap in the background
        finish();
    }
}