package com.tonicartos.superslimexample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.tonicartos.superslim.LayoutManager;
import com.tonicartos.superslimexample.DataDiffer.LoadFragment;

/**
 * Created by hesk on 26/8/15.
 */
public class LoadingDataActivity extends AppCompatActivity {
    private static final String TAG_COUNTRIES_FRAGMENT = "tag_countries_fragment";

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem item = null;

        LoadFragment countriesFragment = getCountriesFragment();
        final int headerMode = countriesFragment.getHeaderMode();
        if (headerMode == LayoutManager.LayoutParams.HEADER_INLINE) {
            item = menu.findItem(R.id.action_header_inline);

        } else if ((headerMode & LayoutManager.LayoutParams.HEADER_ALIGN_START)
                == LayoutManager.LayoutParams.HEADER_ALIGN_START) {
            item = menu.findItem(R.id.action_header_start);

        } else if ((headerMode & LayoutManager.LayoutParams.HEADER_ALIGN_END)
                == LayoutManager.LayoutParams.HEADER_ALIGN_END) {
            item = menu.findItem(R.id.action_header_end);
        }

        if (item != null) {
            item.setChecked(true);
        }

        menu.findItem(R.id.action_overlay).setChecked(countriesFragment.areHeadersOverlaid());
        menu.findItem(R.id.action_sticky).setChecked(countriesFragment.areHeadersSticky());
        menu.findItem(R.id.action_fixed_margins).setChecked(countriesFragment.areMarginsFixed());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        boolean checked = item.isChecked();
        if (id == R.id.action_overlay) {
            LoadFragment f = getCountriesFragment();
            f.setHeadersOverlaid(!checked);
            item.setChecked(!checked);
            return true;
        }

        if (id == R.id.action_sticky) {
            LoadFragment f = getCountriesFragment();
            f.setHeadersSticky(!checked);
            item.setChecked(!checked);
            return true;
        }

        if (id == R.id.action_fixed_margins) {
            LoadFragment f = getCountriesFragment();
            f.setMarginsFixed(!checked);
            item.setChecked(!checked);
            return true;
        }

        if (id == R.id.action_header_inline) {
            if (!checked) {
                item.setChecked(true);
                updateHeaderMode(LayoutManager.LayoutParams.HEADER_INLINE);
            }
            return true;
        }

        if (id == R.id.action_header_start) {
            if (!checked) {
                item.setChecked(true);
                updateHeaderMode(LayoutManager.LayoutParams.HEADER_ALIGN_START);
            }
            return true;
        }

        if (id == R.id.action_header_end) {
            if (!checked) {
                item.setChecked(true);
                updateHeaderMode(LayoutManager.LayoutParams.HEADER_ALIGN_END);
            }
            return true;
        }

        if (id == R.id.action_random_scroll) {
            getCountriesFragment().scrollToRandomPosition();
        }

        if (id == R.id.action_random_smooth_scroll) {
            getCountriesFragment().smoothScrollToRandomPosition();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new LoadFragment(), TAG_COUNTRIES_FRAGMENT)
                    .commit();
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
    }

    private LoadFragment getCountriesFragment() {
        return (LoadFragment) getSupportFragmentManager().findFragmentByTag(TAG_COUNTRIES_FRAGMENT);
    }

    private void updateHeaderMode(int mode) {
        LoadFragment fragment = getCountriesFragment();
        fragment.setHeaderMode(mode);
    }
}
