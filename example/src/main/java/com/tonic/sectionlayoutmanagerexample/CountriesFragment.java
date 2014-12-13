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

    private ViewHolder mViews;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViews = new ViewHolder(view);
        mViews.initViews(getActivity());
        mViews.setAdapter(new CountryNamesAdapter(getActivity()));
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
