package com.tonicartos.superslimexample;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

/**
 * Simple view holder for a single text view.
 */
class CountryViewHolder extends RecyclerView.ViewHolder {

    private TextView mTextView;

    CountryViewHolder(View view) {
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
