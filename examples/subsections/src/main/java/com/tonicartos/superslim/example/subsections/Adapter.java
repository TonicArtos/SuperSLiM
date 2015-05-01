package com.tonicartos.superslim.example.subsections;

import com.tonicartos.superslim.SectionAdapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class Adapter extends RecyclerView.Adapter<TextViewHolder>
        implements SectionAdapter<Utils.Section> {

    private final Utils.Section mData;

    public Adapter(Context context) {
        mData = Utils.createGrid(context);
    }

    @Override
    public int getItemCount() {
        return mData.getCount();
    }

    @Override
    public int getItemViewType(int position) {
        return mData.getItem(position).itemKind;
    }

    @Override
    public List<Utils.Section> getSections() {
        return mData.getSubsections();
    }

    @Override
    public void onBindViewHolder(TextViewHolder holder, int position) {
        final Utils.Item item = mData.getItem(position);
        final View itemView = holder.itemView;

        itemView.setBackgroundColor(item.background);
        holder.bindItem(String.valueOf(position));
    }

    @Override
    public TextViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view;
        switch (viewType) {
            case Utils.START_ALIGNED_HEADER:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.start_header_item, parent, false);
                break;
            case Utils.INLINE_HEADER:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.inline_header_item, parent, false);
                break;
            default:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.text_line_item, parent, false);
        }
        return new TextViewHolder(view);
    }
}
