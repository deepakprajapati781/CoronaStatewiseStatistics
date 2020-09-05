package com.coronacount.myapplication;


import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    public String apiData = "";
    Spinner dropdown;
    TextView total_cases,textview_graph_ActiveCases,textview_graph_NewCases,textview_graph_TotalCasesDischargeDeath1,
            textview_graph_TotalCasesDischargeDeath2,textview_graph_TotalCasesDischargeDeath3,
    textview_graph_NewCasesDischargeDeath1,textview_graph_NewCasesDischargeDeath2,textview_graph_NewCasesDischargeDeath3;
    GraphView graphViewTotalCases,graphViewActiveCase,graphViewNewCase,
            graphViewTotalCasesDischargeDeath,graphViewNewCasesDischargeDeath;

    LineGraphSeries<DataPoint> series;
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("d MMM yy");
    ArrayList<HashMap<String,HashMap>> gujArray=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        
        if (!haveNetwork()) {
            Toast.makeText(MainActivity.this, "No Internet connection...", Toast.LENGTH_SHORT).show();
        }
        final SwipeRefreshLayout pullToRefresh = findViewById(R.id.pullToRefresh);
        pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (!haveNetwork()) {
                    Toast.makeText(MainActivity.this, "No Internet connection...", Toast.LENGTH_SHORT).show();
                }else {
                    loadingCode(); // your code
                }
                pullToRefresh.setRefreshing(false);
            }
        });
        loadingCode();

    }

    public void loadingCode(){
        total_cases=(TextView)findViewById(R.id.textview_graph_TotalCases);
        textview_graph_ActiveCases=(TextView)findViewById(R.id.textview_graph_ActiveCases);
        textview_graph_NewCases=(TextView)findViewById(R.id.textview_graph_NewCases);
        textview_graph_TotalCasesDischargeDeath1=(TextView)findViewById(R.id.textview_graph_TotalCasesDischargeDeath1);
        textview_graph_TotalCasesDischargeDeath2=(TextView)findViewById(R.id.textview_graph_TotalCasesDischargeDeath2);
        textview_graph_TotalCasesDischargeDeath3=(TextView)findViewById(R.id.textview_graph_TotalCasesDischargeDeath3);
        textview_graph_NewCasesDischargeDeath1=(TextView)findViewById(R.id.textview_graph_NewCasesDischargeDeath1);
        textview_graph_NewCasesDischargeDeath2=(TextView)findViewById(R.id.textview_graph_NewCasesDischargeDeath2);
        textview_graph_NewCasesDischargeDeath3=(TextView)findViewById(R.id.textview_graph_NewCasesDischargeDeath3);


        dropdown = findViewById(R.id.spinner1);
        String[] items = new String[]{"Andaman and Nicobar Islands", "Andhra Pradesh", "Arunachal Pradesh",
                "Assam","Bihar","Chandigarh","Chhattisgarh","Dadra and Nagar Haveli and Daman and Diu","Delhi","Goa","Gujarat","Haryana","Himachal Pradesh",
                "Jammu and Kashmir","Jharkhand","Karnataka","Kerala","Ladakh","Madhya Pradesh","Maharashtra","Manipur","Meghalaya","Mizoram",
                "Nagaland","Odisha","Puducherry","Punjab","Rajasthan","Sikkim","Tamil Nadu","Telangana","Tripura","Uttarakhand",
                "Uttar Pradesh","West Bengal"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        dropdown.setAdapter(adapter);
        dropdown.setOnItemSelectedListener(this);
        int spinnerPosition = adapter.getPosition("Delhi");
        dropdown.setSelection(spinnerPosition);

        graphViewTotalCases=(GraphView) findViewById(R.id.graph_TotalCases);
        graphViewActiveCase=(GraphView) findViewById(R.id.graph_ActiveCases);
        graphViewNewCase=(GraphView) findViewById(R.id.graph_NewCases);
        graphViewTotalCasesDischargeDeath=(GraphView) findViewById(R.id.graph_TotalCasesDischargeDeath);
        graphViewNewCasesDischargeDeath=(GraphView) findViewById(R.id.graph_NewCasesDischargeDeath);


        OkHttpClient client = new OkHttpClient();
        String url = "https://api.rootnet.in/covid19-in/stats/history";
        Request request = new Request.Builder()
                .url(url)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String myResponse = response.body().string();
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            apiData=myResponse;
                            prepareMap(myResponse,"Delhi");
                            setGraphView(gujArray,graphViewTotalCases,"totalConfirmed",false);
                            total_cases.setText("Total Cases : "+gujArray.get(gujArray.size()-1).get("data").get("totalConfirmed"));
                            setGraphView(gujArray,graphViewActiveCase,"activeCases",false);
                            textview_graph_ActiveCases.setText("Active Cases : "+gujArray.get(gujArray.size()-1).get("data").get("activeCases"));
                            setGraphView(gujArray,graphViewNewCase,"newCases",false);
                            textview_graph_NewCases.setText("New Cases : "+gujArray.get(gujArray.size()-1).get("data").get("newCases"));

                            setGraphView(gujArray,graphViewTotalCasesDischargeDeath,"totalConfirmed",true);
                            textview_graph_TotalCasesDischargeDeath1.setText("Total Cases : "+gujArray.get(gujArray.size()-1).get("data").get("totalConfirmed"));
                            setGraphView(gujArray,graphViewTotalCasesDischargeDeath,"totalDeaths",true);
                            textview_graph_TotalCasesDischargeDeath2.setText("Total Discharged : "+gujArray.get(gujArray.size()-1).get("data").get("totalDischarged"));
                            setGraphView(gujArray,graphViewTotalCasesDischargeDeath,"totalDischarged",true);
                            textview_graph_TotalCasesDischargeDeath3.setText("Total Death : "+gujArray.get(gujArray.size()-1).get("data").get("totalDeaths"));
                            setGraphView(gujArray,graphViewNewCasesDischargeDeath,"newCases",true);
                            textview_graph_NewCasesDischargeDeath1.setText("  Daily Cases : "+gujArray.get(gujArray.size()-1).get("data").get("newCases") + "    (In Last 24 Hour)");
                            setGraphView(gujArray,graphViewNewCasesDischargeDeath,"newDeath",true);
                            textview_graph_NewCasesDischargeDeath2.setText("    Daily Discharged : "+gujArray.get(gujArray.size()-1).get("data").get("newdischarge") + "     (In Last 24 Hour)");
                            setGraphView(gujArray,graphViewNewCasesDischargeDeath,"newdischarge",true);
                            textview_graph_NewCasesDischargeDeath3.setText("Daily Death : "+gujArray.get(gujArray.size()-1).get("data").get("newDeath") + "     (In Last 24 Hour)");

                        }
                    });
                }
            }
        });

    }

    private DataPoint[] getDataPoint(ArrayList<HashMap<String,HashMap>> gujArray,String dataKey) {
        Calendar []cal = new Calendar[30];
        for(int i=0;i<cal.length;i++) {
            cal[i] = Calendar.getInstance();
            cal[i].set(Calendar.YEAR, 2020);
            cal[i].set(Calendar.MONTH, 1);
            cal[i].set(Calendar.DAY_OF_MONTH, i+1);
        }
        DataPoint[] dp = new DataPoint[gujArray.size()];
        for(int i=0;i<dp.length;i++) {
            dp[i] = new DataPoint(new Date(gujArray.get(i).get("data").get("date").toString()),Integer.parseInt(gujArray.get(i).get("data").get(dataKey).toString()) );
        }
        return dp;
    }

    private JSONObject prepareMap(String res,String stateName){
        gujArray = new ArrayList<>();
        try {
            JSONObject obj = new JSONObject(res);
            JSONArray objArr = obj.getJSONArray("data");
            int preCases = 0;
            int predeath = 0;
            int preDischarge = 0;

            for(int i=0;i<objArr.length();i++){
                JSONObject oneDateRegion = (JSONObject) objArr.get(i);
                JSONArray oneDateRegionArr = oneDateRegion.getJSONArray("regional");
                HashMap<String,String> map=null;

                for(int x=0;x<oneDateRegionArr.length();x++) {
                    JSONObject stateData = (JSONObject) oneDateRegionArr.get(x);
                    if(stateData.get("loc").toString().equals(stateName)){
                        map = new HashMap<>();
                        int newCases = Integer.parseInt(stateData.get("totalConfirmed").toString())-preCases;
                        int newdischarge = Integer.parseInt(stateData.get("discharged").toString())-preDischarge;
                        int newDeath = Integer.parseInt(stateData.get("deaths").toString())-predeath;

                        preCases = Integer.parseInt(stateData.get("totalConfirmed").toString());
                        preDischarge = Integer.parseInt(stateData.get("discharged").toString());
                        predeath = Integer.parseInt(stateData.get("deaths").toString());
                        int activeCases = preCases - (preDischarge+predeath);
                        map.put("totalConfirmed",stateData.get("totalConfirmed").toString());
                        map.put("newCases",String.valueOf(newCases));
                        map.put("newdischarge",String.valueOf(newdischarge));
                        map.put("newDeath",String.valueOf(newDeath));
                        map.put("activeCases",String.valueOf(activeCases));
                        map.put("date",oneDateRegion.get("day").toString().replace("-","/"));

                        map.put("totalDischarged",stateData.get("discharged").toString());
                        map.put("totalDeaths",stateData.get("deaths").toString());


                        break;
                    }

                }
                if(map!=null){
                    HashMap<String,HashMap> gujdata = new HashMap<>();
                    gujdata.put("data",map);
                    gujArray.add(gujdata);
                }

            }
            System.out.println(gujArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }



    private void setGraphView(ArrayList<HashMap<String,HashMap>> gujArray,GraphView graphView,String dataKey,boolean isCombineGraph){
        series=new LineGraphSeries<>(getDataPoint(gujArray,dataKey));
        if(!isCombineGraph){
            series.setColor(Color.BLUE);
            graphView.removeAllSeries();
        }else {
            switch (dataKey) {
                case "totalConfirmed":
                case "newCases":
                    graphView.removeAllSeries();
                    series.setColor(Color.BLUE);
                    break;

                case "totalDischarged":
                case "newdischarge":
                    series.setColor(Color.GREEN);
                    break;


                case "totalDeaths":
                case "newDeath":
                    series.setColor(Color.RED);
                    break;
            }
        }

        graphView.addSeries(series);
        series.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                String ss = simpleDateFormat.format(new Date((long)dataPoint.getX())) + " \ncount:" + (int)dataPoint.getY();
                System.out.println(ss);
                Toast.makeText(MainActivity.this,ss,Toast.LENGTH_SHORT).show();
            }
        });
        GridLabelRenderer render = graphView.getGridLabelRenderer();
        render.setHorizontalLabelsAngle(135);
        GridLabelRenderer gridLabel = graphView.getGridLabelRenderer();
        gridLabel.setLabelFormatter(new DefaultLabelFormatter(){
            @Override
            public String formatLabel(double value,boolean isValueX){
                if(isValueX){
                    return simpleDateFormat.format(new Date((long)value));
                }
                else {
                    return super.formatLabel(value, isValueX);
                }
            }
        });
        gridLabel.setPadding(100);
        gridLabel.setTextSize(22);
        series.setAnimated(true);
