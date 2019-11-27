package com.ndzl.z_gravity_service;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    SeekBar sbarGravity;
    TextView tvSeek;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = getSharedPreferences("GRAVITY_CONFIG", MODE_PRIVATE);



        tvSeek = findViewById(R.id.idtvSeek);
        sbarGravity = findViewById(R.id.idSeekGravityThreshold);
        sbarGravity.setProgress( prefs.getInt("ANGLE", 5) );
        tvSeek.setText(""+sbarGravity.getProgress());
        sbarGravity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                                   @Override
                                                   public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                                                        tvSeek.setText(""+i);
                                                   }

                                                   @Override
                                                   public void onStartTrackingTouch(SeekBar seekBar) {

                                                   }

                                                   @Override
                                                   public void onStopTrackingTouch(SeekBar seekBar) {

                                                   }
                                               }
        );

        startService(new Intent(this, GravityService.class));
    }

    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences.Editor shpr = getSharedPreferences("GRAVITY_CONFIG", MODE_PRIVATE).edit();
        shpr.putInt("ANGLE", sbarGravity.getProgress());
        shpr.apply();
    }


}
