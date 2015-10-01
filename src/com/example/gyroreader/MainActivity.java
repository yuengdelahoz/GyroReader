package com.example.gyroreader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements SensorEventListener {
	private BufferedWriter output;
	private SensorManager SM;
	private String gyro = "blu blu blu blu";
	private String Accel = "blablablabla";
	TextView x, y, z;
	private File file;
	private Button msavebtn;
	private Button btn;
	private int sentinel;
	private Thread ab = null;
	private com.example.gyroreader.SensorManager sensors;
	private AsyncTask logInBackground;
	private static final String TAG = "MainActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SM = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensors = com.example.gyroreader.SensorManager.getInstance(this);

		RegisterSensors();
		Toast.makeText(getApplicationContext(), "Creating File",
				Toast.LENGTH_SHORT).show();
		String dirName = "GYRO";

		Date date = new Date();
		File dir = getAlbumStorageDir(dirName);
		file = new File(dir, "Readings_" + date.getTime() + ".csv");
		setContentView(R.layout.activity_main);

	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		x = (TextView) findViewById(R.id.gyrox);
		y = (TextView) findViewById(R.id.gyroy);
		z = (TextView) findViewById(R.id.gyroz);

		btn = (Button) findViewById(R.id.button1);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void mSave(View view) {

		if (!sensors.isAzimuthReady()) {
			Log.e("BLU", "gohaogfhoafgha");
			try {
				sensors.initYeiSensor();
				sensors.setAzimuthReady(true);
				btn.setText("Disconnect");
				Log.e("", "Connected to YEI Sensor");
				if (logInBackground != null)
					logInBackground = new LongOperation().execute("");
			
			
			} catch (Exception e) {
			
			}
		} else {
				sensors.closeYEI();
				logInBackground.cancel(false);
				btn.setText("Connect");
			}
		}

//		Toast.makeText(getApplicationContext(), "Creating File",
//				Toast.LENGTH_SHORT).show();
//
//		sentinel = 0;
//		if (btn.getText().equals("Save Data")) {
//			ab = new Thread(new Runnable() {
//
//				@Override
//				public void run() {
//					// TODO Auto-generated method stub
//					while (sentinel == 0) {
//						try {
//							FileWriter writer = new FileWriter(file, true);
//							output = new BufferedWriter(writer);
//							output.append(gyro + "\n");
//							output.append(Accel + "\n");
//							output.close();
//						} catch (IOException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//					}
//
//				}
//			});
//			ab.start();
//			btn.setText("Stop");
//
//		} else if (btn.getText().equals("Stop")) {
//			sentinel = 1;
//			btn.setText("Save Data");
//		}

//	}

	static public File getAlbumStorageDir(String albumName) {
		// Get the directory for the user's public pictures directory.
		File file = new File(
				Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
				albumName);
		if (!file.mkdirs()) {
			Log.e("Error", "Directory not created");
		}
		return file;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		synchronized (this) {

			switch (event.sensor.getType()) {
			case Sensor.TYPE_ACCELEROMETER:
				/*
				 * Get the Measures the acceleration force in m/s2 that is
				 * applied to a device on all three physical axes (x, y, and z),
				 * including the force of gravity.
				 */
				float xA = event.values[0];
				float yA = event.values[1];
				float zA = event.values[2];
				Accel = xA + "," + yA + "," + zA;
				// x.setText(Accel);
				// Log.e("Acceleration", Accel);

				break;

			case Sensor.TYPE_GYROSCOPE:
				// Measures a device's rate of rotation in rad/s around each of
				// the three physical axes (x, y, and z)
				float xG = event.values[0];
				float yG = event.values[1];
				float zG = event.values[2];
				gyro = xG + "," + yG + "," + zG;
				// Log.e("Gyroscope", gyro);

				break;

			}
		}
	}

	public void RegisterSensors() {
		// Register sensors listeners
		SM.registerListener(this,
				SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_NORMAL);
		SM.registerListener(this, SM.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
				SensorManager.SENSOR_DELAY_NORMAL);

	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		try {
			output.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		super.onDestroy();

	}

	private class LongOperation extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			while (!isCancelled()) {
				try {
					Thread.sleep(100);
					double[] heading = sensors.getImmediateAzimuth();
					// heading -= azimuthZero;
					if (heading[1] < 0.0)
						heading[1] += 2 * Math.PI;
					// heading[1] += Math.PI;
					Log.v(TAG, "COMPASS:	" + Double.toString(heading[1])
							+ "\t[0]: " + Double.toString(heading[0])
							+ "\t[2]: " + Double.toString(heading[2]));
				} catch (InterruptedException e) {
					Log.i(TAG, "LogInBackground interrupted");
				}
			}
			return "";
		}

	}
}
