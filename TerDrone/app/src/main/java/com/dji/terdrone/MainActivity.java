package com.dji.terdrone;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.TextureView.SurfaceTextureListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.dji.terdrone.DemoApplication;
import com.google.android.gms.maps.OnMapReadyCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMissionGotoWaypointMode;
import dji.common.product.Model;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.KeyManager;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.Compass;
import dji.sdk.useraccount.UserAccountManager;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;

import static dji.keysdk.FlightControllerKey.AIRCRAFT_LOCATION_LATITUDE;
import static dji.keysdk.FlightControllerKey.AIRCRAFT_LOCATION_LONGITUDE;

public class MainActivity extends Activity implements SurfaceTextureListener, View.OnClickListener {

    private static final String TAG = MainActivity.class.getName();
    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;

    // Codec for video live view
    protected DJICodecManager mCodecManager = null;

    protected TextureView mVideoSurface = null;
    private Button mTakeOff, mLanding, mWaypointMissionBtn, mStopBtn;

    private Handler handler;

    public static WaypointMission.Builder waypointMissionBuilder;
    private WaypointMissionOperator instance;
    private WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
    private WaypointMissionHeadingMode mHeadingMode = WaypointMissionHeadingMode.AUTO;
    private WaypointMission mission;
    private FlightController mFlightController;
    private Compass mCompass;
    private EditText Distance, NBPoints;
    private double droneLocationLat = 181, droneLocationLng = 181;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler();

