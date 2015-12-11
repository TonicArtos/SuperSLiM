package com.tonicartos.superslimdbexample;

import com.tonicartos.superslim.adapter.Item;
import com.tonicartos.superslim.adapter.Section;
import com.tonicartos.superslim.adapter.SectionGraphAdapter;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class Adapter extends SectionGraphAdapter<String, Adapter.ViewHolder> {

    private final static int VIEW_TYPE_REGION_HEADER = 1;

    private final static int VIEW_TYPE_SUBREGION_HEADER = 2;

    private final static int VIEW_TYPE_COUNTRY_VIEW = 3;

    private Cursor mCursor;

    public Adapter() {
        super();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @NonNull Item item) {
        holder.bind(item);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        return ViewHolder.make(this, viewGroup, viewType);
    }

    public void swapCursor(Cursor data) {
        mCursor = data;
        if (mCursor == null) {
            reset();
            return;
        }

        updateGraph();
    }

    public void toggle() {
        getSection(0).toggleChildren();
        getSection(1).toggleChildren();
    }

    private void reset() {

    }

    private void updateGraph() {
        final int count = mCursor.getCount();
        mCursor.moveToFirst();
        for (int i = 0; i < count; ) {
            final String currentRegionName = mCursor.getString(1);
            final Item currentRegion = new Item(VIEW_TYPE_REGION_HEADER, currentRegionName);
            final Section regionSection = createSection(currentRegionName, currentRegion, null);

            while (i < count) {
                final String region = mCursor.getString(1);
                if (!TextUtils.equals(currentRegionName, region)) {
                    break;
                }

                final String currentSubRegionName = mCursor.getString(4);
                final Item currentSubRegion = new Item(VIEW_TYPE_SUBREGION_HEADER, currentSubRegionName);
                final Section subregionSection = createSection(currentSubRegionName, currentSubRegion, null);
                regionSection.add(subregionSection);
                for (; i < count; i++) {
                    final String subRegion = mCursor.getString(4);
                    if (!TextUtils.equals(currentSubRegionName, subRegion)) {
                        break;
                    }

                    subregionSection.add(new Item(VIEW_TYPE_COUNTRY_VIEW, mCursor.getString(7)));

                    mCursor.moveToNext();
                }
            }
            addSection(regionSection);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        protected final TextView mTextView;

        protected final Adapter mAdapter;

        protected String name;

        public ViewHolder(View itemView, Adapter adapter) {
            super(itemView);
            mTextView = (TextView) itemView.findViewById(R.id.text);
            mAdapter = adapter;
        }

        public static ViewHolder make(Adapter adapter, ViewGroup viewGroup, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
            switch (viewType) {
                case VIEW_TYPE_COUNTRY_VIEW:
                    return new CountryViewHolder(
                            inflater.inflate(R.layout.country_item, viewGroup, false), adapter);
                case VIEW_TYPE_REGION_HEADER:
                    return new RegionViewHolder(
                            inflater.inflate(R.layout.region_header, viewGroup, false), adapter);
                case VIEW_TYPE_SUBREGION_HEADER:
                    return new SubregionViewHolder(
                            inflater.inflate(R.layout.subregion_header, viewGroup, false), adapter);
                default:
                    throw new RuntimeException("Unknown view type " + viewType);

            }
        }

        public void bind(Item item) {
            Object data = item.getData();
            assert data != null;
            name = data.toString();
            mTextView.setText(name);
        }
    }

    private static class ClickableHolder extends ViewHolder implements View.OnClickListener {

        public ClickableHolder(View view, Adapter adapter) {
            super(view, adapter);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Snackbar.make(itemView,
                    "Click on item " + getAdapterPosition() + " with text " + mTextView.getText(),
                    Snackbar.LENGTH_SHORT).show();
        }
    }

    private static class RegionViewHolder extends ClickableHolder {

        public RegionViewHolder(View itemView, Adapter adapter) {
            super(itemView, adapter);
        }

        @Override
        public void onClick(View v) {
            final Section section = mAdapter.getSectionWithId(name);
            if (section != null) {
                section.toggleChildren();
                Snackbar.make(itemView,
                        (section.getCollapsed() ? "Collapsed region " : "Expanded region ") + mTextView
                                .getText(),
                        Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private static class SubregionViewHolder extends ClickableHolder {

        public SubregionViewHolder(View itemView, Adapter adapter) {
            super(itemView, adapter);
        }

        @Override
        public void onClick(View v) {
            Section section = mAdapter.getSectionWithId(name);
            if (section != null) {
                section.toggleChildren();
                Snackbar.make(itemView,
                        (section.getCollapsed() ? "Collapsed subregion " : "Expanded subregion ")
                                + mTextView.getText(),
                        Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private static class CountryViewHolder extends ClickableHolder {

        public CountryViewHolder(View itemView, Adapter adapter) {
            super(itemView, adapter);
        }

        @Override
        public void onClick(View v) {
            Snackbar.make(itemView, "Clicked on country " + mTextView.getText(),
                    Snackbar.LENGTH_SHORT)
                    .show();
        }
    }
}
