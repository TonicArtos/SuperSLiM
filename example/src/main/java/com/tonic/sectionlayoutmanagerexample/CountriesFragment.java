package com.tonic.sectionlayoutmanagerexample;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tonic.sectionlayoutmanager.LayoutManager;
import com.tonic.sectionlayoutmanager.LinearSectionLayoutManager;
import com.tonic.sectionlayoutmanager.SectionLayoutManager;

/**
 * Fragment that displays a list of country names.
 */
public class CountriesFragment extends Fragment {

    private static final java.lang.String KEY_HEADER_MODE = "key_header_mode";

    private ViewHolder mViews;

    private CountryNamesAdapter mAdapter;

    private int mHeaderMode;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            mHeaderMode = savedInstanceState.getInt(KEY_HEADER_MODE, LayoutManager.HEADER_INLINE);
        } else {
            mHeaderMode = LayoutManager.HEADER_INLINE;
        }


        mViews = new ViewHolder(view);
        mViews.initViews(getActivity());
        mAdapter = new CountryNamesAdapter(getActivity(), mHeaderMode);
        mViews.setAdapter(mAdapter);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_HEADER_MODE, mHeaderMode);
    }

    public void setHeaderMode(int mode) {
        mHeaderMode = mode;
        mAdapter.setHeaderMode(mode);
    }

    public int getHeaderMode() {
        return mHeaderMode;
    }

    private static class ViewHolder {

        private final RecyclerView mRecyclerView;

        private LayoutManager.SlmFactory mSlmFactory = new LayoutManager.SlmFactory() {
            @Override
            public SectionLayoutManager getSectionLayoutManager(int section) {
                return new LinearSectionLayoutManager();
            }
        };

        public ViewHolder(View view) {
            mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        }

        public void initViews(Context context) {
            LayoutManager lm = new LayoutManager(context);
            lm.setSlmFactory(mSlmFactory);
            mRecyclerView.setLayoutManager(lm);
        }

        public void setAdapter(RecyclerView.Adapter<?> adapter) {
            mRecyclerView.setAdapter(adapter);
        }
    }
}
