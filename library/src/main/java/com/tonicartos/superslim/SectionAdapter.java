package com.tonicartos.superslim;

import java.util.List;

public interface SectionAdapter<T extends SectionAdapter.Section> {

    List<T> getSections();

    public abstract class Section<T extends Section> {

        static final int NO_POSITION = -1;

        protected int mEnd = NO_POSITION;

        protected int mStart;

        protected List<T> mSubsections;

        protected SectionLayoutManager.SlmConfig mSlmConfig;

        public Section() {

        }

        public Section(int start) {
            this(start, NO_POSITION);
        }

        public Section(int start, int end) {
            this(start, end, null);
        }

        public Section(int start, int end, List<T> subsections) {
            this(start, end, subsections, null);
        }

        public Section(int start, int end, List<T> subsections,
                SectionLayoutManager.SlmConfig config) {
            this.mStart = start;
            this.mEnd = end;
            this.mSubsections = subsections;
            this.mSlmConfig = config;
        }

        public int getEnd() {
            return mEnd;
        }

        public Section setEnd(int end) {
            this.mEnd = end;
            return this;
        }

        public SectionLayoutManager.SlmConfig getSlmConfig() {
            return mSlmConfig;
        }

        public Section setSlmConfig(SectionLayoutManager.SlmConfig config) {
            mSlmConfig = config;
            return this;
        }

        public int getStart() {
            return mStart;
        }

        public Section setStart(int start) {
            this.mStart = start;
            return this;
        }

        public List<T> getSubsections() {
            return mSubsections;
        }

        public Section setSubsections(List<T> subsections) {
            this.mSubsections = subsections;
            return this;
        }
    }
}
