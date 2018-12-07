package ee.zed.findme;

import android.content.Intent;
import android.graphics.Movie;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ee.zed.findme.model.LocationModel;

public class MainActivity extends AppCompatActivity {

    private List<LocationModel> locationList = new ArrayList<>();
    private RecyclerView recyclerView;
    private LocationAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               // Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
               //         .setAction("Action", null).show();
                Intent showMap = new Intent(getApplicationContext(), MapsActivity.class);
                showMap.putExtra("new", true);
                startActivity(showMap);
            }
        });

        recyclerView = findViewById(R.id.items);

        mAdapter = new LocationAdapter(locationList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

        recyclerView.setAdapter(mAdapter);
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                LocationModel location = locationList.get(position);
                Intent showMap = new Intent(getApplicationContext(), MapsActivity.class);
                showMap.putExtra("new", false);
                showMap.putExtra("title", location.name);
                showMap.putExtra("lat", location.lat);
                showMap.putExtra("lng", location.lng);
                startActivity(showMap);
            }

            @Override
            public void onLongClick(View view, int position) {
                TextView textView = view.findViewById(R.id.title);
                textView.setCursorVisible(true);
                textView.setFocusableInTouchMode(true);
                textView.setInputType(InputType.TYPE_CLASS_TEXT);
                textView.requestFocus(); //to trigger the soft input
            }
        }));

        prepareLocationData();
    }

    private void prepareLocationData() {
        LocationModel loc_1 = LocationModel.builder().name("Kaarsild, raekoja pool").lat(58.380665).lng(26.725371).build();
        locationList.add(loc_1);
        LocationModel loc_2 = LocationModel.builder().name("Kaarsild, ülejõe").lat(58.380955).lng(26.726559).build();
        locationList.add(loc_2);
        LocationModel loc_3 = LocationModel.builder().name("Tartu Saksa Kultuuri Instituut").lat(58.378937).lng(26.709121).build();
        locationList.add(loc_3);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
