package com.tonicartos.superslim;

import java.util.ArrayList;
import java.util.List;

public interface SectionAdapter<T extends SectionAdapter.Section> {

    List<T> getSections();

    public abstract class Section<T extends Section> {

        static final int NO_POSITION = -1;

        public int end = NO_POSITION;

        public int start;

        public List<T> subsections;

        public SectionLayoutManager.SlmConfig slmConfig;

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
            this.start = start;
            this.end = end;
            this.subsections = subsections;
            this.slmConfig = config;
        }

        public int getEnd() {
            return end;
        }

        public Section setEnd(int end) {
            this.end = end;
            return this;
        }

        public SectionLayoutManager.SlmConfig getSlmConfig() {
            return null;
        }

        public Section setSlmConfig(SectionLayoutManager.SlmConfig config) {
            slmConfig = config;
            return this;
        }

        public int getStart() {
            return start;
        }

        public Section setStart(int start) {
            this.start = start;
            return this;
        }

        public List<T> getSubsections() {
            return subsections;
        }

        public Section setSubsections(List<T> subsections) {
            this.subsections = subsections;
            return this;
        }
    }
}
