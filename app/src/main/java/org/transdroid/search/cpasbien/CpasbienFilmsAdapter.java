package org.transdroid.search.cpasbien;

public class CpasbienFilmsAdapter extends CpasbienAdapter {
    @Override
    public String getSiteName() {
        return "Cpasbien (films)";
    }

    @Override
    protected String getQueryUrl() {
        // Add 'rip' keyword to ignore BluRay releases
        return "/recherche/films/%s-rip/page-%d%s";
    }
}
