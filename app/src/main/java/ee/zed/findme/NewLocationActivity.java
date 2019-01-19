package ee.zed.findme;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import lombok.val;

public class NewLocationActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_REPLY = "ee.zed.findme.newlocation";

    private EditText mEditWordView;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_location);
        mEditWordView = findViewById(R.id.edit_word);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.new_location_map);
        mapFragment.getMapAsync(this);

        final Button button = findViewById(R.id.button_save);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent replyIntent = new Intent();
                if (TextUtils.isEmpty(mEditWordView.getText())) {
                    setResult(RESULT_CANCELED, replyIntent);
                } else {
                    String word = mEditWordView.getText().toString();
                    replyIntent.putExtra(EXTRA_REPLY, word);
                    setResult(RESULT_OK, replyIntent);
                }
                finish();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        val extras = getIntent().getExtras();
        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(extras.getDouble("lat", 68), extras.getDouble("lng", 54));
        mMap.addMarker(new MarkerOptions().position(sydney).title(extras.getString("title", "new location")));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 18.0f));
    }
}

