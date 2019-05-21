package com.dji.terdrone;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.TextureView.SurfaceTextureListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMissionGotoWaypointMode;
import dji.common.product.Model;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.Compass;
import dji.sdk.useraccount.UserAccountManager;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class MainActivity extends Activity implements SurfaceTextureListener, View.OnClickListener {

    private static final double ONE_METER_OFFSET = 0.00000899322;

    private static final String TAG = MainActivity.class.getName();
    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;

    // Codec for video live view
    protected DJICodecManager mCodecManager = null;

    protected TextureView mVideoSurface = null;
    private Button mTakeOff, mLanding, mWaypointMissionBtn, mStopBtn, mLoadButton;
    private ToggleButton mTypeMission;
    private Handler handler;

    private FlightController mFlightController;

    private WaypointMissionOperator instance;
    private WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.GO_FIRST_WAYPOINT;
    private WaypointMissionHeadingMode mHeadingMode = WaypointMissionHeadingMode.AUTO;

    private EditText Btn_Rayon, Btn_NBPoints, Btn_Altitude, Btn_NBRotattion;
    private double droneLocationLat = 181, droneLocationLng = 181;

    private boolean mission = false;
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
        mVideoSurface = findViewById(R.id.video_previewer_surface);

        mTakeOff = findViewById(R.id.btn_takeoff);
        mLanding = findViewById(R.id.btn_landing);
        mWaypointMissionBtn =  findViewById(R.id.btn_waypoint_mission);
        mStopBtn = findViewById(R.id.btn_stop);
        mLoadButton = findViewById(R.id.btn_loadMission);
        mTypeMission = findViewById(R.id.type_mission);

        Btn_Rayon = findViewById(R.id.Rayon);
        Btn_NBPoints = findViewById(R.id.NbPoints);
        Btn_Altitude = findViewById(R.id.altitude);
        Btn_NBRotattion = findViewById(R.id.nbTour);

        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }

        mTakeOff.setOnClickListener(this);
        mLanding.setOnClickListener(this);
        mWaypointMissionBtn.setOnClickListener(this);
        mStopBtn.setOnClickListener(this);
        mLoadButton.setOnClickListener(this);

        mTypeMission.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mission = true;
                    showToast("Manual");
                } else {
                    mission = false;
                    showToast("Auto");
                }
            }
        });
    }

    private void initFlightController() {

        BaseProduct product = DemoApplication.getProductInstance();
        if (product != null && product.isConnected()){
                if (product instanceof Aircraft) {
                    mFlightController = ((Aircraft) product).getFlightController();
                }
        } else {
            showToast("null");
        }

        if (mFlightController != null) {
            mFlightController.setStateCallback(new FlightControllerState.Callback() {

                @Override
                public void onUpdate(FlightControllerState djiFlightControllerCurrentState) {
                    droneLocationLat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                    droneLocationLng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();
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


            case R.id.btn_loadMission:{
                showToast("Load Waypoint Mission");
                if(mission) {
                    showToast("Button Mission Manual");
                    createWaypointMissionManual();
                } else {
                    showToast("Button Mission Auto");
                    createWaypointMissionAuto();
                }
                loadWaypointMission();
                break;
            }

            case R.id.btn_waypoint_mission:{
                showToast("Waypoint Mission");
                startWaypointMission();
                break;
            }

            case R.id.btn_stop:{
                showToast("Stop Waypoint Mission");
                stopWaypointMission();
                break;
            }
            default:
                break;
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
        return r*180.0/Math.PI;
    }
    private double degToRad(double d){
        return d*Math.PI/180.0;
    }
    private double meterToRad(double m){
        return ((1.0/60.0)/(180.0/Math.PI))*(m/1852.0);
    }
    private double RealMod(double val,double modVal){
        double res = Math.IEEEremainder(val,modVal);
        if(res<0)res+=modVal;
        return res;
    }

    private Waypoint getNewPoint(Waypoint p1, double az,double raddist,float alti){
        double lat=degToRad(p1.coordinate.getLatitude());
        double lng=degToRad(p1.coordinate.getLongitude());
        float altitude= p1.altitude+alti;
        double latitude =Math.asin(Math.sin(lat)*Math.cos(raddist)+Math.cos(lat)*Math.sin(raddist)*Math.cos(az));
        double longitude=RealMod(lng-(Math.atan2(Math.sin(az)*Math.sin(raddist)*Math.cos(lat),Math.cos(raddist)-Math.sin(lat)*Math.sin(latitude))+Math.PI),Math.PI/2 );
        showToast("az : " + az);
        showToast("lat : "+radToDeg(latitude) + " long : " +radToDeg(longitude) + " alt : " + altitude );
        Waypoint w = new Waypoint(radToDeg(latitude),radToDeg(longitude),altitude);

        return w;
    }

    private WaypointMission createWaypointMissionAuto() {
        LocationCoordinate3D dronePosition = mFlightController.getState().getAircraftLocation();
        double longitude = dronePosition.getLongitude();
        double latitude = dronePosition.getLatitude();
        float altitude = dronePosition.getAltitude();

        int numberOfWaypoint = Integer.parseInt(Btn_NBPoints.getText().toString());
        double rayon = Double.parseDouble(Btn_Rayon.getText().toString());
        int NbTour = Integer.parseInt(Btn_NBRotattion.getText().toString());
        float Altitude = Float.parseFloat(Btn_Altitude.getText().toString());

        //Position actuelle du drone (point de départ)
        Waypoint drone=new Waypoint(latitude,longitude,altitude);
        //orientation du drone
        double a = mFlightController.getCompass().getHeading();
        //if(a<0) a = 180 + (180 + a);
       // showToast(" a : "+ a);

        a=degToRad(a);
       // showToast(" a : "+ a);

        //Position de l'objet
        Waypoint centre = getNewPoint(drone, a,meterToRad(rayon),altitude);

        WaypointMission.Builder builder = new WaypointMission.Builder();

        //final float baseAltitude = 20.0f;
        builder.autoFlightSpeed(5f);
        builder.maxFlightSpeed(10f);
        builder.setExitMissionOnRCSignalLostEnabled(false);
        builder.finishedAction(WaypointMissionFinishedAction.NO_ACTION);
        builder.flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        builder.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY);
        builder.headingMode(WaypointMissionHeadingMode.AUTO);
        builder.repeatTimes(1);


//        List<Double> rads=new ArrayList<>();
       // showToast(" a : "+ a);
        double angle=(2*Math.PI/numberOfWaypoint);
        a = a + angle + Math.PI;



       for (int i = 0; i < numberOfWaypoint; i++) {
           a=a%(Math.PI*2);
            //Création du ième point de passage
            Waypoint newWaypoint = getNewPoint(centre,a*(double)NbTour,meterToRad(rayon),(i*Altitude)/numberOfWaypoint);
            newWaypoint.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, (int)radToDeg( Math.PI)));
            //eachWaypoint.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, (int)radToDeg(angle-Math.PI)));
            newWaypoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO,1));
           a=a+angle;
            builder.addWaypoint(newWaypoint);
        }

        return builder.build();
    }

    private WaypointMission createWaypointMissionManual(){
       LocationCoordinate3D dronePosition = mFlightController.getState().getAircraftLocation();
        double longitude = dronePosition.getLongitude();
        double latitude = dronePosition.getLatitude();
        float altitude = dronePosition.getAltitude();


        //Position actuelle du drone (point de départ)

        WaypointMission.Builder builder = new WaypointMission.Builder();

        //final float baseAltitude = 20.0f;
        builder.autoFlightSpeed(5f);
        builder.maxFlightSpeed(10f);
        builder.setExitMissionOnRCSignalLostEnabled(false);
        builder.finishedAction(mFinishedAction);
        builder.flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        builder.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY);
        builder.headingMode(mHeadingMode);
        builder.repeatTimes(1);

        Waypoint w1 = new Waypoint(latitude, longitude + ONE_METER_OFFSET, 1);
        Waypoint w2 = new Waypoint(latitude + ONE_METER_OFFSET,longitude + ONE_METER_OFFSET, 1);

        List<Waypoint> waypointList = new ArrayList<>();

        waypointList.add(w1);
        waypointList.add(w2);
        builder.waypointList(waypointList).waypointCount(waypointList.size());

        return builder.build();
    }

    private void loadWaypointMission(){
        DJIError error = getWaypointMissionOperator().loadMission(createWaypointMissionManual());
        if (error == null) {
            showToast("loadWaypoint succeeded");
        } else {
            showToast("loadWaypoint failed " + error.getDescription());
        }

        getWaypointMissionOperator().uploadMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    showToast("Mission upload successfully!");
                } else {
                    showToast("Mission upload failed, error: " + error.getDescription() + " retrying...");
                    getWaypointMissionOperator().retryUploadMission(null);
                }
            }
        });
    }
    private void startWaypointMission(){
        mFlightController.setHomeLocationUsingAircraftCurrentLocation(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                showToast("Set Home Location : " + (error == null ? "Successfully" : error.getDescription()));
            }
        });
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
