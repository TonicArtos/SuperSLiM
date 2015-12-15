package com.tonicartos.superslim;

import com.tonicartos.superslim.adapter.Item;
import com.tonicartos.superslim.adapter.SuperSlimAdapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

/**
 *
 */
public class AdapterStub extends SuperSlimAdapter<String, RecyclerView.ViewHolder> {

    public Util.ItemData lastBoundItem;

    public AdapterStub() {
        super();
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, @NonNull Item item) {
        lastBoundItem = (Util.ItemData) item.getData();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }
}
