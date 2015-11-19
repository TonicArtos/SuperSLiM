package com.tonicartos.superslimdbexample;

import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;


public class MainActivity extends AppCompatActivity {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        // FIXME: Remove hack around bug in coordinator layout 23.0.0 when fix is released.
        ((CoordinatorLayout.LayoutParams) findViewById(R.id.appbar).getLayoutParams())
                .setBehavior(new AppBarLayoutBehavior());
    }

    public class AppBarLayoutBehavior extends AppBarLayout.Behavior {

        @Override
        public boolean onInterceptTouchEvent(CoordinatorLayout parent, AppBarLayout child,
                MotionEvent ev) {
            return !(parent != null && child != null && ev != null) || super
                    .onInterceptTouchEvent(parent, child, ev);
        }
    }
}
