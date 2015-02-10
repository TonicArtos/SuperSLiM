package com.tonicartos.superslimexample;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.tonicartos.superslim.GridSectionLayoutManager;
import com.tonicartos.superslim.LayoutManager;
import com.tonicartos.superslim.LinearSectionLayoutManager;
import com.tonicartos.superslim.SectionLayoutManager;

import java.util.Random;

/**
 * Fragment that displays a list of country names.
 */
public class CountriesFragment extends Fragment {

    private static final String KEY_HEADER_POSITIONING = "key_header_mode";

    private static final String KEY_MARGINS_FIXED = "key_margins_fixed";

    private ViewHolder mViews;

    private CountryNamesAdapter mAdapter;

    private int mHeaderDisplay;

    private boolean mAreMarginsFixed;

    private Random mRng = new Random();

    private Toast mToast = null;

    private GridSectionLayoutManager mGridSectionLayoutManager;

    private SectionLayoutManager mLinearSectionLayoutManager;

    private LayoutManager.SlmFactory mSlmFactory = new LayoutManager.SlmFactory() {

        @Override
        public SectionLayoutManager getSectionLayoutManager(LayoutManager layoutManager,
                int section) {
            int sectionKind = section % 2;
            final SectionLayoutManager slm;
            if (sectionKind == 0) {
                GridSectionLayoutManager grid = mGridSectionLayoutManager;
                slm = mGridSectionLayoutManager;
            } else {
                slm = mLinearSectionLayoutManager;
            }
            return slm;
        }
    };

    public boolean areHeadersOverlaid() {
        return (mHeaderDisplay & LayoutManager.LayoutParams.HEADER_OVERLAY) != 0;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            mHeaderDisplay = savedInstanceState
                    .getInt(KEY_HEADER_POSITIONING,
                            getResources().getInteger(R.integer.default_header_display));
            mAreMarginsFixed = savedInstanceState
                    .getBoolean(KEY_MARGINS_FIXED,
                            getResources().getBoolean(R.bool.default_margins_fixed));
        } else {
            mHeaderDisplay = getResources().getInteger(R.integer.default_header_display);
            mAreMarginsFixed = getResources().getBoolean(R.bool.default_margins_fixed);
        }

        LayoutManager lm = new LayoutManager();
        mGridSectionLayoutManager = new GridSectionLayoutManager(lm, getActivity());
        mGridSectionLayoutManager.setColumnMinimumWidth((int) getResources()
                .getDimension(R.dimen.grid_column_width));
        mLinearSectionLayoutManager = new LinearSectionLayoutManager(lm);
        lm.setSlmFactory(mSlmFactory);

        mViews = new ViewHolder(view);
        mViews.initViews(getActivity(), lm);
        mAdapter = new CountryNamesAdapter(getActivity(), mHeaderDisplay);
        mAdapter.setMarginsFixed(mAreMarginsFixed);
        mAdapter.setHeaderDisplay(mHeaderDisplay);
        mViews.setAdapter(mAdapter);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_HEADER_POSITIONING, mHeaderDisplay);
        outState.putBoolean(KEY_MARGINS_FIXED, mAreMarginsFixed);
    }

    public void setHeaderMode(int mode) {
        mHeaderDisplay = mode | (mHeaderDisplay & LayoutManager.LayoutParams.HEADER_OVERLAY) | (mHeaderDisplay & LayoutManager.LayoutParams.HEADER_STICKY);
        mAdapter.setHeaderDisplay(mHeaderDisplay);
    }

    public int getHeaderMode() {
        return mHeaderDisplay;
    }

    public boolean areHeadersSticky() {
        return (mHeaderDisplay & LayoutManager.LayoutParams.HEADER_STICKY) != 0;
    }

    public boolean areMarginsFixed() {
        return mAreMarginsFixed;
    }

    public void setHeadersOverlaid(boolean areHeadersOverlaid) {
        mHeaderDisplay = areHeadersOverlaid ? mHeaderDisplay | LayoutManager.LayoutParams.HEADER_OVERLAY : mHeaderDisplay & ~LayoutManager.LayoutParams.HEADER_OVERLAY;
        mAdapter.setHeaderDisplay(mHeaderDisplay);
    }

    public void setHeadersSticky(boolean areHeadersSticky) {
        mHeaderDisplay = areHeadersSticky ? mHeaderDisplay | LayoutManager.LayoutParams.HEADER_STICKY : mHeaderDisplay & ~LayoutManager.LayoutParams.HEADER_STICKY;
        mAdapter.setHeaderDisplay(mHeaderDisplay);
    }

    public void setMarginsFixed(boolean areMarginsFixed) {
        mAreMarginsFixed = areMarginsFixed;
        mAdapter.setMarginsFixed(areMarginsFixed);
    }

    public void scrollToRandomPosition() {
        int position = mRng.nextInt(mAdapter.getItemCount());
        String s = "Scroll to position " + position
                + (mAdapter.isItemHeader(position) ? ", header " : ", item ")
                + mAdapter.itemToString(position) + ".";
        if (mToast != null) {
            mToast.setText(s);
        } else {
            mToast = Toast.makeText(getActivity(), s, Toast.LENGTH_SHORT);
        }
        mToast.show();
        mViews.scrollToPosition(position);
    }

    public void smoothScrollToRandomPosition() {
        int position = mRng.nextInt(mAdapter.getItemCount());
        String s = "Smooth scroll to position " + position
                + (mAdapter.isItemHeader(position) ? ", header " : ", item ")
                + mAdapter.itemToString(position) + ".";
        if (mToast != null) {
            mToast.setText(s);
        } else {
            mToast = Toast.makeText(getActivity(), s, Toast.LENGTH_SHORT);
        }
        mToast.show();
        mViews.smoothScrollToPosition(position);
    }

    private static class ViewHolder {

        private final RecyclerView mRecyclerView;


        public ViewHolder(View view) {
            mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        }

        public void initViews(Context context, LayoutManager lm) {
            mRecyclerView.setLayoutManager(lm);
        }

        public void setAdapter(RecyclerView.Adapter<?> adapter) {
            mRecyclerView.setAdapter(adapter);
        }

        public void scrollToPosition(int position) {
            mRecyclerView.scrollToPosition(position);
        }

        public void smoothScrollToPosition(int position) {
            mRecyclerView.smoothScrollToPosition(position);
        }
    }
}
