package com.example.meteoapp2;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.meteoapp2.uidata.LocationPagerAdapter;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private LocationPagerAdapter pagerAdapter;
    private ArrayList<String> locationList;
    private ProgressBar progressBar;
    private static final String PREFS_NAME = "weather_prefs";
    private static final String LOCATIONS_KEY = "locations";
    private TabLayoutMediator tabMediator;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "channel1",
                    "Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Xd channel");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        progressBar = findViewById(R.id.progressBar);
        // progressBar.setVisibility(View.GONE);
        viewPager = findViewById(R.id.viewPager);
        FloatingActionButton fab = findViewById(R.id.fabAdd);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        FloatingActionButton fabMenu = findViewById(R.id.fabMenu);
        FloatingActionButton fabDelete = findViewById(R.id.fabDelete);
        FloatingActionButton fabOther = findViewById(R.id.fabOther);
        TextView infoSign1 = findViewById(R.id.InfoSign1);
        ImageView voipLogo = findViewById(R.id.voipLogo);
        CardView voipLogo2 = findViewById(R.id.voipLogoCard2);

        locationList = loadLocations();
        pagerAdapter = new LocationPagerAdapter(this, locationList);
        viewPager.setAdapter(pagerAdapter);

        if (locationList.isEmpty()) {
            infoSign1.setVisibility(View.VISIBLE);
            voipLogo.setVisibility(View.VISIBLE);
            voipLogo2.setVisibility(View.VISIBLE);
        } else {
            infoSign1.setVisibility(View.GONE);
            voipLogo.setVisibility(View.GONE);
            voipLogo2.setVisibility(View.VISIBLE);
        }

        tabMediator = new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    // tab.setText(locationList.get(position));
                } );
        tabMediator.attach();

        boolean[] isMenuOpen = {false};
        fabMenu.setOnClickListener(v -> {
            isMenuOpen[0] = !isMenuOpen[0];
            if (isMenuOpen[0]) {
                fabDelete.show();
                fabOther.show();
            } else {
                fabDelete.hide();
                fabOther.hide();
            }
        });

        fabDelete.setOnClickListener(v -> {
            int position = viewPager.getCurrentItem();
            if (locationList.size() > 1) {
                locationList.remove(position);
                saveLocations(locationList);

                pagerAdapter.updateLocations(locationList);
                tabMediator.detach();
                tabMediator = new TabLayoutMediator(tabLayout, viewPager,
                        (tab, pos) -> {
                            // tab.setText(locationList.get(pos))
                        }
                );
                tabMediator.attach();

                int newPosition = Math.min(position, locationList.size() - 1);
                viewPager.setCurrentItem(newPosition, true);
            } else {
                Toast.makeText(this, "You must have at least 1 location", Toast.LENGTH_SHORT).show();
            }
        });

        fabOther.setOnClickListener(v -> {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channel1")
                    .setSmallIcon(R.drawable.voip4)
                    .setContentTitle("VOIP ALERT!")
                    .setContentText("xddd")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.notify(1, builder.build());


            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
            View sheetView = LayoutInflater.from(this).inflate(R.layout.popup_bottom_sheet, null);
            bottomSheetDialog.setContentView(sheetView);
            bottomSheetDialog.show();
        });

        fab.setOnClickListener(v -> showAddLocationDialog()); // pop up to insert location name
    }

    private void showAddLocationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Location");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String newLocation = input.getText().toString().trim();
            if (!newLocation.isEmpty()) {
                locationList.add(newLocation);
                saveLocations(locationList);
                pagerAdapter.updateLocations(locationList);
                progressBar.setVisibility(View.VISIBLE);
                viewPager.setCurrentItem(locationList.size() - 1, true);
            }
        });
        /*  If the user clicks Add:
            The new location name is read and added to locationList.
            The list is saved to SharedPreferences and the adapter is updated (with the new list).
            The ViewPager2 moves to the new location page. */

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private ArrayList<String> loadLocations() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> locationSet = prefs.getStringSet(LOCATIONS_KEY, new LinkedHashSet<>());
        return new ArrayList<>(locationSet);
        // loads the saved location names using LinkedHashSet (to keep order)
    }

    private void saveLocations(ArrayList<String> locations) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Set<String> locationSet = new LinkedHashSet<>(locations);
        editor.putStringSet(LOCATIONS_KEY, locationSet);
        editor.apply();
    }
}
