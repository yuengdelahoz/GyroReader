package com.example.gyroreader;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;



import java.io.IOException;
import java.io.InvalidObjectException;

/**
 * This class holds all the ugly code.
 *
 */
public class SensorManager {
	
	private static final String TAG = "SensorManager";
	private static SensorManager instance = null;
	private YeiSensor yeiSensor;
	private Activity parent;
	private float[] orientation;
	public static boolean antConnected = false;

    //
	
	/**
	 * Singleton
	 */
	protected SensorManager(Activity parent) {
		this.parent = parent;

	}
	
	public void initYeiSensor() throws Exception {
		yeiSensor = YeiSensor.getInstance();
		Thread.sleep(500);		// Just because
		yeiSensor.setTareCurrentOrient();
	}
	

	
	public void pollAzimuth() {
        orientation = yeiSensor.getFiltTaredEulerAngles();
        double pitch = orientation[0];
        double yaw = orientation[1];
        double roll = orientation[2];
        double[] angles = new double[3];
        angles[0] = pitch;
        angles[1] = yaw;
        angles[2] = roll;
//		orientation = yeiSensor.getFiltTaredOrientMat();
//		double[] heading = computeHeadings(orientation);
//		headingCallback(angles[1], angles[0]);
//		azimuthCallback(computeHeadings(orientation));
	}
	
	public double[] getImmediateAzimuth() {
        orientation = yeiSensor.getFiltTaredEulerAngles();
        double pitch = orientation[0];
        double yaw = orientation[1];
        double roll = orientation[2];
        double[] angles = new double[3];
        angles[0] = pitch;
        angles[1] = yaw;
        angles[2] = roll;
        return angles;
//		orientation = yeiSensor.getFiltTaredOrientMat();
//		return computeHeadings(orientation);
	}
	

	
	/**
	 * Singleton getter
	 * @return instance
	 */
	public static SensorManager getInstance(Activity parent) {
		if (instance == null)
			instance = new SensorManager(parent);
		return instance;
	}

    public static SensorManager getInstance() {
        return instance;
    }
	
//	public void setListener(SensorListener listener) {
//		this.listener = listener;
//	}
	
	public void zero() throws InvalidObjectException{
//		yeiSensor.setAxisDirections("YXZ", false, false, false);
        if(yeiSensor != null) {
            yeiSensor.setTareCurrentOrient();
            double[] zeros = getImmediateAzimuth();
            azimuthZero = zeros[0];
            inclineZero = zeros[1];
        } else {
            throw new InvalidObjectException("YEI Sensor not initialized");
        }
	}
	
	public void stop() throws IOException {
//        antSensor.toggleConnection();
//        antSensor.onPause();
		yeiSensor.close();
//		TMAWriter.stop();
        recording = false;
        azimuthReady = false;
	}

    public void closeYEI() {
        yeiSensor.close();
        azimuthReady = false;
    }
	
	// Doing things
	private boolean recordToFile = false, debugSensors = false;
	private boolean azimuthReady = false, speedReady = false;
	private boolean azimuthDataReady = false, speedDataReady = false, recording = false;
	
	private double azimuthZero = 0.0, inclineZero = 0.0;
	private double distance = 0.0;
	private double incline = 0.0;
	private double deltaDist = 0.0;
	private double heading = 0.0;
	
	public void setAzimuthReady(boolean ready) {	azimuthReady = ready;	}

	public void setDebugSensors(boolean debug) {	debugSensors = debug;	}
	public boolean isRecording() {	return recording;	}
	public boolean isAzimuthReady() {	return azimuthReady;	}
	public boolean isSpeedReady() {	return speedReady;	}
	public double getDistance() {	return distance;	}
	




    BroadcastReceiver blueReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String name;
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null) {
                name = device.getName();
            } else {
                name = "null";
            }
            Log.i(TAG, "Bluetooth device: " + device.getName());
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                Log.i(TAG, "Bluetooth connected: " + device);
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                Log.w(TAG, "Bluetooth disconnected: " + device);
                if(name.contains("YEI_3SpaceBT")) {
                    // We got a problem - stop everything
                    Toast.makeText(context, "Compass disconnected", Toast.LENGTH_SHORT).show();
                    if(isRecording()) {
//                        MainActivity.btnRecord.performClick();
                    } else {
                        try {
                            stop();
                        } catch (IOException e) {
                            Log.w(TAG, "Problem disconnecting compass on bluetooth disconnect broadcast");
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    };
}