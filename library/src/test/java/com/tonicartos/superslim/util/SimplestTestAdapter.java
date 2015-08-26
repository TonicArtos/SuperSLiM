package com.tonicartos.superslim.util;

import com.tonicartos.superslim.GridSLM;
import com.tonicartos.superslim.LayoutManager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class SimplestTestAdapter extends RecyclerView.Adapter<SimplestTestAdapter.TestViewHolder> {

    private final Context mContext;

    private List<Item> mItems = new ArrayList<>();

    public SimplestTestAdapter(Context context) {
        mContext = context;
    }

    public void addItem(Item item) {
        mItems.add(item);
    }

    @Override
    public TestViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new TestViewHolder(mItems, new DummyView(mContext));
    }

    @Override
    public void onBindViewHolder(TestViewHolder holder, int position) {
        holder.bindItem(position);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public static class Item {

        public int sectionStart;

        public int width;

        public int height;

        public LayoutManager.LayoutParams referenceParams = new LayoutManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        public void configureView(View v) {
            v.setMinimumHeight(height);
            v.setMinimumWidth(width);
            LayoutManager.LayoutParams params = GridSLM.LayoutParams.from(referenceParams);
            params.setFirstPosition(sectionStart);
            v.setLayoutParams(params);
        }
    }

    public static class HeaderItem extends Item {

        public int flags;

        public HeaderItem(int flags) {
            this.flags = flags;
        }

        @Override
        public void configureView(View v) {
            super.configureView(v);
            LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) v.getLayoutParams();
            params.headerDisplay = flags;
            params.isHeader = true;
            v.setLayoutParams(params);
        }
    }

    public static class TestViewHolder extends RecyclerView.ViewHolder {

        private final List<Item> mItems;

        public TestViewHolder(List<Item> items, View itemView) {
            super(itemView);
            mItems = items;
        }

        public void bindItem(final int position) {
            Item item = mItems.get(position);
            item.configureView(itemView);
        }
    }

    @SuppressLint("ViewConstructor")
    private static class DummyView extends View {

        public DummyView(Context context) {
            super(context);
        }
    }
}
