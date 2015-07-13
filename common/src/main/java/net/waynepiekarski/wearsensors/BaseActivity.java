package net.waynepiekarski.wearsensors;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

public class BaseActivity extends Activity {

    private SensorManager sensorManager;
    private Sensor sensor;
    private SensorEventListener listener;
    private Sensor[] sensorArray;
    private int sensorIndex;
    private TextView viewSensorType;
    private TextView viewSensorDetails;
    private TextView viewSensorAccuracy;
    private TextView viewSensorRaw;
    private BarView[] viewBarArray;
    private LinearLayout viewSensorBarLayout;
    private Button viewSensorNext;
    private Button viewSensorPrev;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewSensorType = (TextView)findViewById(R.id.sensorType);
        viewSensorDetails = (TextView)findViewById(R.id.sensorDetails);
        viewSensorAccuracy = (TextView)findViewById(R.id.sensorAccuracy);
        viewSensorRaw = (TextView)findViewById(R.id.sensorRaw);
        viewSensorBarLayout = (LinearLayout)findViewById(R.id.sensorBarLayout);
        viewSensorNext = (Button)findViewById(R.id.sensorNext);
        viewSensorPrev = (Button)findViewById(R.id.sensorPrev);

        viewBarArray = new BarView[6];
        for (int i = 0; i < viewBarArray.length; i++) {
            viewBarArray[i] = new BarView(this, null);
            viewSensorBarLayout.addView(viewBarArray[i]);
        }

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        List<Sensor> list = sensorManager.getSensorList(Sensor.TYPE_ALL);
        if (list.size() < 1)
            Logging.fatal("No sensors returned from getSensorList");
        sensorArray = list.toArray(new Sensor[list.size()]);
        for (int i = 0; i < sensorArray.length; i++) {
            Logging.debug("Found sensor " + i + " " + sensorArray[i].toString());
        }
        sensorIndex = 0;
        sensor = sensorArray[sensorIndex];

        // Implement the ability to cycle through the sensor list
        viewSensorNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sensorIndex++;
                if (sensorIndex >= sensorArray.length)
                    sensorIndex = 0;
                sensor = sensorArray[sensorIndex];
                stopSensor();
                startSensor();
            }
        });
        viewSensorPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sensorIndex--;
                if (sensorIndex < 0)
                    sensorIndex = sensorArray.length-1;
                sensor = sensorArray[sensorIndex];
                stopSensor();
                startSensor();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        startSensor();
    }

    private void startSensor() {
        String type = "Sensor #" + (sensorIndex+1) + ", type " + sensor.getType();
        if (Build.VERSION.SDK_INT >= 20)
            type = type + ", " + sensor.getStringType();
        Logging.debug("Opened up " + type);
        viewSensorType.setText(type);
        viewSensorDetails.setText(sensor.toString());
        for (int i = 0; i < viewBarArray.length; i++) {
            viewBarArray[i].setMaximum(sensor.getMaximumRange());
        }
        viewSensorRaw.setText("n/a");
        viewSensorAccuracy.setText("n/a");

        listener = new SensorEventListener() {
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (sensorEvent.sensor.getType() == sensor.getType()) {
                    Logging.detailed("Sensor update: " + Arrays.toString(sensorEvent.values));
                    viewSensorRaw.setText("Sensor=" + Arrays.toString(sensorEvent.values));
                    if (sensorEvent.values.length > viewBarArray.length)
                        Logging.fatal("Sensor update contained " + sensorEvent.values.length + " which is larger than expected " + viewBarArray.length);
                    for (int i = 0; i < sensorEvent.values.length; i++) {
                        viewBarArray[i].setValue(sensorEvent.values[i]);
                    }
                    for (int i = sensorEvent.values.length; i < viewBarArray.length; i++) {
                        viewBarArray[i].setValue(0);
                    }
                }
            }

            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                Logging.detailed("Accuracy update: " + accuracy);
                viewSensorAccuracy.setText("Accuracy=" + accuracy);
            }
        };
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onStop() {
        super.onStop();
        stopSensor();
    }

    private void stopSensor() {
        sensorManager.unregisterListener(listener);
    }
}