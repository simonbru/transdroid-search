package org.transdroid.search.cpasbien;

public class CpasbienSeriesAdapter extends CpasbienAdapter {
    @Override
    public String getSiteName() {
        return "Cpasbien (séries)";
    }

    @Override
    protected String getQueryUrl() {
        return "/recherche/series/%s/page-%d%s";
    }
}