//        series.setDrawBackground(true);
        series.setDrawDataPoints(true);
        series.setDataPointsRadius(1);
        series.setThickness(5);
        graphView.getGridLabelRenderer().setNumHorizontalLabels(11);
        graphView.getGridLabelRenderer().setLabelsSpace(100);
//        graphView.getGridLabelRenderer().setHumanRounding(true);
        graphView.getViewport().setScrollable(true);
        graphView.getViewport().setScrollableY(true);
        graphView.getViewport().setScalable(true);
        graphView.getViewport().setScalableY(true);
    }


    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        String name= "Delhi";
        switch (i){
            case 0:
                name="Andaman and Nicobar Islands";
                break;
            case 1:
                name="Andhra Pradesh";
                break;
            case 2:
                name="Arunachal Pradesh";
                break;
            case 3:
                name="Assam";
                break;
            case 4:
                name="Bihar";
                break;
            case 5:
                name="Chandigarh";
                break;
            case 6:
                name="Chhattisgarh";
                break;
            case 7:
                name="Dadra and Nagar Haveli and Daman and Diu";
                break;
            case 8:
                name="Delhi";
                break;
            case 9:
                name="Goa";
                break;
            case 10:
                name="Gujarat";
                break;
            case 11:
                name="Haryana";
                break;
            case 12:
                name="Himachal Pradesh";
                break;
            case 13:
                name="Jammu and Kashmir";
                break;
            case 14:
                name="Jharkhand";
                break;
            case 15:
                name="Karnataka";
                break;
            case 16:
                name="Kerala";
                break;
            case 17:
                name="Ladakh";
                break;
            case 18:
                name="Madhya Pradesh";
                break;
            case 19:
                name="Maharashtra";
                break;
            case 20:
                name="Manipur";
                break;
            case 21:
                name="Meghalaya";
                break;
            case 22:
                name="Mizoram";
                break;
            case 23:
                name="Nagaland";
                break;
            case 24:
                name="Odisha";
                break;
            case 25:
                name="Puducherry";
                break;
            case 26:
                name="Punjab";
                break;
            case 27:
                name="Rajasthan";
                break;
            case 28:
                name="Sikkim";
                break;
            case 29:
                name="Tamil Nadu";
                break;
            case 30:
                name="Telangana";
                break;
            case 31:
                name="Tripura";
                break;
            case 32:
                name="Uttarakhand";
                break;
            case 33:
                name="Uttar Pradesh";
                break;
            case 34:
                name="West Bengal";
                break;

        }
        if(!apiData.isEmpty()) {
            prepareMap(apiData, name);
            setGraphView(gujArray,graphViewTotalCases,"totalConfirmed",false);
            total_cases.setText("Total Cases : "+gujArray.get(gujArray.size()-1).get("data").get("totalConfirmed"));
            setGraphView(gujArray,graphViewActiveCase,"activeCases",false);
            textview_graph_ActiveCases.setText("Active Cases : "+gujArray.get(gujArray.size()-1).get("data").get("activeCases"));
            setGraphView(gujArray,graphViewNewCase,"newCases",false);
            textview_graph_NewCases.setText("New Cases : "+gujArray.get(gujArray.size()-1).get("data").get("newCases"));

            setGraphView(gujArray,graphViewTotalCasesDischargeDeath,"totalConfirmed",true);
            textview_graph_TotalCasesDischargeDeath1.setText("Total Cases : "+gujArray.get(gujArray.size()-1).get("data").get("totalConfirmed"));
            setGraphView(gujArray,graphViewTotalCasesDischargeDeath,"totalDeaths",true);
            textview_graph_TotalCasesDischargeDeath2.setText("Total Discharged : "+gujArray.get(gujArray.size()-1).get("data").get("totalDischarged"));
            setGraphView(gujArray,graphViewTotalCasesDischargeDeath,"totalDischarged",true);
            textview_graph_TotalCasesDischargeDeath3.setText("Total Death : "+gujArray.get(gujArray.size()-1).get("data").get("totalDeaths"));
            setGraphView(gujArray,graphViewNewCasesDischargeDeath,"newCases",true);
            textview_graph_NewCasesDischargeDeath1.setText("  Daily Cases : "+gujArray.get(gujArray.size()-1).get("data").get("newCases") + "    (In Last 24 Hour)");
            setGraphView(gujArray,graphViewNewCasesDischargeDeath,"newDeath",true);
            textview_graph_NewCasesDischargeDeath2.setText("    Daily Discharged : "+gujArray.get(gujArray.size()-1).get("data").get("newdischarge") + "     (In Last 24 Hour)");
            setGraphView(gujArray,graphViewNewCasesDischargeDeath,"newdischarge",true);
            textview_graph_NewCasesDischargeDeath3.setText("Daily Death : "+gujArray.get(gujArray.size()-1).get("data").get("newDeath") + "     (In Last 24 Hour)");

        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }


    private boolean haveNetwork(){
        boolean have_WIFI= false;
        boolean have_MobileData = false;
        boolean have_Mobile = false;
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo[] networkInfos = connectivityManager.getAllNetworkInfo();
        for(NetworkInfo info:networkInfos){
            if (info.getTypeName().equalsIgnoreCase("WIFI"))if (info.isConnected())have_WIFI=true;
            if (info.getTypeName().equalsIgnoreCase("MOBILE DATA"))if (info.isConnected())have_MobileData=true;
            if (info.getTypeName().equalsIgnoreCase("MOBILE"))if (info.isConnected())have_Mobile=true;
        }
        return have_WIFI||have_MobileData||have_Mobile;
    }
}

