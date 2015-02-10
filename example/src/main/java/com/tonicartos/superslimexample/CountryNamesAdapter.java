package com.tonicartos.superslimexample;

import com.tonicartos.superslim.LayoutManager;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

/**
 *
 */
public class CountryNamesAdapter extends RecyclerView.Adapter<CountryViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0x01;

    private static final int VIEW_TYPE_CONTENT = 0x00;

    private final Context mContext;

    private final ArrayList<LineItem> mItems;

    private int mHeaderDisplay;

    private boolean mMarginsFixed;

    public CountryNamesAdapter(Context context, int headerMode) {
        final String[] countryNames = context.getResources().getStringArray(R.array.country_names);
        mHeaderDisplay = headerMode;

        mItems = new ArrayList<>();

        //Insert headers into list of items.
        String lastHeader = "";
        int sectionCount = 0;
        int headerCount = 0;
        int sectionFirstPosition = 0;
        for (int i = 0; i < countryNames.length; i++) {
            String header = countryNames[i].substring(0, 1);
            if (!TextUtils.equals(lastHeader, header)) {
                sectionCount += 1;
                sectionFirstPosition = i + headerCount;
                lastHeader = header;
                headerCount += 1;
                mItems.add(new LineItem(header, true, sectionCount, sectionFirstPosition));
            }
            mItems.add(new LineItem(countryNames[i], false, sectionCount, sectionFirstPosition));
        }
        mContext = context;
    }

    public boolean isItemHeader(int position) {
        return mItems.get(position).isHeader;
    }

    public String itemToString(int position) {
        return mItems.get(position).text;
    }

    @Override
    public CountryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TextView view;
        if (viewType == VIEW_TYPE_HEADER) {
            view = (TextView) LayoutInflater.from(mContext).inflate(
                    R.layout.header_item, parent, false);
        } else {
            view = (TextView) LayoutInflater.from(mContext)
                    .inflate(R.layout.text_line_item, parent, false);
        }
        return new CountryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CountryViewHolder holder, int position) {
        final LineItem item = mItems.get(position);
        final View itemView = holder.itemView;

        holder.bindItem(item.text);

        final LayoutManager.LayoutParams lp = (LayoutManager.LayoutParams) itemView
                .getLayoutParams();
        // Overrides xml attrs, could use different layouts too.
        if (item.isHeader) {
            lp.headerDisplay = mHeaderDisplay;
            if (lp.isHeaderInline() || (mMarginsFixed && !lp.isHeaderOverlay())) {
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            } else {
                lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            }

            lp.headerEndMarginIsAuto = !mMarginsFixed;
            lp.headerStartMarginIsAuto = !mMarginsFixed;
        }
        lp.section = item.section;
        lp.sectionFirstPosition = item.sectionFirstPosition;
        itemView.setLayoutParams(lp);
    }

    @Override
    public int getItemViewType(int position) {
        return mItems.get(position).isHeader ? VIEW_TYPE_HEADER : VIEW_TYPE_CONTENT;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public void setHeaderDisplay(int headerDisplay) {
        mHeaderDisplay = headerDisplay;
        notifyHeaderChanges();
    }

    public void setMarginsFixed(boolean marginsFixed) {
        mMarginsFixed = marginsFixed;
        notifyHeaderChanges();
    }

    private void notifyHeaderChanges() {
        for (int i = 0; i < mItems.size(); i++) {
            LineItem item = mItems.get(i);
            if (item.isHeader) {
                notifyItemChanged(i);
            }
        }
    }

    private static class LineItem {

        public int section;

        public int sectionFirstPosition;

        public boolean isHeader;

        public String text;

        public LineItem(String text, boolean isHeader, int section, int sectionFirstPosition) {
            this.isHeader = isHeader;
            this.text = text;
            this.section = section;
            this.sectionFirstPosition = sectionFirstPosition;
        }
    }
}
