package com.vfdev.android.magic8ball;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

public class MainActivity extends Activity implements SensorEventListener {

    private static final String TAG="com.vfdev.android.magic8ball.MainActivity";

    private Random mRandomizer = new Random();
    private static final int RAND_INT_MAX = 20;

    private TextView mPrediction;
    private static final int FADEIN_DURATION= 500;
    private static final int PREDICTION_DURATION= 5000;
    private static final int FADEOUT_DURATION= 500;

    /** Countdown timer to count the time needed before hiding the prediction */
    private CountDownTimer mTimer = new CountDownTimer(FADEIN_DURATION + PREDICTION_DURATION, 1000) {

        public void onTick(long millisUntilFinished) {
            // Empty
        }

        public void onFinish() {
            hidePrediction();
        }
    };

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private float mSensorZ = -12345;
    private float[] mRotationMatrix = new float[16];
    private static final int UPSIDE_DOWN_MIN_COUNT=15;
    private int mUpsideDownCounter=0;


    private Vibrator mVibrator;
    private static final int VIBRATE_TIME=250;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPrediction = (TextView) findViewById(R.id.prediction);
        mPrediction.setVisibility(TextView.INVISIBLE);

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (mVibrator == null) {
            Log.w(TAG, "No vibration service exists.");
        }

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    }

    @Override
    public void onResume() {
        super.onResume();
        activateSensorListener();
        if (mSensor == null) {
            Log.w(TAG, "No gyroscope sensor is available.");
        }
    }

    @Override
    public void onPause() {
        desactivateSensorListener();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_about) {
//            Toast.makeText(getApplicationContext(),
//                    getResources().getString(R.string.about_text),
//                    Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onAccuracyChanged(Sensor s, int acc) {
        // Empty
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
//            String s = "RM : " +
//                    String.valueOf(rotationMatrix[0]) + ", " +
//                    String.valueOf(rotationMatrix[1]) + ", " +
//                    String.valueOf(rotationMatrix[2]) + ", " +
//                    String.valueOf(rotationMatrix[3]) + ", " +
//                    String.valueOf(rotationMatrix[4]) + ", " +
//                    String.valueOf(rotationMatrix[5]) + ", " +
//                    String.valueOf(rotationMatrix[6]) + ", " +
//                    String.valueOf(rotationMatrix[7]) + ", " +
//                    String.valueOf(rotationMatrix[8]) + ", " +
//                    String.valueOf(rotationMatrix[9]) + ", " +
//                    String.valueOf(rotationMatrix[10]) + ", " +
//                    String.valueOf(rotationMatrix[11]) + ", " +
//                    String.valueOf(rotationMatrix[12]) + ", " +
//                    String.valueOf(rotationMatrix[13]) + ", " +
//                    String.valueOf(rotationMatrix[14]) + ", " +
//                    String.valueOf(rotationMatrix[15])
//                    ";";
//            Log.i(TAG, s);

            if (isReadyToPredict(mRotationMatrix[10])) {
                startPrediction();
            }
        }
    }

    /**
     * Method to test if user is accomplished turn upside-down move of the device
     *  // and turned back
     *  Input is a value of z component of the rotation matrix <-> M(2,2) or M[10]
     * */
    protected boolean isReadyToPredict(float z) {

        // if sensorZ is not initialized :
        if (mSensorZ < -1) {
            mSensorZ = z;
            return false;
        }

        // neglect z value less than 0.7
        if (Math.abs(z) < 0.7) {
            return false;
        }

        // register the input sensor value if z is negative
        // (device is upside down) :
        if (z < 0) {
            if (mSensorZ > 0 &&
                    mUpsideDownCounter > UPSIDE_DOWN_MIN_COUNT ) {
                Log.i(TAG, "Device is set upside-down");
                if (mVibrator !=null) mVibrator.vibrate(VIBRATE_TIME);
                mSensorZ = z;
                return true;
            }
            mUpsideDownCounter++;
        }

        if (z > 0) {
            if (mSensorZ < 0){
                Log.i(TAG, "Device is back");
                // device is returned back
                // -> registered current position
                mSensorZ = z;
                mUpsideDownCounter=0;
//                return true;
                return false;
            }
        }

        return false;
    }

    /** Method to enable sensor when app is restored */
    protected void activateSensorListener() {
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    /** Method to disable sensor when app is paused */
    protected void desactivateSensorListener() {
        mSensorManager.unregisterListener(this);
    }

    /** Testing method responding on button click */
    public void onPredictTestBClicked(View view) {
        //startPrediction();
    }

    /** Method to display the prediciton with animations */
    protected void startPrediction() {
        mPrediction.setText(getNextPrediction());
        showPrediction();
        mTimer.start();
    }

    /** Method to animate prediction appearance */
    protected void showPrediction() {
        AlphaAnimation animation = new AlphaAnimation(0, 1);
//        animation.setStartOffset(0);
        animation.setDuration(FADEIN_DURATION);
        mPrediction.setVisibility(TextView.VISIBLE);
        mPrediction.startAnimation(animation);
    }

    /** Method to animate prediction disappearance */
    protected void hidePrediction() {
        AlphaAnimation animation = new AlphaAnimation(1, 0);
        animation.setDuration(FADEOUT_DURATION);
        mPrediction.setVisibility(TextView.INVISIBLE);
        mPrediction.startAnimation(animation);
    }

    /** Method to pick randomly the next prediction */
    private String getNextPrediction() {
        int i = mRandomizer.nextInt(RAND_INT_MAX);
        return getResources().getStringArray(R.array.responses)[i];
    }

}
