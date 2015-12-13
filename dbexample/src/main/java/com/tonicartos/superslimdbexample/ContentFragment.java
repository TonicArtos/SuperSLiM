package com.tonicartos.superslimdbexample;

import com.tonicartos.superslim.SuperSlim;
import com.tonicartos.superslim.adapter.Section;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;


/**
 * A placeholder fragment containing a simple view.
 */
public class ContentFragment extends Fragment {

    private static final int LOADER = 0;

    private static final Uri URI = Uri
            .parse("content://com.tonicartos.superslimdbexample.provider/all");

    Adapter mAdapter;

    private LoaderManager.LoaderCallbacks<Cursor> mLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new CursorLoader(getActivity(), URI, null, null, null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mAdapter.swapCursor(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mAdapter.swapCursor(null);
        }
    };

    private Section removedSection = null;

    public ContentFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getLoaderManager().restartLoader(LOADER, null, mLoaderCallbacks);

        setHasOptionsMenu(true);

        mAdapter = new Adapter();

        RecyclerView rv = (RecyclerView) view.findViewById(R.id.recycler_view);
        rv.setLayoutManager(new LinearInterceptManager(getActivity()));
        rv.setAdapter(mAdapter);

        SuperSlim a = new SuperSlim();

//        SuperSLiM.Helper sslmHelper = new SuperSLiM.Helper(getActivity())
//                .setAdapter(mAdapter)
//                .setRecyclerView((RecyclerView) view.findViewById(R.id.recycler_view))
//                .init();
//
//        sslmHelper.getRecyclerView();
//        sslmHelper.getAdapter();
//        sslmHelper.getLayoutManager();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add("sadf");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        if (removedSection == null) {
//            removedSection = mAdapter.removeSection(1);
//        } else {
//            mAdapter.insertSection(1, removedSection);
//            removedSection = null;
//        }
        mAdapter.moveSection(0, 1);
        return false;
    }
}
