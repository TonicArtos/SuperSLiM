package com.tonicartos.superslim.example.subsections;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.tonicartos.superslim.GridSLM;
import com.tonicartos.superslim.ItemDecorator;
import com.tonicartos.superslim.LayoutManager;
import com.tonicartos.superslim.LinearSLM;

import java.util.Random;

public class ContentFragment extends Fragment {

    private static final String KEY_HEADER_POSITIONING = "key_header_mode";

    private static final String KEY_MARGINS_FIXED = "key_margins_fixed";

    private Adapter mAdapter;

    private Random mRng = new Random();

    private RecyclerView mRecyclerView;

    private Toast mToast = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);

        ItemDecorator decor = new ItemDecorator.Builder(getActivity())
                .setDrawableBelow(R.drawable.divider_horizontal, ItemDecorator.INTERNAL)
                .decorateSlm(LinearSLM.ID)
                .decorateSlm(GridSLM.ID)
                .build();

//        mRecyclerView.addItemDecoration(decor);

        mAdapter = new Adapter(getActivity());
        LayoutManager layoutManager = new LayoutManager.Builder(getActivity())
                .addAdapter(mAdapter)
                .build();
        mRecyclerView.setLayoutManager(layoutManager);

        mRecyclerView.setAdapter(mAdapter);
    }

    public void scrollToRandomPosition() {
        int position = mRng.nextInt(mAdapter.getItemCount());
        String s = "Scroll to position " + position;
        if (mToast != null) {
            mToast.setText(s);
        } else {
            mToast = Toast.makeText(getActivity(), s, Toast.LENGTH_SHORT);
        }
        mToast.show();
        mRecyclerView.scrollToPosition(position);
    }

    public void smoothScrollToRandomPosition() {
        int position = mRng.nextInt(mAdapter.getItemCount());
        String s = "Smooth scroll to position " + position;
        if (mToast != null) {
            mToast.setText(s);
        } else {
            mToast = Toast.makeText(getActivity(), s, Toast.LENGTH_SHORT);
        }
        mToast.show();
        mRecyclerView.smoothScrollToPosition(position);
    }
}
