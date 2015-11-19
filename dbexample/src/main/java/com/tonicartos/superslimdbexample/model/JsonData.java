package com.tonicartos.superslimdbexample.model;

import java.util.List;

/**
 * Quick model for json data to initialise the database with.
 */
public class JsonData {

    public List<Region> regions;

    public static class Region {

        public String name;

        public List<SubRegion> sub_regions;
    }

    public static class SubRegion {

        public String name;

        public List<Country> countries;
    }

    public static class Country {

        public String name;
    }
}
