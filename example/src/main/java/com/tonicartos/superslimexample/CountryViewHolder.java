package com.tonicartos.superslimexample;

import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

/**
 * Simple view holder for a single text view.
 */
class CountryViewHolder extends RecyclerView.ViewHolder {

    private TextView mTextView;

    CountryViewHolder(TextView view) {
        super(view);

        mTextView = view;
    }

    @Override
    public String toString() {
        return mTextView.getText().toString();
    }

    public void bindItem(String text) {
        mTextView.setText(text);
    }
}
