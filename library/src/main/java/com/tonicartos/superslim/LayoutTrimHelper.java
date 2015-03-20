package com.tonicartos.superslim;

import android.view.View;

public interface LayoutTrimHelper extends LayoutQueryHelper {

    LayoutTrimHelper getSubsectionLayoutTrimHelper();

    int getTrimEdge();

    void init(SectionData sd, int trimEdge, int stickyEdge);

    void recycle();
}
