package com.dji.terdrone;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.TextureView.SurfaceTextureListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

public class MainActivity extends FragmentActivity implements SurfaceTextureListener, View.OnClickListener, GoogleMap.OnMapClickListener, OnMapReadyCallback {

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

    private GoogleMap gMap;
    private final Map<Integer, Marker> mMarkers = new ConcurrentHashMap<Integer, Marker>();
    private Marker droneMarker = null;
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

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
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
        //mVideoSurface = findViewById(R.id.video_previewer_surface);

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

 /*       if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }*/

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
                try {
                    loadWaypointMission();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
        /*showToast("az : " + az);
        showToast("lat : "+radToDeg(latitude) + " long : " +radToDeg(longitude) + " alt : " + altitude );*/
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
        markWaypoint(new LatLng(centre.coordinate.getLatitude(),centre.coordinate.getLongitude()));
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
        a = (a + angle + Math.PI);

        int h =(int)radToDeg(a)%360;
        h+=180;
        for (int i = 0; i < numberOfWaypoint; i++) {
            //showToast("a : "+radToDeg(a));

            //showToast("h : " + h );
            if(h>180 )   h=h-360;

            if(h<-180 )h=h+360;

            // showToast("azi : " radToDeg(a=+" - heading : "+h);
            //Création du ième point de passage
            Waypoint newWaypoint = getNewPoint(centre,a,meterToRad(rayon),(i*Altitude)/numberOfWaypoint);
            newWaypoint.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, h));
            markWaypoint(new LatLng(newWaypoint.coordinate.getLatitude(), newWaypoint.coordinate.getLongitude()));
            //eachWaypoint.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, (int)radToDeg(angle-Math.PI)));
            newWaypoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO,1));
            a=(a+angle*NbTour)%(Math.PI*2);
            h-=(int)radToDeg(angle);
            builder.addWaypoint(newWaypoint);
        }

        return builder.build();
    }

    private double distance(double latiA, double latiB, double longA, double longB) {
        Double dif = longB-longA;
        int rayon = 6378137;
        return Math.acos((Math.sin(Math.toRadians(latiA)) * Math.sin(Math.toRadians(latiB))) + (Math.cos(Math.toRadians(latiA)) * Math.cos(Math.toRadians(latiB)) * Math.cos(Math.toRadians(dif)))) * rayon;
    }

    private Waypoint getmiddle(Waypoint a,Waypoint b){

        double gLongi;
        double gLat;
        double N=(a.coordinate.getLongitude() + b.coordinate.getLongitude())/2;
        if(a.coordinate.getLongitude()+b.coordinate.getLongitude() < 180){
            gLongi = (a.coordinate.getLongitude()+b.coordinate.getLongitude())/2;
        } else {
            if(N<0){
                gLongi= 180+N;
            } else if(N>0){
                gLongi = -180+N;
            } else {
                gLongi = 180.0;
            }
        }
        gLat = (a.coordinate.getLatitude()+b.coordinate.getLatitude())/2;

        Waypoint g = new Waypoint(gLat, gLongi, 0);
        return g;
    }

    private double calculAzimut(Waypoint a, Waypoint b) {

        double x = (Math.cos(degToRad(a.coordinate.getLatitude())) * Math.sin(degToRad(b.coordinate.getLatitude()))) - ((Math.sin(degToRad(a.coordinate.getLatitude())) * Math.cos(degToRad(b.coordinate.getLatitude())) * Math.cos(degToRad(b.coordinate.getLongitude()) - degToRad(a.coordinate.getLongitude()))));
        double y = Math.sin(degToRad(b.coordinate.getLongitude()) - degToRad(a.coordinate.getLongitude())) * Math.cos(degToRad(b.coordinate.getLatitude()));

        return 2 * Math.atan(y / (Math.sqrt(x * x + y * y) + x));
    }

    private WaypointMission createWaypointMissionManual(){

        float Altitude = Float.parseFloat(Btn_Altitude.getText().toString());

        ArrayList<Waypoint> listGPS = new ArrayList<Waypoint>();

        Waypoint un = new Waypoint(43.768014, 4.000106, 0);
        Waypoint deux = new Waypoint(43.768023, 4.000114,0);
        Waypoint trois = new Waypoint(43.768059, 4.000073, 0);
        Waypoint quatre = new Waypoint(43.767935, 4.000129,0);

        listGPS.add(un);
        listGPS.add(deux);
        listGPS.add(trois);
        listGPS.add(quatre);

        double distanceMax = 0.0;

        Waypoint aMax = null;
        Waypoint bMax = null;

        for(int i = 0; i < listGPS.size(); i++) {
            for (int j = 0; j < listGPS.size(); j++) {
                double tmp = distance(listGPS.get(i).coordinate.getLatitude(), listGPS.get(j).coordinate.getLatitude(),
                        listGPS.get(i).coordinate.getLongitude(), listGPS.get(j).coordinate.getLongitude());
                if (tmp > distanceMax) {
                    distanceMax = tmp;
                    aMax = new Waypoint(listGPS.get(i).coordinate.getLatitude(),
                            listGPS.get(i).coordinate.getLongitude(),
                            0);
                    bMax = new Waypoint(listGPS.get(j).coordinate.getLatitude(),
                            listGPS.get(j).coordinate.getLongitude(),
                            0);
                }
            }
        }

        Waypoint g = getmiddle(aMax, bMax);
        double r = distanceMax/2;
        double rSecu = 2 + r;
        System.out.println("rayon = " + r);
        System.out.println("rayonSecu = " + rSecu);

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

        for(int j = 0; j < (int)Altitude; j++) {
            for (int i = 0; i < listGPS.size(); i++) {
                double az = calculAzimut(g, listGPS.get(i));
                double aziInverse = calculAzimut(g, listGPS.get(i));
                aziInverse = radToDeg(aziInverse);

                Waypoint F = new Waypoint(0.0, 0.0, 0);
                F = getNewPoint(g, az, meterToRad(rSecu), j + 1);

                if (aziInverse > 180) aziInverse = aziInverse - 360;

                if (aziInverse < -180) aziInverse = aziInverse + 360;

                markWaypoint(new LatLng(F.coordinate.getLatitude(), F.coordinate.getLongitude()));
                F.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, (int) aziInverse));
                F.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 1));

                builder.addWaypoint(F);
            }
        }
        return builder.build();
    }

    /*    private WaypointMission createWaypointMissionManual() throws IOException {
*//*
        LocationCoordinate3D dronePosition = mFlightController.getState().getAircraftLocation();
        double longitude = dronePosition.getLongitude();
        double latitude = dronePosition.getLatitude();
        float altitude = dronePosition.getAltitude();

*//*

        //Position actuelle du drone (point de départ)
        showToast("create");
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

        Waypoint w1 = new Waypoint(30, 30, 1);
        Waypoint w2 = new Waypoint(31,31, 1);

        List<Waypoint> waypointList = new ArrayList<>();

        BufferedWriter waypointFile = new BufferedWriter(new FileWriter("waypointManual.txt"));
        String state = Environment.getExternalStorageState();


        waypointList.add(w1);
        waypointList.add(w2);

        waypointFile.write(w1.coordinate.getLatitude() + " " + w1.coordinate.getLongitude() + " " + w1.altitude);
        showToast(w1.coordinate.getLatitude() + " " + w1.coordinate.getLongitude() + " " + w1.altitude);
        waypointFile.write(w2.coordinate.getLatitude() + " " + w2.coordinate.getLongitude() + " " + w2.altitude);
        showToast(w2.coordinate.getLatitude() + " " + w2.coordinate.getLongitude() + " " + w2.altitude);
        builder.waypointList(waypointList).waypointCount(waypointList.size());

        return builder.build();
    }*/


    private void loadWaypointMission() throws IOException {
        DJIError error;
        if(mission) {
            showToast("Button Mission Manual");
            error = getWaypointMissionOperator().loadMission(createWaypointMissionManual());
        } else {
            showToast("Button Mission Auto");
            error = getWaypointMissionOperator().loadMission(createWaypointMissionAuto());
        }

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

    @Override
    public void onMapClick(LatLng latLng) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (gMap == null) {
            gMap = googleMap;
            setUpMap();
        }

        LatLng shenzhen = new LatLng(22.5362, 113.9454);
        gMap.addMarker(new MarkerOptions().position(shenzhen).title("Marker in Shenzhen"));
        gMap.moveCamera(CameraUpdateFactory.newLatLng(shenzhen));

        updateDroneLocation();
    }

    private void setUpMap() {
        gMap.setOnMapClickListener(this);
    }

    public static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    private void updateDroneLocation(){

        LocationCoordinate3D dronePosition = mFlightController.getState().getAircraftLocation();
        double longitude = dronePosition.getLongitude();
        double latitude = dronePosition.getLatitude();
        LatLng dronePos = new LatLng(latitude, longitude);

        showToast(longitude + " " + latitude);
        gMap.moveCamera(CameraUpdateFactory.newLatLng(dronePos));

        //Create MarkerOptions object
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(dronePos);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));

        //gMap.addMarker(markerOptions);
        /*
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (droneMarker != null) {
                    droneMarker.remove();
                }

                if (checkGpsCoordination(droneLocationLat, droneLocationLng)) {
                    droneMarker = gMap.addMarker(markerOptions);
                }
            }
        });*/
    }

    private void markWaypoint(LatLng point){
        //Create MarkerOptions object
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        Marker marker = gMap.addMarker(markerOptions);
        mMarkers.put(mMarkers.size(), marker);
    }
}
