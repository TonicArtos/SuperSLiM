package com.tonicartos.superslimexample.DataDiffer;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.tonicartos.superslimexample.R;

/**
 * Simple view holder for a single text view.
 */
public class CountryViewHolder extends RecyclerView.ViewHolder {

    private TextView mTextView;

    public CountryViewHolder(View view) {
        super(view);

        mTextView = (TextView) view.findViewById(R.id.text);
    }

    public void bindItem(String text) {
        mTextView.setText(text);
    }

    @Override
    public String toString() {
        return mTextView.getText().toString();
    }
}
