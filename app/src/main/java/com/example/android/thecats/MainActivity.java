package com.example.android.thecats;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.example.android.thecats.response.Category;
import com.example.android.thecats.response.Image;
import com.example.android.thecats.response.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends ActionBarActivity {
    private RecyclerViewAdapter adapter;
    private RecyclerView recyclerView;
    private String[] mPlanetTitles;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle actionBarDrawerToggle;

    private ConnectionReceiver connectionReceiver = new ConnectionReceiver();
    private boolean connected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.registerReceiver(connectionReceiver, new IntentFilter(
                "android.net.conn.CONNECTIVITY_CHANGE"));
        tryConnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(connectionReceiver);
    }


    public void tryConnect() {
        if (Utils.isOnline(this) && !connected) {
            prepareMainActivity();
            prepareNavigationDrawer();
            configToolBar();
            connected = true;
        }

    }

    private boolean loadMore(RecyclerViewAdapter adapter, int count) {
        if (!Utils.isOnline(this) || adapter == null) return false;
        ArrayList<Image> createLists = loadFirst(count, null);
        return adapter.addItems(createLists);
    }

    private void prepareNavigationDrawer() {
//        mPlanetTitles = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15"};
        mPlanetTitles = loadCategories();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);


        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, mPlanetTitles));

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Toast.makeText(MainActivity.this, "Category " + mPlanetTitles[i], Toast.LENGTH_SHORT).show();
                ArrayList<Image> images = loadFirst(50, mPlanetTitles[i]);
                adapter.setGalleryList(images);
                recyclerView.scrollToPosition(0);
                mDrawerLayout.closeDrawers();
            }
        });

    }

    private class LoadCategoriesTask extends AsyncTask<Void, Void, retrofit2.Response<Response>> {

        @Override
        protected retrofit2.Response<Response> doInBackground(Void... voids) {
            retrofit2.Response<Response> response = null;
            try {
                response = App.getApi().getCategories().execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }
    }

    private String[] loadCategories() {
        Response response = null;
        LoadCategoriesTask t = new LoadCategoriesTask();
        t.execute();
//        System.out.println(App.getApi().getCategories().request().toString());
        try {
            retrofit2.Response<Response> rez = t.get();
            if (rez == null)
                Toast.makeText(this, "Categories Load Failed", Toast.LENGTH_LONG).show();
            else {
                Toast.makeText(this, "Categories Load Success", Toast.LENGTH_LONG).show();
                response = rez.body();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        List<String> strings = new ArrayList<>();

        for (Category c : response.getData().getCategories().getCategory()) {
            strings.add(c.getName());
        }

        String[] stockArr = new String[strings.size()];
        stockArr = strings.toArray(stockArr);

        return stockArr;
    }

    private void configToolBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.categ);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        getSupportActionBar().setHomeButtonEnabled(true);

        actionBarDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.app_name, R.string.app_name);
    }

    private void prepareMainActivity() {
        recyclerView = (RecyclerView) findViewById(R.id.imagegallery);
        recyclerView.setHasFixedSize(true);

        final RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getApplicationContext(), 3);
        recyclerView.setLayoutManager(layoutManager);
        ArrayList<Image> createLists = loadFirst(50, null);

        adapter = new RecyclerViewAdapter(getApplicationContext(), createLists);
        recyclerView.setAdapter(adapter);

        adapter.setClickListener(new RecyclerClickListener() {
            @Override
            public void onClick(View v) {
                Image img = adapter.getItem(getPos());

                Intent intent = new Intent(MainActivity.this, DetailsActivity.class);
                intent.putExtra("id", img.getId() + " " + getPos());
                intent.putExtra("url", img.getUrl());

                //Start details activity
                startActivity(intent);
            }
        });

        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                GridLayoutManager layoutManager = ((GridLayoutManager) recyclerView.getLayoutManager());

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();
                if (pastVisibleItems + visibleItemCount >= totalItemCount) {
                    //End of list
                    loadMore(adapter, 50);
//                    Toast.makeText(MainActivity.this, "Adding", Toast.LENGTH_LONG).show();
                }

            }
        });
    }

    private class LoadImagesTask extends AsyncTask<String, Void, retrofit2.Response<com.example.android.thecats.response.Response>> {

        @Override
        protected retrofit2.Response<com.example.android.thecats.response.Response> doInBackground(String... strings) {
            retrofit2.Response<com.example.android.thecats.response.Response> response = null;
            try {
                if (strings.length < 2) throw new IOException();
                if (strings[0] == null)
                    response = App.getApi().getData(Integer.parseInt(strings[1])).execute();
                else
                    response = App.getApi().getData(Integer.parseInt(strings[1]), strings[0]).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }
    }

    private ArrayList<Image> loadFirst(int count, String category) {
        com.example.android.thecats.response.Response response = null;
        LoadImagesTask t = new LoadImagesTask();
        t.execute(category, String.valueOf(count));
        try {
            retrofit2.Response<com.example.android.thecats.response.Response> rez = t.get();
            if (rez == null) Toast.makeText(this, "Error loading", Toast.LENGTH_LONG).show();
            else {
//                Toast.makeText(this, "Loaded", Toast.LENGTH_LONG).show();
                response = rez.body();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        ArrayList<Image> images = new ArrayList<>();
        if (response != null)
            images.addAll(response.getData().getImages().getImages());

        return images;
    }

}
