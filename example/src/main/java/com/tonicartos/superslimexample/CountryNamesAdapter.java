package com.tonicartos.superslimexample;

import com.tonicartos.superslim.GridSLM;
import com.tonicartos.superslim.LayoutManager;
import com.tonicartos.superslim.LinearSLM;
import com.tonicartos.superslim.SectionAdapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class CountryNamesAdapter extends RecyclerView.Adapter<CountryViewHolder>
        implements SectionAdapter<CountryNamesAdapter.Section> {

    private static final int VIEW_TYPE_HEADER = 0x01;

    private static final int VIEW_TYPE_CONTENT = 0x00;

    private static final int LINEAR = 0;

    private final Section mSectionGraph = new Section();

    private int mHeaderDisplay;

    private boolean mMarginsFixed;

    private final Context mContext;

    public CountryNamesAdapter(Context context, int headerMode) {
        mContext = context;

        final String[] countryNames = context.getResources().getStringArray(R.array.country_names);
        mHeaderDisplay = headerMode;

        //Insert headers into list of items.
        String lastHeaderText = "";
        int sectionManager = -1;
        int headerCount = 0;
        int sectionFirstPosition = 0;
        Section currentSection = new Section();
        for (int i = 0; i < countryNames.length; i++) {
            String headerText = countryNames[i].substring(0, 1);
            if (!TextUtils.equals(lastHeaderText, headerText)) {
                // Insert new header view and update section data.
                sectionManager = (sectionManager + 1) % 2;
                sectionFirstPosition = i + headerCount;
                lastHeaderText = headerText;
                headerCount += 1;
                currentSection.setEnd(sectionFirstPosition - 1);
                currentSection = new Section(sectionFirstPosition);
                currentSection.addHeader(
                        new LineItem(headerText, true, sectionManager, sectionFirstPosition));
                mSectionGraph.addSubsection(currentSection);
            }
            currentSection.addItem(
                    new LineItem(countryNames[i], false, sectionManager, sectionFirstPosition));
        }
        currentSection.setEnd(mSectionGraph.getCount());
    }

    @Override
    public List<Section> getSections() {
        return mSectionGraph.getSubsections();
    }

    public boolean isItemHeader(int position) {
        return mSectionGraph.getItem(position).isHeader;
    }

    public String itemToString(int position) {
        return mSectionGraph.getItem(position).text;
    }

    @Override
    public CountryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_HEADER) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.header_item, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.text_line_item, parent, false);
        }
        return new CountryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CountryViewHolder holder, int position) {
        final LineItem item = mSectionGraph.getItem(position);
        final View itemView = holder.itemView;

        holder.bindItem(item.text);

        final GridSLM.LayoutParams lp = new GridSLM.LayoutParams(
                itemView.getLayoutParams());
        // Overrides xml attrs, could use different layouts too.
        if (item.isHeader) {
            lp.headerDisplay = mHeaderDisplay;
            if (lp.isHeaderInline() || (mMarginsFixed && !lp.isHeaderOverlay())) {
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            } else {
                lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            }
        }

        if (position == item.sectionFirstPosition) {
            lp.setSlm(item.sectionManager == LINEAR ? LinearSLM.ID : GridSLM.ID);
            lp.marginEnd = mMarginsFixed ? mContext.getResources()
                    .getDimensionPixelSize(R.dimen.default_section_marginEnd)
                    : LayoutManager.LayoutParams.MARGIN_AUTO;
            lp.marginStart = mMarginsFixed ? mContext.getResources()
                    .getDimensionPixelSize(R.dimen.default_section_marginStart)
                    : LayoutManager.LayoutParams.MARGIN_AUTO;
            lp.setColumnWidth(mContext.getResources().getDimensionPixelSize(R.dimen.grid_column_width));
        }

        itemView.setLayoutParams(lp);
    }

    @Override
    public int getItemViewType(int position) {
        return mSectionGraph.getItem(position).isHeader ? VIEW_TYPE_HEADER : VIEW_TYPE_CONTENT;
    }

    @Override
    public int getItemCount() {
        return mSectionGraph.getCount();
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
        final int count = mSectionGraph.getCount();
        for (int i = 0; i < count; i++) {
            final LineItem item = mSectionGraph.getItem(i);
            if (item.isHeader) {
                notifyItemChanged(i);
            }
        }
    }

    private static class LineItem {

        public int sectionManager;

        public int sectionFirstPosition;

        public boolean isHeader;

        public String text;

        public LineItem(String text, boolean isHeader, int sectionManager,
                int sectionFirstPosition) {
            this.isHeader = isHeader;
            this.text = text;
            this.sectionManager = sectionManager;
            this.sectionFirstPosition = sectionFirstPosition;
        }
    }

    public static class Section extends SectionAdapter.Section<Section> {

        private ArrayList<LineItem> mItems = new ArrayList<>();

        private LineItem mHeader;

        public Section() {
        }

        public Section(int start) {
            super(start);
        }

        public Section addHeader(LineItem header) {
            mHeader = header;
            return this;
        }

        public Section addItem(LineItem item) {
            mItems.add(item);
            return this;
        }

        public Section addSubsection(Section section) {
            if (subsections == null) {
                subsections = new ArrayList<>();
            }
            subsections.add(section);
            return this;
        }

        public int getCount() {
            int sum = mHeader == null ? 0 : 1;
            if (subsections != null && subsections.size() != 0) {
                for (Section sub : subsections) {
                    sum += sub.getCount();
                }
            } else {
                sum += mItems.size();
            }

            return sum;
        }

        public LineItem getItem(int position) {
            if (mHeader != null && position == start) {
                return mHeader;
            }

            if (subsections != null) {
                for (Section sub : subsections) {
                    if (sub.contains(position)) {
                        return sub.getItem(position);
                    }
                }
            }

            return mItems.get(position - start - (mHeader != null ? 1 : 0));
        }

        private boolean contains(int position) {
            return start <= position && position <= end;
        }
    }
}
