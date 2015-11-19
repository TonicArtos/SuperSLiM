package com.tonicartos.superslim;

import com.tonicartos.superslim.adapter.Section;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

/**
 *
 */
public class Util {

    public static FeatureMatcher<Section, Integer> hasChildCount(Matcher<Integer> matcher) {
        return new FeatureMatcher<Section, Integer>(matcher, "child count", "child count") {
            @Override
            protected Integer featureValueOf(Section actual) {
                return actual.getChildCount();
            }
        };
    }


    public static class ItemData {

        private static int sItemCount = 0;

        private int mItemNumber;

        private ItemData(int itemNumber) {
            mItemNumber = itemNumber;
        }

        public static ItemData newInstance() {
            ItemData item = new ItemData(sItemCount);
            sItemCount += 1;
            return item;
        }

        public static ItemData[] newInstances(int num) {
            ItemData[] items = new ItemData[num];
            for (int i = 0; i < num; i++) {
                items[i] = newInstance();
            }
            return items;
        }

        @Override
        public String toString() {
            return "" + mItemNumber;
        }
    }
}
