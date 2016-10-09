package baidumapsdk.demo.custom;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.internal.view.menu.ExpandedMenuView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;

import java.util.List;

import baidumapsdk.demo.R;

import static com.baidu.location.b.g.T;

public class LocationDemo2 extends AppCompatActivity implements BDLocationListener {

    private MapView mMapView;
    private BaiduMap mBaiduMap;
    private ListView listPoi;

    private static final boolean MY_LOCATION_ENALE = true; // 是否开启定位图层
    /**
     * 定位端
     */
    private LocationClient mLocClient;
    /**
     * 定位坐标
     */
    private LatLng locationLatLng;
    /**
     * 定位城市
     */
    private String city;
    /**
     * 反地理编码
     */
    private GeoCoder geoCoder;
    /**
     * 界面上方布局
     */
    private RelativeLayout topRL;
    /**
     * 搜索地址输入框
     */
    private EditText searchAddress;
    /**
     * 搜索输入框对应的ListView
     */
    private ListView searchPois;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_demo2);
        initView();
    }

    ImageView ivLocation;

    private void initView() {
        mMapView = (MapView) findViewById(R.id.main_bdmap);
        mMapView.showScaleControl(false);
        mMapView.showZoomControls(false);
        mBaiduMap = mMapView.getMap();

        topRL = (RelativeLayout) findViewById(R.id.main_top_RL);
        searchAddress = (EditText) findViewById(R.id.main_search_address);
        searchPois = (ListView) findViewById(R.id.main_search_pois);

        listPoi = (ListView) findViewById(R.id.main_pois);
        listPoi.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PoiInfo poiInfo = (PoiInfo) parent.getItemAtPosition(position);
                if (poiInfo == null) {
                    return;
                }

                Toast.makeText(LocationDemo2.this,
                        "select location, lat = " + poiInfo.location.latitude + "，long = " + poiInfo.location.longitude,
                        Toast.LENGTH_LONG).show();
            }
        });

        if (MY_LOCATION_ENALE) {
            //开启定位图层
            mBaiduMap.setMyLocationEnabled(true);
            mBaiduMap.setMyLocationConfigeration(new MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL, true, null));
        }

        // init map location btn
        ivLocation = (ImageView) findViewById(R.id.location_btn);
        ivLocation.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // 手动定位
                        mLocClient.requestLocation();
                    }
                }
        );

        // init MapStatus，default zoom level = 18
        MapStatus mMapStatus = new MapStatus.Builder().zoom(18).build();
        MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
        mBaiduMap.setMapStatus(mMapStatusUpdate);
        mBaiduMap.setOnMapStatusChangeListener(new BaiduMap.OnMapStatusChangeListener() {
            /**
             * 手势操作地图，设置地图状态等操作导致地图状态开始改变
             *
             * @param mapStatus 地图状态改变开始时的地图状态
             */
            @Override
            public void onMapStatusChangeStart(MapStatus mapStatus) {
            }

            /**
             * 地图状态变化中
             *
             * @param mapStatus 当前地图状态
             */
            @Override
            public void onMapStatusChange(MapStatus mapStatus) {
            }

            /**
             * 地图状态改变结束
             *
             * @param mapStatus 地图状态改变结束后的地图状态
             */
            @Override
            public void onMapStatusChangeFinish(MapStatus mapStatus) {
                //地图的中心点
                LatLng center = mapStatus.target;
                geoCoder.reverseGeoCode(new ReverseGeoCodeOption().location(center));
            }
        });

        // init GeoCoder
        geoCoder = GeoCoder.newInstance();
        geoCoder.setOnGetGeoCodeResultListener(new OnGetGeoCoderResultListener() {
            //地理编码查询结果回调函数
            @Override
            public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {
            }

            //反地理编码查询结果回调函数
            @Override
            public void onGetReverseGeoCodeResult(ReverseGeoCodeResult reverseGeoCodeResult) {
                List<PoiInfo> poiInfos = reverseGeoCodeResult.getPoiList();
                PoiAdapter poiAdapter = new PoiAdapter(LocationDemo2.this, poiInfos);
                listPoi.setAdapter(poiAdapter);
            }
        });

        // init location client
        mLocClient = new LocationClient(this);
        mLocClient.registerLocationListener(this);

        LocationClientOption option = new LocationClientOption();        //定位选项
        option.setCoorType("bd09ll");
        option.setIsNeedAddress(true);        //设置是否需要地址信息，默认为无地址
        option.setIsNeedLocationDescribe(true);      //设置是否需要返回位置语义化信息，可以在BDLocation.getLocationDescribe()中得到数据，ex:"在天安门附近"， 可以用作地址信息的补充
        option.setIsNeedLocationPoiList(true);  //设置是否需要返回位置POI信息，可以在BDLocation.getPoiList()中得到数据
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setOpenGps(true);        //设置是否打开gps进行定位
        option.setScanSpan(0);   //设置扫描间隔，单位是毫秒 当<1000(1s)时，定时定位无效
        mLocClient.setLocOption(option);

        mLocClient.start();
    }

    /**
     * 定位监听
     *
     * @param bdLocation
     */
    @Override
    public void onReceiveLocation(BDLocation bdLocation) {
        // 如果页面销毁，此时mBaiduMap==null，不再处理定位事件
        if (bdLocation == null || mBaiduMap == null) {
            return;
        }

        if (MY_LOCATION_ENALE) {
            //设置定位数据
            MyLocationData data = new MyLocationData.Builder()
                    .accuracy(bdLocation.getRadius())
                    .direction(bdLocation.getDirection())
                    .latitude(bdLocation.getLatitude())
                    .longitude(bdLocation.getLongitude())
                    .build();
            mBaiduMap.setMyLocationData(data);
        }

        // 地图显示定位位置
        LatLng ll = new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude());
        MapStatusUpdate msu = MapStatusUpdateFactory.newLatLngZoom(ll, 18);
        mBaiduMap.animateMapStatus(msu);

        // TODO 有问题
        locationLatLng = new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude()); //获取坐标，待会用于POI信息点与定位的距离
        city = bdLocation.getCity(); //获取城市，待会用于POISearch
        //文本输入框改变监听，必须在定位完成之后
        searchAddress.addTextChangedListener(searchTextWatcher);

        // 反地理编码
        ReverseGeoCodeOption reverseGeoCodeOption = new ReverseGeoCodeOption();
        reverseGeoCodeOption.location(new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude()));
        geoCoder.reverseGeoCode(reverseGeoCodeOption);
    }

    private TextWatcher searchTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            String input = s.toString().trim();
            if (input.isEmpty()) {
                searchPois.setVisibility(View.GONE);
                return;
            }

            // PoiSearch
            PoiSearch poiSearch = PoiSearch.newInstance();
            PoiCitySearchOption poiCitySearchOption = new PoiCitySearchOption(); //城市内检索
            poiCitySearchOption.keyword(s.toString()); //关键字
            poiCitySearchOption.city(city);
            poiCitySearchOption.pageCapacity(10);
            poiCitySearchOption.pageNum(1);
            poiSearch.searchInCity(poiCitySearchOption);
            poiSearch.setOnGetPoiSearchResultListener(new OnGetPoiSearchResultListener() {
                //poi 查询结果回调
                @Override
                public void onGetPoiResult(PoiResult poiResult) {
                    List<PoiInfo> poiInfos = poiResult.getAllPoi();
                    PoiSearchAdapter poiSearchAdapter = new PoiSearchAdapter(LocationDemo2.this, poiInfos, locationLatLng);
                    searchPois.setVisibility(View.VISIBLE);
                    searchPois.setAdapter(poiSearchAdapter);
                }

                @Override
                public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {
                }

                @Override
                public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

                }
            });
        }
    };

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mLocClient != null) {
            mLocClient.stop(); //退出时停止定位
            mLocClient = null;
        }
        if (geoCoder != null) {
            geoCoder.destroy();
            geoCoder = null;
        }
        if (mMapView != null) {
            mMapView.onDestroy(); // activity 销毁时同时销毁地图控件
            mMapView = null;
        }
    }

}
