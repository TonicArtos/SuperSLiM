package com.tonicartos.superslim;

import com.tonicartos.superslim.adapter.Item;
import com.tonicartos.superslim.adapter.SectionGraphAdapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

/**
 *
 */
public class AdapterStub extends SectionGraphAdapter<String, RecyclerView.ViewHolder> {

    public Util.ItemData lastBoundItem;

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, @NonNull Item item) {
        lastBoundItem = (Util.ItemData) item.getData();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }
}
