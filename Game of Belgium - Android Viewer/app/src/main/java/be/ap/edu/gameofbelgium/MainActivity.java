package be.ap.edu.gameofbelgium;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.*;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.TextView;

import org.json.*;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.*;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import com.android.volley.*;
import com.android.volley.toolbox.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {
    final String PACKAGE_TAG = "be.ap.edu";
    final String VIEW_URL = "http://jimbauwens.cloudant.com/ejustice/_design/views/_view/locationByDate";
    final String STOP_SPOT_MARKER = "einde";

    final String START_DATE = "2014-01-01";
    final String END_DATE = "2015-01-01";

    final int ROWS_PER_REQUEST = 30;
    final int INITIAL_ZOOM = 9;
    final double START_POINT_LAT = 51.5244;
    final double START_POINT_LON = 3.35;

    private MapView mapView;
    private TextView mDatumLabel;
    private TextView mStartCounter;
    private TextView mEindeCounter;
    private MenuItem playButton;
    private MenuItem pauseButton;

    private String currentDate;
    private int startCounter;
    private int eindeCounter;
    private boolean playing;

    private RequestQueue mRequestQueue;
    private ResourceProxy mResourceProxy;
    private DotOverlay myDots;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    private void initGUI() {
        mDatumLabel = (TextView)findViewById(R.id.datumLabel);
        mStartCounter = (TextView)findViewById(R.id.startCounter);
        mEindeCounter = (TextView)findViewById(R.id.eindeCounter);

        mapView = (MapView)findViewById(R.id.mapview);
        mapView.setTileSource(TileSourceFactory.MAPQUESTOSM);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(INITIAL_ZOOM);
        mapView.getController().setCenter(new GeoPoint(START_POINT_LAT, START_POINT_LON));
    }

    private void resetOverlay(boolean newOverlay) {
        if (newOverlay) {
            if (myDots != null)
                mapView.getOverlays().remove(myDots);

            myDots = new DotOverlay(mResourceProxy, mapView);
        }

        mapView.getOverlays().add(myDots);
        mapView.invalidate();
    }

    private void resetGame() {
        startCounter = 0;
        eindeCounter = 0;
        currentDate  = START_DATE;
        playing = false;

        showStats();
    }

    private void showStats(){
        mDatumLabel.setText(currentDate);
        mEindeCounter.setText(Integer.toString(eindeCounter));
        mStartCounter.setText(Integer.toString(startCounter));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mResourceProxy = new DefaultResourceProxyImpl(getApplicationContext());
        mRequestQueue =  Volley.newRequestQueue(this);

        initGUI();
        resetGame();
        resetOverlay(true);
    }

    public String addDaysToDate(String date, int days) {
        Calendar c = Calendar.getInstance();

        try {
            c.setTime(sdf.parse(date));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        c.add(Calendar.DATE, days);
        return sdf.format(c.getTime());
    }

    public void renderDay(final String date) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDatumLabel.setText(date);
            }
        });

        renderDay(date, 0);
    }

    public void updateStats() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mEindeCounter.setText(Integer.toString(eindeCounter));
                mStartCounter.setText(Integer.toString(startCounter));
            }
        });
    }

    public void renderDay(final String date, final int skip) {
        JsonObjectRequest jr = new JsonObjectRequest(VIEW_URL + "?key=%22" + date + "%22&limit="+ROWS_PER_REQUEST+"&skip="+skip, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray rows = response.getJSONArray("rows");

                    for (int i=0; i<rows.length(); i++) {
                        JSONObject row = rows.getJSONObject(i);
                        JSONArray rowData = row.getJSONArray("value");

                        if (!rowData.isNull(0)) {
                            JSONObject point = rowData.getJSONObject(0);
                            boolean einde = rowData.getString(1).equals(STOP_SPOT_MARKER);
                            int color = einde ? Color.RED : Color.GREEN;

                            if (einde)
                                eindeCounter++;
                            else
                                startCounter++;


                            myDots.addPoint(new GeoPoint(point.getDouble("lat"), point.getDouble("lon")), color);

                            updateStats();
                            mapView.invalidate();
                        }
                    }

                    if (rows.length() > 0) {
                        renderDay(date, skip + ROWS_PER_REQUEST);
                    } else {
                        mapView.invalidate();
                        tickDay();
                    }


                }
                catch(JSONException ex) {
                    Log.e(PACKAGE_TAG, ex.getMessage());
                }
            }
        },new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String message = error.getMessage();
                Log.e(PACKAGE_TAG, message != null ? message : "no error specified.. :(");
            }
        });

        mRequestQueue.add(jr);
    }

    public void tickDay() {
        Log.i(PACKAGE_TAG, currentDate);

        if (playing && !currentDate.equals(END_DATE)){
            renderDay(currentDate);
            currentDate = addDaysToDate(currentDate, 1);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_main);
        initGUI();
        resetOverlay(false);
        showStats();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        playButton = menu.findItem(R.id.action_play);
        pauseButton = menu.findItem(R.id.action_pause);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_play:
                playing = true;

                playButton.setVisible(false);
                pauseButton.setVisible(true);
                tickDay();

                return true;

            case R.id.action_pause:
                playing = false;

                pauseButton.setVisible(false);
                playButton.setVisible(true);

                return true;

            case R.id.action_reset:
                resetGame();
                resetOverlay(true);

                playButton.setVisible(true);
                pauseButton.setVisible(false);

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

}