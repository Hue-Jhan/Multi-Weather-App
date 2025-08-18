package com.example.meteoapp2.uidata;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.List;

public class LocationPagerAdapter extends FragmentStateAdapter {

    private List<String> locations;

    public LocationPagerAdapter(@NonNull FragmentActivity fragmentActivity, List<String> locations) {
        super(fragmentActivity);
        this.locations = new ArrayList<>(locations);
    }

    public List<String> getLocations() {
        return locations;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        String location = locations.get(position);
        return LocationFragment.newInstance(location);
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    public void updateLocations(List<String> newLocations) {
        this.locations = new ArrayList<>(newLocations);
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        locations.remove(position);
        // notifyItemRemoved(position);
        // notifyItemRangeChanged(position, locations.size());
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        // Give each location a stable ID
        return locations.get(position).hashCode();
    }

    @Override
    public boolean containsItem(long itemId) {
        for (String location : locations) {
            if (location.hashCode() == itemId) {
                return true;
            }
        }
        return false;
    }

}
