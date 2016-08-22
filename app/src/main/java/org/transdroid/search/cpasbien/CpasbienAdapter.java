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

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.transdroid.search.ISearchAdapter;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;

import java.io.InputStream;
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
	private static final String QUERYURL = DOMAIN + "/recherche/%1$s.html%2$s";
	private static final String SORT_COMPOSITE = "";
	private static final String SORT_SEEDS = ",trie-seeds-d";
	private static final int CONNECTION_TIMEOUT = 20000;

	@Override
	public List<SearchResult> search(Context context, String query, SortOrder order, int maxResults) throws Exception {
		if (query == null) {
			return null;
		}

		// Build full URL string
		final String url = String.format(
				QUERYURL,
				URLEncoder.encode(query, "UTF-8"),
				(order == SortOrder.BySeeders? SORT_SEEDS: SORT_COMPOSITE)
		);

		// Start synchronous search
		Document doc = Jsoup.connect(url).get();
		return scrapeResults(doc);
	}

	@Override
	public InputStream getTorrentFile(Context context, String url) throws Exception {
		// TODO: Rewrite this
		// Provide a simple file handle to the requested url
		HttpParams httpparams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpparams, CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(httpparams, CONNECTION_TIMEOUT);
		DefaultHttpClient httpclient = new DefaultHttpClient(httpparams);
		HttpResponse response = httpclient.execute(new HttpGet(url));
		return response.getEntity().getContent();

	}

	protected List<SearchResult> scrapeResults(Document doc) throws Exception {
		// TODO: Add pagination ?
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
		// The Pirate Bay doesn't support RSS feeds
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
