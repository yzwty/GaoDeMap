package com.fzu.yzw.gaodemap;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeAddress;
import com.amap.api.services.geocoder.GeocodeQuery;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkPath;
import com.amap.api.services.route.WalkRouteResult;
import com.amap.api.services.route.WalkStep;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements GeocodeSearch.OnGeocodeSearchListener,RouteSearch.OnRouteSearchListener{
    private static final String TAG = "MainActivity";
    private MapView mapView;
    private AMap aMap;
    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;
    //声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;
    public MyLocationStyle myLocationStyle;
    private GeocodeSearch geocodeSearch;
    private RouteSearch routeSearch;
    private TextView mAddressText;
    private EditText mTargetAddress;
    private Button navBtn;
    public double mLatitude;
    public double mLongitude;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapView=(MapView)findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mAddressText=(TextView)findViewById(R.id.address_text);
        mTargetAddress=(EditText)findViewById(R.id.targetAddress);
        initGPS();
        init();

        //创建地址解析对象GeocodeSearch
        geocodeSearch=new GeocodeSearch(this);
        //设置GeocodeSearch的监听
        geocodeSearch.setOnGeocodeSearchListener(this);
        //创建路线导航对象RouteSearch
        routeSearch=new RouteSearch(this);
        ////设置RouteSearch的监听
        routeSearch.setRouteSearchListener(this);

        ToggleButton tb=(ToggleButton)findViewById(R.id.toggleBtn);
        tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    aMap.setMapType(AMap.MAP_TYPE_SATELLITE);
                }else {
                    aMap.setMapType(AMap.MAP_TYPE_NORMAL);
                }
            }
        });

        navBtn=(Button)findViewById(R.id.navBtn);
        navBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String targetAddress=mTargetAddress.getText().toString().trim();
                if(targetAddress.equals("")){
                    Toast.makeText(MainActivity.this,"请输入有效地址",Toast.LENGTH_SHORT).show();
                }
                else
                {
                    GeocodeQuery query=new GeocodeQuery(targetAddress,"福州");
                    geocodeSearch.getFromLocationNameAsyn(query);
                    // 隐藏软键盘
                    InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    manager.hideSoftInputFromWindow(mTargetAddress.getWindowToken(), 0);
                }
            }
        });

    }

    private void initGPS(){
        LocationManager locationManager=(LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        // 通过GPS卫星定位，定位级别可以精确到街（通过24颗卫星定位，在室外和空旷的地方定位准确、速度快）
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // 通过WLAN或移动网络(4G/3G)确定的位置（也称作AGPS，辅助GPS定位。主要用于在室内或遮盖物（建筑群或茂密的深林等）密集的地方定位）
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!gps||!network){
            AlertDialog.Builder dialog=new AlertDialog.Builder(this);
            dialog.setMessage("该应用请求打开GPS");
            dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // 转到手机设置界面，用户设置GPS
                    Intent intent = new Intent(
                            android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(intent, 0); // 设置完成后返回到原来的界面
                }
            });
            dialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            dialog.setCancelable(false);
            dialog.show();
        }
    }
    private void init(){
        if(aMap==null){
            aMap=mapView.getMap();
        }
        //初始化定位
        mLocationClient = new AMapLocationClient(getApplicationContext());
        //初始化AMapLocationClientOption对象
        mLocationOption = new AMapLocationClientOption();
        //设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
        //设置定位间隔,单位毫秒,默认为2000ms，最低1000ms。
        mLocationOption.setInterval(5000);
        //设置是否返回地址信息（默认返回地址信息）
        mLocationOption.setNeedAddress(true);

        //设置地图放大级别的
        CameraUpdate cu= CameraUpdateFactory.zoomTo(17);
        aMap.moveCamera(cu);


        myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
        myLocationStyle.interval(5000); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
      //  myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE) ;//定位一次，且将视角移动到地图中心点
        aMap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
        aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。

        aMap.setTrafficEnabled(true);//显示实时路况图层，aMap是地图控制器对象。

        //设置定位回调监听
        mLocationClient.setLocationListener(mLocationListener);

        //启动定位
        mLocationClient.startLocation();
    }
    AMapLocationListener mLocationListener = new AMapLocationListener(){
        @Override
        public void onLocationChanged(AMapLocation amapLocation) {

            if(amapLocation!=null){
                updatePosition(amapLocation);

            }else {
                Toast.makeText(MainActivity.this,"当前GPS信号较弱，请到开阔地体验更佳",Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void updatePosition(AMapLocation amapLocation){
        //更新地图显示区域
        if(amapLocation.getLatitude()!=0.0D&&amapLocation.getLongitude()!=0.0D){
            mLatitude=amapLocation.getLatitude();
            mLongitude=amapLocation.getLongitude();
        }

        LatLng latLng=new LatLng(amapLocation.getLatitude(),amapLocation.getLongitude());
        CameraUpdate cu= CameraUpdateFactory.changeLatLng(latLng);
        aMap.moveCamera(cu);

        if(!TextUtils.isEmpty(amapLocation.getAddress())){
            mAddressText.setText(amapLocation.getAddress());
        }
        /*
        //创建地图图标
        MarkerOptions markerOptions=new MarkerOptions();
        //设置图标的位置
        markerOptions.position(latLng);
        //设置图标（可以使用默认，也可以自己找的图片）
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        markerOptions.draggable(true);
        //在地图中添加marker
        aMap.addMarker(markerOptions);
        */

    }



    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mLocationClient.stopLocation();//停止定位后，本地定位服务并不会被销毁

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        mLocationClient.onDestroy();//销毁定位客户端，同时销毁本地定位服务。
    }


    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {

    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

        GeocodeAddress addr=geocodeResult.getGeocodeAddressList().get(0);
        //获取目的地的经纬度
        LatLonPoint latLng=addr.getLatLonPoint();
        //创建路线规划的起始点
        RouteSearch.FromAndTo ft=new RouteSearch.FromAndTo(new LatLonPoint(mLatitude,mLongitude),latLng);

        //创建自驾车的查询条件
      //  RouteSearch.DriveRouteQuery driveRouteQuery=new RouteSearch.DriveRouteQuery(ft,RouteSearch.DrivingDefault,null,null,null);
      //  routeSearch.calculateDriveRouteAsyn(driveRouteQuery);

        //创建步行的查询条件
        RouteSearch.WalkRouteQuery walkRouteQuery = new RouteSearch.WalkRouteQuery(ft,RouteSearch.WalkDefault);
        routeSearch.calculateWalkRouteAsyn(walkRouteQuery);
    }

    @Override
    public void onBusRouteSearched(BusRouteResult busRouteResult, int i) {

    }

    @Override
    public void onDriveRouteSearched(DriveRouteResult driveRouteResult, int i) {
        /*
        DrivePath drivePath=driveRouteResult.getPaths().get(0);
        aMap.clear();// 清理地图上的所有覆盖物
        List<DriveStep> steps=drivePath.getSteps();
        for(DriveStep step: steps){
            List<LatLonPoint> points=step.getPolyline();
            List<LatLng> latLngs=new ArrayList<>();
            for(LatLonPoint point: points){
                latLngs.add(new LatLng(point.getLatitude(),point.getLongitude()));
            }
            PolylineOptions polylineOptions=new PolylineOptions()
                    .addAll(latLngs)
                    .color(0xffff0000)
                    .width(8);
            aMap.addPolyline(polylineOptions);
        }
        */
    }

    @Override
    public void onWalkRouteSearched(WalkRouteResult walkRouteResult, int i) {
        WalkPath walkPath=walkRouteResult.getPaths().get(0);
        aMap.clear();// 清理地图上的所有覆盖物
        List<WalkStep> steps=walkPath.getSteps();
        for(WalkStep step: steps){
            List<LatLonPoint> points=step.getPolyline();
            List<LatLng> latLngs=new ArrayList<>();
            for(LatLonPoint point: points){
                latLngs.add(new LatLng(point.getLatitude(),point.getLongitude()));
            }
            PolylineOptions polylineOptions=new PolylineOptions()
                    .addAll(latLngs)
                    .color(0xffff0000)
                    .width(8);
            aMap.addPolyline(polylineOptions);
        }
    }

    @Override
    public void onRideRouteSearched(RideRouteResult rideRouteResult, int i) {

    }
}
