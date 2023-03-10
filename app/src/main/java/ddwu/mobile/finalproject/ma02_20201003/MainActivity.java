package ddwu.mobile.finalproject.ma02_20201003;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";

    ListView lvList;
    String apiAddress;
    FavoritesDBHelper helper;

    ArrayAdapter<CulturalSpaceInfoDTO> adapter;
    List<CulturalSpaceInfoDTO> resultList;
    CulturalSpaceInfoDTO culturalSpaceInfoDTO = new CulturalSpaceInfoDTO();
    final int REQ_PERMISSION_CODE = 100;

    TextView tvText;

    FusedLocationProviderClient flpClient;
    Location mLastLocation;

    //    GoogleMap ?????? ??????
    private GoogleMap mGoogleMap;
    private Marker centerMarker;

    private Button btn_open_bt_sheet;
    LatLng currentLoc;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn_open_bt_sheet = findViewById(R.id.btn_open_bt_sheet);
        //txt_result = findViewById(R.id.txt_result);
        helper = new FavoritesDBHelper(this);
        btn_open_bt_sheet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CulturalSpaceInfoGpsBottomSheetFragment bottomSheetDialog = new CulturalSpaceInfoGpsBottomSheetFragment();
                bottomSheetDialog.show(getSupportFragmentManager(), "BottomSheet");

                Bundle bundle = new Bundle();
                bundle.putString("key", String.valueOf(mLastLocation));
                bottomSheetDialog.setArguments(bundle);
                //txt_result.setText("???????????? ?????????");

            }
        });
        Log.d(TAG,"????????? ???/ ???"+ mLastLocation);
        apiAddress = "http://openapi.seoul.go.kr:8088/6d55526c4f6a696e3839435071654c/xml/culturalSpaceInfo/1/820";

        //lvList = (ListView) findViewById(R.id.lvList);

        resultList = new ArrayList<CulturalSpaceInfoDTO>();
        adapter = new ArrayAdapter<CulturalSpaceInfoDTO>(this, android.R.layout.simple_list_item_1, resultList);

        //lvList.setAdapter(adapter);

        flpClient = LocationServices.getFusedLocationProviderClient(this);



        if(checkPermission()) {
            flpClient.requestLocationUpdates(
                    getLocationRequest(),
                    mLocCallback,
                    Looper.getMainLooper()
            );
        }


//        MapFragment ?????? ??? ?????? ?????? ??????
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(mapReadyCallback);


