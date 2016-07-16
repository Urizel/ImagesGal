package com.spb.kbv.imagesgal;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.spb.kbv.imagesgal.Animation.DepthPageTransformer;
import com.spb.kbv.imagesgal.Animation.ZoomOutPageTransformer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements ImageFragment.OnImageClickEventListener {

    private static String CURRENT_IMAGE = "CURRENT_IMAGE";

    private ZoomViewPager pager;
    private List<Image> imageList;
    private int currentPosition;
    private Timer timer;
    private boolean isAutoPlayEnabled;
    private ActionBar actionBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(null);
            actionBar.hide();
        }

        //Hide status bar
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //Load list of images
        imageList = Image.listAll(Image.class);
        if (imageList.size() == 0) {
            loadFromJSON();
        }

        pager = (ZoomViewPager)findViewById(R.id.pager);
        PagerAdapter mPagerAdapter = new MyFragmentPagerAdapter(getSupportFragmentManager());
        pager.setAdapter(mPagerAdapter);

        //Feature for stable work in case of swiping while autoplay
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                currentPosition = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        //Load and setup settings from preferences file
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        currentPosition = sp.getInt(CURRENT_IMAGE, 0);
        pager.setCurrentItem(currentPosition);

        boolean showOnlyFavorite = sp.getBoolean(getString(R.string.prefs_only_favorite_key), false);
        if (showOnlyFavorite) {
            imageList = Image.findWithQuery(Image.class, "Select * from Image where favorite = ?",
                    "1");
            mPagerAdapter.notifyDataSetChanged();
            if (imageList.size() == 0) {
                TextView textView = (TextView) findViewById(R.id.acivity_main_textView);
                textView.setVisibility(View.VISIBLE);
                actionBar.show();
            }
        }

        String showOrder = sp.getString(getString(R.string.prefs_show_order_key), "1");

        //Make random show
        if (showOrder.equals("2")) {
            Collections.shuffle(imageList);
            mPagerAdapter.notifyDataSetChanged();
        }

        if (showOnlyFavorite) {
            for (int i = (imageList.size() - 1); i >= 0; i--){
                if (!imageList.get(i).isFavorite()){
                    imageList.remove(i);
                }
            }
        }

        String animation = sp.getString(getString(R.string.prefs_animation_key), "1");
        switch (animation){
            case "1":
                pager.setPageTransformer(true, new DepthPageTransformer());
                break;
            case "2":
                pager.setPageTransformer(true, new ZoomOutPageTransformer());
        }

        isAutoPlayEnabled = sp.getBoolean(getString(R.string.prefs_autoplay_key), false);
        if (isAutoPlayEnabled) {
            attachTimer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Save current show image position
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(CURRENT_IMAGE, pager.getCurrentItem()).apply();
    }

    @Override
    public void onTimerPause() {
        if (timer != null){
            timer.cancel();
        }
    }

    @Override
    public void onTimerResume() {
        if (isAutoPlayEnabled)
            attachTimer();
    }

    @Override
    public void showActionbBar() {
        if (actionBar.isShowing()){
            actionBar.hide();
        } else {
            actionBar.show();
        }
    }


    private class MyFragmentPagerAdapter extends FragmentPagerAdapter{

        public MyFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return ImageFragment.newInstance(imageList.get(position));
        }

        @Override
        public int getCount() {
            return imageList.size();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        startActivityForResult(new Intent(this, SettingsActivity.class), 1);
        return true;
    }

    //Attach timer if autoplay enabled
    private void attachTimer(){
        int autoPlayFrequency = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this)
                .getString(getString(R.string.prefs_autoplay_frequency_key), "3"));
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (currentPosition <= 4) {
                            pager.setCurrentItem(currentPosition);
                            currentPosition++;
                        } else {
                            currentPosition = 0;
                            pager.setCurrentItem(currentPosition);
                        }
                    }
                });
            }
        }, 500, autoPlayFrequency * 1000);
    }

    //Load images list from JSON file
    private void loadFromJSON() {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        String jsonFile = loadJSONFromAsset();
        Image[] images = gson.fromJson(jsonFile, Image[].class);
        //Make Images database with Sugar ORM
        for (Image image : images){
            image.save();
        }
        imageList = Image.listAll(Image.class);
    }

    //Load JSON file from assets directory
    public String loadJSONFromAsset() {
        String json = null;
        try {
            InputStream is = getAssets().open("trains.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) {
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }
    }
}
