/*
 *	This file is part of Transdroid Torrent Search 
 *	<http://code.google.com/p/transdroid-search/>
 *	
 *	Transdroid Torrent Search is free software: you can redistribute 
 *	it and/or modify it under the terms of the GNU Lesser General 
 *	Public License as published by the Free Software Foundation, 
 *	either version 3 of the License, or (at your option) any later 
 *	version.
 *	
 *	Transdroid Torrent Search is distributed in the hope that it will 
 *	be useful, but WITHOUT ANY WARRANTY; without even the implied 
 *	warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 *	See the GNU Lesser General Public License for more details.
 *	
 *	You should have received a copy of the GNU Lesser General Public 
 *	License along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.transdroid.search.cpasbien;

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

/**
 * An adapter that provides access to Cpasbien torrent searches by parsing
 * the raw HTML output.
 * 
 * @author Simon Brulhart
 */
public class CpasbienAdapter implements ISearchAdapter {

	private static final String DOMAIN = "http://www.cpasbien.cm";
	private static final String QUERYURL = DOMAIN + "/recherche/%s/page-%d%s";
	private static final String SORT_COMPOSITE = "";
	private static final String SORT_SEEDS = ",trie-seeds-d";
	private static final int MAX_PAGES = 3;
	private static final int CONNECTION_TIMEOUT = 20_000;

	@Override
	public List<SearchResult> search(Context context, String query, SortOrder order, int maxResults) throws Exception {
		if (query == null) {
			return null;
		}

		ArrayList<SearchResult> results = new ArrayList<>();
		for (int page=0; page<MAX_PAGES; page++) {
			// Build full URL string
			final String url = String.format(
					QUERYURL,
					URLEncoder.encode(query, "UTF-8"),
					page,
					(order == SortOrder.BySeeders ? SORT_SEEDS : SORT_COMPOSITE)
			);

			// Start synchronous search
			Document doc = Jsoup.connect(url).get();
			List<SearchResult> pageResults = scrapeResults(doc);
			if (pageResults.size() > 0) {
				results.addAll(pageResults);
			} else {
				break;
			}
		}
		return results;
	}

	@Override
	public InputStream getTorrentFile(Context context, String torrentUrl) throws Exception {
		URL url = new URL(torrentUrl);
		URLConnection urlConnection = url.openConnection();
		return new BufferedInputStream(urlConnection.getInputStream());
	}

	protected List<SearchResult> scrapeResults(Document doc) throws Exception {
		Elements divs = doc.select("#gauche div.ligne0, div.ligne1");
		List<SearchResult> results = new ArrayList<>();
		for (Element div : divs) {
			try {
				Element link = div.select("a.titre").first();
				String name = link.text();
				String detailsUrl = link.attr("href");
				String size = div.select("div.poid").first().text();
				int seeders = Integer.parseInt(div.select("div.up").first().text());
				int leechers = Integer.parseInt(div.select("div.down").first().text());

				String[] parts = detailsUrl.split("/");
				String fname = parts[parts.length - 1].replaceFirst("\\.html", ".torrent");
				String torrentLink = DOMAIN + "/telechargement/" + fname;

				Date date = null;
				results.add(new SearchResult(name, torrentLink, detailsUrl, size, date, seeders, leechers));
			} catch (NullPointerException e) {
				Log.e(CpasbienAdapter.class.getCanonicalName(),
					"Error parsing item: " + div.html());
			}
		}
		return results;
	}

	@Override
	public String buildRssFeedUrlFromSearch(String query, SortOrder order) {
		// Cpasbien doesn't support RSS feeds
		return null;
	}

	@Override
	public String getSiteName() {
		return "Cpasbien";
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
