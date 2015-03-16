package com.tonicartos.superslim;

import android.view.View;

public interface LayoutTrimHelper extends LayoutQueryHelper {
    void init(SectionData sd);

    public abstract LayoutTrimHelper getSubsectionLayoutTrimHelper();

    void recycle();
}