//        res/values/strings.xml ??? server_url ?????? ?????????
//        apiAddress = getResources().getString(R.string.server_url);


        Button Button_Search = findViewById(R.id.Button_Search);
        Button_Search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), NaverBlogSearchActivity.class);
                startActivity(intent);
            }
        });
        Button Button_favorite = findViewById(R.id.Button_favorite);
        Button_favorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),AllFavoritesActivity.class);
                startActivity(intent);
            }
        });
        Button Button_near = findViewById(R.id.Button_near);
        Button_near.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),NearPlaceActivity.class);
                intent.putExtra("Location", mLastLocation);
                startActivity(intent);
            }
        });

    }


    class NetworkAsyncTask extends AsyncTask<String, Void, String> {

        final static String NETWORK_ERR_MSG = "Server Error!";
        public final static String TAG = "NetworkAsyncTask";
        ProgressDialog progressDlg;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDlg = ProgressDialog.show(MainActivity.this, "Wait", "Downloading...");     // ???????????? ??????????????? ??????
        }

        @Override
        protected String doInBackground(String... strings) {
            String address = strings[0];
            String result = downloadContents(address);
            if (result == null) {
                cancel(true);
                return NETWORK_ERR_MSG;
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            progressDlg.dismiss();  // ???????????? ??????????????? ??????
            adapter.clear();        // ???????????? ???????????? ?????? ????????? ????????? ?????????

//          parser ?????? ??? OpenAPI ?????? ????????? ???????????? parsing ??????
            CultureSapceInfoXmlParser parser = new CultureSapceInfoXmlParser();

            resultList = parser.parse(result);

            if (resultList == null) {       // ????????? ????????? ???????????? ???????????? ?????? ??????
                Toast.makeText(MainActivity.this, "????????? ???????????? ???????????????.", Toast.LENGTH_SHORT).show();
            } else if (!resultList.isEmpty()) {
                adapter.addAll(resultList);
                for (CulturalSpaceInfoDTO data : resultList) {
                    LatLng latLng = new LatLng(data.X_Coord, data.Y_Coord);
                    MarkerOptions options = new MarkerOptions()
                            .title(data.Fac_Name)
                            .position(latLng)
                            .snippet(data.Addr+ "\n" + data.Fac_Desc)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    centerMarker = mGoogleMap.addMarker(options);

                    // centerMarker.showInfoWindow();
                }// ??????????????? ???????????? ?????? ???????????? parsing ?????? ArrayList ??? ??????
                mGoogleMap.setOnInfoWindowLongClickListener(new GoogleMap.OnInfoWindowLongClickListener() {
                    @Override
                    public void onInfoWindowLongClick(@NonNull Marker marker) {
                        SQLiteDatabase db = helper.getWritableDatabase();

                        ContentValues row = new ContentValues();

                        row.put(FavoritesDBHelper.COL_NAME, marker.getTitle());
                        row.put(FavoritesDBHelper.COL_ADD, marker.getSnippet());

                        //?????? ?????? ?????? result
                        db.insert(FavoritesDBHelper.TABLE_NAME, null, row);

                        //db.execSQL("INSERT INTO "+ FavoritesDBHelper.TABLE_NAME +
                        //     " VALUES (NULL, '" + etName.getText().toString() + "','" +  etPhone.getText().toString() + "','" + etCategory.getText().toString() + "');");

                        helper.close();
                        Toast.makeText(MainActivity.this, marker.getTitle() + "??????????????? ??????????????????.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            Log.d(TAG,"??? ?????????"+ resultList);

        }


        @Override
        protected void onCancelled(String msg) {
            super.onCancelled();
            progressDlg.dismiss();
            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
        }
    }
    /* ??????(apiAddress)??? ???????????? ????????? ???????????? ????????? ??? ?????? */
    protected String downloadContents(String address) {
        HttpURLConnection conn = null;
        InputStream stream = null;
        String result = null;

        try {
            URL url = new URL(address);
            conn = (HttpURLConnection)url.openConnection();
            stream = getNetworkConnection(conn);
            result = readStreamToString(stream);
            if (stream != null) stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) conn.disconnect();
        }

        return result;
    }
    /* URLConnection ??? ???????????? ???????????? ?????? ??? ??????, ?????? ??? ????????? InputStream ?????? */
    private InputStream getNetworkConnection(HttpURLConnection conn) throws Exception {
        conn.setReadTimeout(3000);
        conn.setConnectTimeout(3000);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);

        if (conn.getResponseCode() != HttpsURLConnection.HTTP_OK) {
            throw new IOException("HTTP error code: " + conn.getResponseCode());
        }

        return conn.getInputStream();
    }


    /* InputStream??? ???????????? ???????????? ?????? ??? ?????? */
    protected String readStreamToString(InputStream stream){
        StringBuilder result = new StringBuilder();

        try {
            InputStreamReader inputStreamReader = new InputStreamReader(stream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String readLine = bufferedReader.readLine();

            while (readLine != null) {
                result.append(readLine + "\n");
                readLine = bufferedReader.readLine();
            }

            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result.toString();
    }


    OnMapReadyCallback mapReadyCallback=new OnMapReadyCallback() {
        @Override
        public void onMapReady(@NonNull GoogleMap googleMap) {

            mGoogleMap = googleMap; //????????? ???????????? ??????????????? ??????

            //????????? ????????? ????????????
            if(checkPermission()) {
                mGoogleMap.setMyLocationEnabled(true);
            }
            new NetworkAsyncTask().execute(apiAddress);

            resultList = new ArrayList<CulturalSpaceInfoDTO>();
            Log.d(TAG,"??? ?????????"+ resultList);
           // mGoogleMap.setOnMyLocationButtonClickListener(locationButtonClickListener);
            // mGoogleMap.setOnMyLocationClickListener(locationClickListener);
            //ArrayList<Marker> markers = new ArrayList<Marker>();
            //for(int i=0; i<resultList.size(); i++) {
           // Log.d(TAG,"??? ?????????"+ String.valueOf(culturalSpaceInfoDTO.X_Coord + culturalSpaceInfoDTO.Y_Coord));
            for (CulturalSpaceInfoDTO data : resultList) {
                LatLng latLng = new LatLng(data.X_Coord, data.Y_Coord);
                MarkerOptions options = new MarkerOptions()
                            .title(data.Fac_Name)
                            .position(latLng)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                centerMarker = mGoogleMap.addMarker(options);

                   // centerMarker.showInfoWindow();
                }


            //}
            //Log.d(TAG,  String.valueOf(culturalSpaceInfoDTO.getX_Coord())+ String.valueOf(culturalSpaceInfoDTO.getX_Coord()));
            //LatLng latLng = new LatLng(culturalSpaceInfoDTO.getX_Coord(),culturalSpaceInfoDTO.getY_Coord());
            //mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17)); // ???????????? ?????????
            //mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,17)); // ??????????????? ?????????
            //-> ????????? ???????????? ?????? ???????????? ????????? ???????????? ?????? ????????? ??? ??? ??????
            // ?????? ????????? ????????? ?????? ????????? onMap Ready?????? ?????????
            //MarkerOptions options = new MarkerOptions();
           // options.position(latLng)
           //         .title("????????????")
           //         .snippet("?????????")
            //        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

            //centerMarker = mGoogleMap.addMarker(options); // ????????? ?????? ???????????? ????????? ??????????????? ->  ?????? ?????? ?????? , ????????? ??????
            // ????????? ?????????
            Log.i(TAG, "???????????? onMapReady");
        }

    };


    LocationCallback mLocCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            for (Location loc : locationResult.getLocations()) {
                double lat = loc.getLatitude();
                double lng = loc.getLongitude();
                //setTvText(String.format("(%.6f, %.6f)", lat, lng));
                mLastLocation = loc;

//                ?????? ?????? ????????? GoogleMap ?????? ??????
                LatLng currentLoc = new LatLng(lat,lng);
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLoc,17));
                //centerMarker.setPosition(currentLoc);
                Log.i(TAG, "??????"+ String.valueOf(currentLoc));
            }
        }
    }; // ?????? ????????? ???????????? ???????????? ??????
    private LocationRequest getLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQ_PERMISSION_CODE:
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "???????????? ?????? ??????", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "???????????? ?????????", Toast.LENGTH_SHORT).show();
                }
        }
    }
    private void getLastLocation() {

        flpClient.getLastLocation().addOnSuccessListener(
                new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            //setTvText( String.format("????????????: (%.6f, %.6f)", latitude, longitude) );
                            mLastLocation = location;
                        } else {
                            Toast.makeText(MainActivity.this, "No location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        flpClient.getLastLocation().addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //Log.e(TAG, "Unknown");
                    }
                }
        );

    }

    private boolean checkPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
            // ????????? ?????? ?????? ????????? ??????
            Toast.makeText(this,"Permissions Granted", Toast.LENGTH_SHORT).show();
            return true;
        } else {
            // ?????? ??????
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION}, REQ_PERMISSION_CODE);
            return false;
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        flpClient.removeLocationUpdates(mLocCallback);
    }

}