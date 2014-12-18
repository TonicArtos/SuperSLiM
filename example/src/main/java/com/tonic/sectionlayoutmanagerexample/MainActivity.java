package com.tonic.sectionlayoutmanagerexample;

import com.tonic.sectionlayoutmanager.LayoutManager;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends FragmentActivity {

    private static final String TAG_COUNTRIES_FRAGMENT = "tag_countries_fragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new CountriesFragment(), TAG_COUNTRIES_FRAGMENT)
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem item = null;

        CountriesFragment countriesFragment = getCountriesFragment();

        final int headerMode = countriesFragment.getHeaderMode();
        switch (headerMode) {
            case LayoutManager.HEADER_INLINE:
                item = menu.findItem(R.id.action_header_inline);
                break;
            case LayoutManager.HEADER_ALIGN_START:
                item = menu.findItem(R.id.action_header_inline);
                break;
            case LayoutManager.HEADER_ALIGN_END:
                item = menu.findItem(R.id.action_header_inline);
                break;
            case LayoutManager.HEADER_OVERLAY_START:
                item = menu.findItem(R.id.action_header_inline);
                break;
            case LayoutManager.HEADER_OVERLAY_END:
                item = menu.findItem(R.id.action_header_inline);
                break;
        }
        if (item != null) {
            item.setChecked(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_sticky) {
            return true;
        }

        if (id == R.id.action_header_inline) {
            if (!item.isChecked()) {
                item.setChecked(true);
                updateHeaderMode(LayoutManager.HEADER_INLINE);
            }
            return true;
        }

        if (id == R.id.action_header_start) {
            if (!item.isChecked()) {
                item.setChecked(true);
                updateHeaderMode(LayoutManager.HEADER_ALIGN_START);
            }
            return true;
        }

        if (id == R.id.action_header_end) {
            if (!item.isChecked()) {
                item.setChecked(true);
                updateHeaderMode(LayoutManager.HEADER_ALIGN_END);
            }
            return true;
        }

        if (id == R.id.action_header_overlay_start) {
            if (!item.isChecked()) {
                item.setChecked(true);
                updateHeaderMode(LayoutManager.HEADER_OVERLAY_START);
            }
            return true;
        }

        if (id == R.id.action_header_overlay_end) {
            if (!item.isChecked()) {
                item.setChecked(true);
                updateHeaderMode(LayoutManager.HEADER_OVERLAY_END);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateHeaderMode(int mode) {
        CountriesFragment fragment = getCountriesFragment();
        fragment.setHeaderMode(mode);
    }

    private CountriesFragment getCountriesFragment() {
        return (CountriesFragment) getSupportFragmentManager()
                .findFragmentByTag(TAG_COUNTRIES_FRAGMENT);
    }
}
