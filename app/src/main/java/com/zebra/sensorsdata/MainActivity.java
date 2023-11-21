package com.zebra.sensorsdata;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.zebra.sensorsdata.WiFence;

//some changes for A10 on jul31,2020

public class MainActivity extends AppCompatActivity {

    SeekBar sbarGravity;
    TextView tvSeek;
    Intent intentPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = getSharedPreferences("GRAVITY_CONFIG", MODE_PRIVATE);
        intentPref = new Intent(this, GravityService.class);

        tvSeek = findViewById(R.id.idtvSeek);
        sbarGravity = findViewById(R.id.idSeekGravityThreshold);
        int savedAngle = prefs.getInt("ANGLE", 3);
        sbarGravity.setProgress( savedAngle );
        tvSeek.setText("Scanner activation angle: "+(10*savedAngle)+"°");
        sbarGravity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                                   @Override
                                                   public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                                                        tvSeek.setText("Scanner activation angle: "+(10*i)+"°");
                                                   }

                                                   @Override
                                                   public void onStartTrackingTouch(SeekBar seekBar) {

                                                   }

                                                   @Override
                                                   public void onStopTrackingTouch(SeekBar seekBar) {
                                                       intentPref.putExtra("GRAVITY_THRESHOLD", seekBar.getProgress());
                                                       startService(intentPref);
                                                   }
                                               }
        );

        startService(new Intent(this, GravityService.class));

        new WiFence(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences.Editor shpr = getSharedPreferences("GRAVITY_CONFIG", MODE_PRIVATE).edit();
        shpr.putInt("ANGLE", sbarGravity.getProgress());
        shpr.apply();
    }


}