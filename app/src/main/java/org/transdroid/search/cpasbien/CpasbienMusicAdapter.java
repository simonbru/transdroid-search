package org.transdroid.search.cpasbien;

public class CpasbienMusicAdapter extends CpasbienAdapter {
    @Override
    public String getSiteName() {
        return "Cpasbien (musique)";
    }

    @Override
    protected String getQueryUrl() {
        return "/recherche/musique/%s/page-%d%s";
    }
}