        initUI();
        initFlightController();

        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };


    }

    protected void onProductChange() {
        initPreviewer();
        loginAccount();
    }

    private void loginAccount(){

        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        Log.e(TAG, "Login Success");
                    }
                    @Override
                    public void onFailure(DJIError error) {
                        showToast("Login Error:"
                                + error.getDescription());
                    }
                });
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        initPreviewer();
        onProductChange();

        if(mVideoSurface == null) {
            Log.e(TAG, "mVideoSurface is null");
        }
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        uninitPreviewer();
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }

    public void onReturn(View view){
        Log.e(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        uninitPreviewer();
        super.onDestroy();
    }

    private void initUI() {
        // init mVideoSurface
        mVideoSurface = (TextureView)findViewById(R.id.video_previewer_surface);

        mTakeOff = (Button) findViewById(R.id.btn_takeoff);
        mLanding = (Button) findViewById(R.id.btn_landing);
        mWaypointMissionBtn = (Button) findViewById(R.id.btn_waypoint_mission);
        mStopBtn = (Button) findViewById(R.id.btn_stop);

        Distance = (EditText) findViewById(R.id.Distance);
        NBPoints = (EditText) findViewById(R.id.NbPoints);
        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }

        mTakeOff.setOnClickListener(this);
        mLanding.setOnClickListener(this);
        mWaypointMissionBtn.setOnClickListener(this);
        mStopBtn.setOnClickListener(this);

    }

    private void initFlightController() {

        BaseProduct product = DemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                mFlightController = ((Aircraft) product).getFlightController();
            }
        }

        if (mFlightController != null) {
            mFlightController.setStateCallback(new FlightControllerState.Callback() {

                @Override
                public void onUpdate(FlightControllerState djiFlightControllerCurrentState) {
                    droneLocationLat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                    droneLocationLng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();;
                }
            });
        }

    }
    private void initPreviewer() {

        BaseProduct product = DemoApplication.getProductInstance();

        if (product == null || !product.isConnected()) {
            showToast(getString(R.string.disconnected));
        } else {
            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(this);
            }
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener);
            }
        }
    }

    private void uninitPreviewer() {
        Camera camera = DemoApplication.getCameraInstance();
        if (camera != null){
            // Reset the callback
            VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(null);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureAvailable");
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e(TAG,"onSurfaceTextureDestroyed");
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }

        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btn_takeoff:{
                showToast("Take Off");
              if (mFlightController != null){
                  mFlightController.turnOnMotors(new CommonCallbacks.CompletionCallback() {
                      @Override
                      public void onResult(DJIError djiError) {
                          if (djiError != null) {
                              showToast(djiError.getDescription());
                          } else {
                              showToast("Turn On Motors Success");
                          }
                      }
                  });
                    mFlightController.startTakeoff(
                            new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError != null) {
                                        showToast(djiError.getDescription());
                                    } else {
                                        showToast("Take off Success");
                                    }
                                }
                            }
                    );
                }
                break;
            }

            case R.id.btn_landing:{
                showToast("Landing");
                if (mFlightController != null) {

                    mFlightController.startLanding(
                            new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError != null) {
                                        showToast(djiError.getDescription());
                                    } else {
                                        showToast("Start Landing");
                                    }
                                }
                            }
                    );
                }

                break;
            }

            case R.id.btn_waypoint_mission:{
                showToast("Waypoint Mission");
                //startWaypointMission();

                break;
            }

            case R.id.btn_stop:{
                showToast("Stop");
                //
                mFlightController.turnOffMotors(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            showToast(djiError.getDescription());
                        } else {
                            showToast("Turn Off Motors Sucess");
                        }
                    }
                });

                break;
            }
            default:
                break;
        }
    }

    // Method for taking photo
    private void captureAction(){

        final Camera camera = DemoApplication.getCameraInstance();
        if (camera != null) {

            SettingsDefinitions.ShootPhotoMode photoMode = SettingsDefinitions.ShootPhotoMode.SINGLE; // Set the camera capture mode as Single mode
            camera.setShootPhotoMode(photoMode, new CommonCallbacks.CompletionCallback(){
                @Override
                public void onResult(DJIError djiError) {
                    if (null == djiError) {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                camera.startShootPhoto(new CommonCallbacks.CompletionCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {
                                        if (djiError == null) {
                                            showToast("take photo: success");
                                        } else {
                                            showToast(djiError.getDescription());
                                        }
                                    }
                                });
                            }
                        }, 2000);
                    }
                }
            });
        }
    }

    // Method for starting recording
    private void startRecord(){

        final Camera camera = DemoApplication.getCameraInstance();
        if (camera != null) {
            camera.startRecordVideo(new CommonCallbacks.CompletionCallback(){
                @Override
                public void onResult(DJIError djiError)
                {
                    if (djiError == null) {
                        showToast("Record video: success");
                    }else {
                        showToast(djiError.getDescription());
                    }
                }
            }); // Execute the startRecordVideo API
        }
    }

    // Method for stopping recording
    private void stopRecord(){

        Camera camera = DemoApplication.getCameraInstance();
        if (camera != null) {
            camera.stopRecordVideo(new CommonCallbacks.CompletionCallback(){

                @Override
                public void onResult(DJIError djiError)
                {
                    if(djiError == null) {
                        showToast("Stop recording: success");
                    }else {
                        showToast(djiError.getDescription());
                    }
                }
            }); // Execute the stopRecordVideo API
        }

    }

    public WaypointMissionOperator getWaypointMissionOperator() {
        if (instance == null) {
            if (DJISDKManager.getInstance().getMissionControl() != null){
                instance = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
            }
        }
        return instance;
    }

    /* Fonction Personelle */
    private double radToDeg(double r){
        return r*180/Math.PI;
    }
    private double degToRad(double d){
        return d*Math.PI/180;
    }
    private double meterToRad(double m){
        return ((1/60)/(180/Math.PI))*(m/1852);
    }
    private double RealMod(double val,double modVal){
        double res = Math.IEEEremainder(val,modVal);
        if(res<0)res+=modVal;
        return res;
    }

    private Waypoint getNewPoint(Waypoint p1, double az,double raddist){
        float altitude= p1.altitude;
        double latitude =Math.asin(Math.sin(p1.coordinate.getLatitude())*Math.cos(raddist)+Math.cos(p1.coordinate.getLatitude())*Math.cos(raddist));
        double longitude=RealMod(p1.coordinate.getLongitude()-(Math.atan2(Math.sin(az)*Math.sin(raddist)*Math.cos(p1.coordinate.getLatitude()),Math.cos(raddist)-Math.sin(p1.coordinate.getLatitude())*Math.sin(latitude))+Math.PI),Math.PI/2 );
        return new Waypoint(radToDeg(latitude),radToDeg(longitude),altitude);
    }
    private WaypointMission createWaypointMission() {
        LocationCoordinate3D dronePosition = mFlightController.getState().getAircraftLocation();
        double longitude = dronePosition.getLongitude();
        double latitude = dronePosition.getLatitude();
        float altitude = dronePosition.getAltitude();


        // A changer pour le parametre a saisir dans l'app
        int numberOfWaypoint = 20;
        // A changer pour le parametre a saisir dans l'app
        double rayon = 5;
        //Position actuelle du drone (point de départ)
        Waypoint drone=new Waypoint(latitude,longitude,altitude);
        //orientation du drone
        double a = mFlightController.getCompass().getHeading();
        if(a<0) a = 180 + (180 + a);
        a=degToRad(a);
        //Position de l'objet
        Waypoint centre = getNewPoint(drone, a,meterToRad(rayon));

        WaypointMission.Builder builder = new WaypointMission.Builder();

        final float baseAltitude = 20.0f;
        builder.autoFlightSpeed(5f);
        builder.maxFlightSpeed(10f);
        builder.setExitMissionOnRCSignalLostEnabled(false);
        builder.finishedAction(WaypointMissionFinishedAction.NO_ACTION);
        builder.flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        builder.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY);
        builder.headingMode(WaypointMissionHeadingMode.AUTO);
        builder.repeatTimes(1);

        List<Waypoint> waypointList = new ArrayList<>();

        List<Double> rads=new ArrayList<>();
        double angle=(2*Math.PI/numberOfWaypoint);
        a = a + angle + Math.PI;

        for (int i = 0; i < numberOfWaypoint; i++) {
            //Création du ième point de passage
            Waypoint newWaypoint = getNewPoint(centre,a,meterToRad(rayon));
            newWaypoint.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, (int)radToDeg(angle-Math.PI)));
            //eachWaypoint.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, (int)radToDeg(angle-Math.PI)));
            newWaypoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 1));
            waypointList.add(newWaypoint);
        }

        builder.waypointList(waypointList).waypointCount(waypointList.size());
        return builder.build();
    }

    private void startWaypointMission(){
        getWaypointMissionOperator().startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                showToast("Mission Start: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });
    }

    private void stopWaypointMission(){

        getWaypointMissionOperator().stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                showToast("Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });
    }


}
