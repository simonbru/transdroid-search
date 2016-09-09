package org.transdroid.search.Zooqle;

import android.content.Context;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.transdroid.search.ISearchAdapter;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ZooqleAdapter implements ISearchAdapter {
    private static final String DOMAIN = "https://zooqle.com";
    private static final String QUERY_URL = DOMAIN + "/search?pg=%d&q=%s&v=t%s";
    private static final String SORT_COMPOSITE = "";
    private static final String SORT_SEEDS = "v=t&s=ns&sd=d";
    private static final int MAX_PAGES = 10;
    private static final int CONNECTION_TIMEOUT = 20_000;

    @Override
    public List<SearchResult> search(Context context, String query, SortOrder order, int maxResults) throws Exception {
        if (query == null) {
            return null;
        }

        ArrayList<SearchResult> results = new ArrayList<>();
        Log.i(ZooqleAdapter.class.getCanonicalName(), Integer.toString(maxResults));
        for (int page=1; page<MAX_PAGES; page++) {
            // Build full URL string
            final String url = String.format(
                    QUERY_URL,
                    page,
                    URLEncoder.encode(query, "UTF-8"),
                    (order == SortOrder.BySeeders ? SORT_SEEDS : SORT_COMPOSITE)
            );

            // Start synchronous search
            Log.i(ZooqleAdapter.class.getCanonicalName(), url);
            Document doc = Jsoup.connect(url).get();
            List<SearchResult> pageResults = scrapeResults(doc);
            if (pageResults.size() == 0) {
                break;
            }
            results.addAll(pageResults);
            if (results.size() >= maxResults) {
                break;
            }
        }
        int finalSize = Math.min(maxResults, results.size());
        return results.subList(0, finalSize);
    }

    @Override
    public InputStream getTorrentFile(Context context, String torrentUrl) throws Exception {
        URL url = new URL(torrentUrl);
        URLConnection urlConnection = url.openConnection();
        return new BufferedInputStream(urlConnection.getInputStream());
    }

    protected List<SearchResult> scrapeResults(Document doc) throws Exception {
        Elements trs = doc.select("table.table-torrents tbody tr");
        List<SearchResult> results = new ArrayList<>();
        for (Element tr : trs) {
            try {
                Element link = tr.select("a").first();
                String name = link.text();
                String detailsUrl = DOMAIN + link.attr("href");
                String size = tr.select("td:nth-child(4)").first().text();

                int leechers = -1;
                int seeders = -1;
                Elements peers = tr.select("td:nth-child(6) > div[title]");
                if (!peers.isEmpty()) {
                    String title = peers.attr("title");
                    Matcher m = Pattern
                            .compile("Seeders: ([0-9,]+) \\| Leechers: ([0-9,]+)")
                            .matcher(title);
                    if (m.matches()) {
                        seeders = Integer.parseInt(m.group(1).replace(",", ""));
                        leechers = Integer.parseInt(m.group(2).replace(",", ""));
                    }
                }

                String torrentLink = null;
                Date date = null;
                results.add(new SearchResult(name, torrentLink, detailsUrl, size, date, seeders, leechers));
            } catch (NullPointerException e) {
                Log.e(ZooqleAdapter.class.getCanonicalName(),
                        "Error parsing item: " + tr.html());
            }
        }
        return results;
    }

    @Override
    public String buildRssFeedUrlFromSearch(String query, SortOrder order) {
        return null;
    }

    @Override
    public String getSiteName() {
        return "Zooqle";
    }

    @Override
    public boolean isPrivateSite() {
        return false;
    }

    @Override
    public boolean usesToken() {
        return false;
    }
}
