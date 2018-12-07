package ee.zed.findme;

import android.graphics.Color;
import android.graphics.Movie;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import ee.zed.findme.model.LocationModel;
import lombok.val;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.MyViewHolder> {

    List<LocationModel> locationList;

    public LocationAdapter(List<LocationModel> locationList) {
        this.locationList = locationList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.location_list_row, viewGroup, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder myViewHolder, int i) {
        val location = locationList.get(i);
        myViewHolder.title.setText(location.getName());
        myViewHolder.lat_lng.setText(String.format("%1$,.5f, %2$,.5f", location.getLat(), location.getLng() ));
        myViewHolder.distance.setText(String.format("%1$,.2f m", location.distance ));
        myViewHolder.itemView.setBackgroundColor(location.isInFence() ? Color.GREEN : Color.TRANSPARENT);
    }


    @Override
    public int getItemCount() {
        return locationList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView title;
        public TextView lat_lng;
        public TextView distance;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            title =  itemView.findViewById(R.id.title);
            lat_lng =  itemView.findViewById(R.id.lat_lng);
            distance =  itemView.findViewById(R.id.distance);
        }
    }
}
