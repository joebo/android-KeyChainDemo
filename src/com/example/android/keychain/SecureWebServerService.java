/*
 * Copyright 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.keychain;


import java.io.IOException;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class SecureWebServerService extends Service {

    // Log tag for this class
    private static final String TAG = "SecureWebServerService";

    // A special ID assigned to this on-going notification.
    private static final int ONGOING_NOTIFICATION = 1248;

    // A handle to the simple SSL web server
    private WebServer sws;

    public StepDetector mStepDetector;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private StepDisplayer mStepDisplayer;
    private int mSteps;
    
    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class StepBinder extends Binder {
    	SecureWebServerService getService() {
            return SecureWebServerService.this;
        }
    	public void testStep() {
    		SecureWebServerService.this.mStepDetector.signal();  
    	}
    }
    
    private StepDisplayer.Listener mStepListener = new StepDisplayer.Listener() {
        public void stepsChanged(int value) {
            mSteps = value;
            passValue();
        }
        public void passValue() {
            if (mCallback != null) {
                mCallback.stepsChanged(mSteps);
            }
        }
    };

    /**
     * Start the SSL web server and set an on-going notification
     */
    @Override
    public void onCreate() {
        super.onCreate();
        sws = new WebServer(this);
        sws.start();
   
        
        // Start detecting
        try {
			mStepDetector = new StepDetector(this);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.e(TAG, e.getMessage());
			e.printStackTrace();
		}
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        registerDetector();

        mStepDisplayer = new StepDisplayer();
        
        mStepDetector.addStepListener(mStepDisplayer);
        mStepDisplayer.addListener(mStepListener);
        
        //mStepDisplayer.setSteps(mSteps = mState.getInt("steps", 0));
        //mStepDisplayer.addListener(mStepListener);
        
		createNotification();
    }


    
    private void registerDetector() {
        mSensor = mSensorManager.getDefaultSensor(
            Sensor.TYPE_ACCELEROMETER /*| 
            Sensor.TYPE_MAGNETIC_FIELD | 
            Sensor.TYPE_ORIENTATION*/);
        mSensorManager.registerListener(mStepDetector,
            mSensor,
            SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void unregisterDetector() {
        mSensorManager.unregisterListener(mStepDetector);
    }

    /**
     * Stop the SSL web server and remove the on-going notification
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        sws.stop();
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new StepBinder();

    
    /**
     * Create an on-going notification. It will stop the server when the user
     * clicks on the notification.
     */
    private void createNotification() {
        Log.d(TAG, "Create an ongoing notification");
        Intent notificationIntent = new Intent(this,
                KeyChainDemoActivity.class);
        notificationIntent.putExtra(KeyChainDemoActivity.EXTRA_STOP_SERVER,
                true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        Notification notification = new Notification.Builder(this).
                setContentTitle(getText(R.string.notification_title)).
                setContentText(getText(R.string.notification_message)).
                setSmallIcon(android.R.drawable.ic_media_play).
                setTicker(getText(R.string.ticker_text)).
                setOngoing(true).
                setContentIntent(pendingIntent).
                getNotification();
        startForeground(ONGOING_NOTIFICATION, notification);
    }

    public interface ICallback {
        public void stepsChanged(int value);
    }
    
    private ICallback mCallback;

    public void registerCallback(ICallback cb) {
        mCallback = cb;
        //mStepDisplayer.passValue();
        //mPaceListener.passValue();
    }

}
