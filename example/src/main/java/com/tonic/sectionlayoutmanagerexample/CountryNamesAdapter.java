package com.tonic.sectionlayoutmanagerexample;

import com.tonic.sectionlayoutmanager.LayoutManager;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
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

    private int mHeaderMode;

    private boolean mMarginsFixed = CountriesFragment.DEFAULT_MARGINS_FIXED;

    private boolean mHeadersSticky = CountriesFragment.DEFAULT_HEADERS_STICKY;

    public CountryNamesAdapter(Context context, int headerMode) {
        final String[] countryNames = context.getResources().getStringArray(R.array.country_names);
        mHeaderMode = headerMode;

        mItems = new ArrayList<LineItem>();

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
        for (int i = 0; i < mItems.size(); i++) {
            Log.d("Adapter Item " + mItems.get(i).section + " " + i,
                    mItems.get(i).text + "     " + (mItems.get(i).isHeader ? "Header" : ""));
        }
        mContext = context;
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
    public int getItemViewType(int position) {
        return mItems.get(position).isHeader ? VIEW_TYPE_HEADER : VIEW_TYPE_CONTENT;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
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
            lp.headerAlignment = mHeaderMode;
            if (mHeaderMode == LayoutManager.HEADER_INLINE) {
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            }

            lp.headerEndMarginIsAuto = !mMarginsFixed;
            lp.headerStartMarginIsAuto = !mMarginsFixed;
            if (mMarginsFixed) {
                lp.headerEndMargin = (int) mContext.getResources()
                        .getDimension(R.dimen.default_header_margin);
                lp.headerStartMargin = (int) mContext.getResources()
                        .getDimension(R.dimen.default_header_margin);
                if (mHeaderMode == LayoutManager.HEADER_OVERLAY_END || mHeaderMode == LayoutManager.HEADER_OVERLAY_START) {
                    lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                } else {
                    lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                }
            } else if (mHeaderMode != LayoutManager.HEADER_INLINE) {
                lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            }

            lp.isSticky = mHeadersSticky;
        }
        lp.section = item.section;
        lp.sectionFirstPosition = item.sectionFirstPosition;
        itemView.setLayoutParams(lp);
    }

    public void setHeaderMode(int mode) {
        mHeaderMode = mode;
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

    public void setMarginsFixed(boolean marginsFixed) {
        mMarginsFixed = marginsFixed;
        notifyHeaderChanges();
    }

    public void setHeadersSticky(boolean headersSticky) {
        mHeadersSticky = headersSticky;
        notifyHeaderChanges();
    }

    public String itemToString(int position) {
        return mItems.get(position).text;
    }

    public boolean isItemHeader(int position) {
        return mItems.get(position).isHeader;
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
