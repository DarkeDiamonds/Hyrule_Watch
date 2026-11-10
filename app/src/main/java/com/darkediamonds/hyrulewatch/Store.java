package com.darkediamonds.hyrulewatch;

import android.content.*;
import org.json.*;
import java.net.URI;
import java.util.*;

final class Store {
    static final String DEFAULT_ZIP = "80227";
    static final String[] VARIANTS = {"console", "controller", "stand"};
    static final String[] TITLES = {"Switch 2 console", "Pro Controller", "Pro Controller + stand"};
    static final Set<String> HOSTS = new HashSet<>(Arrays.asList("nintendo.com", "bestbuy.com", "gamestop.com", "target.com", "walmart.com"));
    static final Object LOCK = new Object();
    final SharedPreferences prefs;
    Store(Context context) { prefs = context.getSharedPreferences("watch", Context.MODE_PRIVATE); }
    static String title(String variant) { return TITLES[Arrays.asList(VARIANTS).indexOf(variant)]; }
    static boolean validUrl(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            return "https".equals(uri.getScheme()) && uri.getRawUserInfo() == null && (uri.getPort() == -1 || uri.getPort() == 443)
                    && host != null && HOSTS.stream().anyMatch(h -> host.equals(h) || host.equals("www." + h));
        } catch (IllegalArgumentException ex) { return false; }
    }
    JSONArray listings() {
        try {
            JSONArray rows = new JSONArray(prefs.getString("listings", "[]"));
            for (int i = 0; i < rows.length(); i++) migrateAvailability(rows.optJSONObject(i));
            return rows;
        }
        catch (JSONException ex) { throw new IllegalStateException("Saved listings could not be read", ex); }
    }
    void save(JSONArray rows) { if (!prefs.edit().putString("listings", rows.toString()).commit()) throw new IllegalStateException("Could not save listings"); }
    void seed() {
        synchronized (LOCK) {
            if (prefs.contains("listings")) { save(listings()); return; }
            JSONArray rows = new JSONArray();
            add(rows, "Nintendo", "console", "https://www.nintendo.com/us/store/products/nintendo-switch-2-the-legend-of-zelda-40th-anniversary-edition-121642/");
            add(rows, "Nintendo", "controller", "https://www.nintendo.com/us/store/products/nintendo-switch-2-pro-controller-the-legend-of-zelda-40th-anniversary-edition-127074/");
            add(rows, "Nintendo", "stand", "https://www.nintendo.com/us/store/products/nintendo-switch-2-pro-controller-display-stand-the-legend-of-zelda-40th-anniversary-edition-127076/");
            add(rows, "Best Buy", "console", "https://www.bestbuy.com/product/switch-2-the-legend-of-zelda-40th-anniversary-edition/6691841");
            add(rows, "GameStop", "console", "https://www.gamestop.com/consoles-hardware/nintendo-switch-2/products/nintendo-switch-2-the-legend-of-zelda-40th-anniversary-edition/451607.html");
            add(rows, "GameStop", "controller", "https://www.gamestop.com/gaming-accessories/controllers/nintendo-switch-2/products/nintendo-switch-2-pro-controller-the-legend-of-zelda---40th-anniversary-edition/451609.html");
            add(rows, "Target", "controller", "https://www.target.com/p/-/A-1013213521");
            save(rows);
        }
    }
    static void add(JSONArray rows, String retailer, String variant, String url) {
        try { rows.put(new JSONObject().put("id", UUID.randomUUID().toString()).put("retailer", retailer).put("variant", variant).put("url", url).put("stock", "Unknown").put("price", "").put("target", "").put("watch", true).put("history", new JSONArray()).put("stockPolicy", 2)); }
        catch (JSONException ex) { throw new IllegalStateException(ex); }
    }
    static void migrateAvailability(JSONObject row) {
        if (row.optInt("stockPolicy", 0) >= 2) return;
        put(row, "stockPolicy", 2);
        put(row, "legacyStock", row.optString("stock"));
        put(row, "stock", "Unknown");
        put(row, "note", "Previous availability estimate is unverified. Check again.");
        row.remove("confirmedStock");
        JSONArray history = row.optJSONArray("history");
        if (history != null) for (int i = 0; i < history.length(); i++) {
            JSONObject point = history.optJSONObject(i);
            put(point, "legacyStock", point.optString("stock"));
            put(point, "stock", "Legacy estimate (unverified)");
        }
    }
    void change(String id, java.util.function.Consumer<JSONObject> mutation) {
        synchronized (LOCK) {
            JSONArray rows = listings();
            for (int i = 0; i < rows.length(); i++) if (rows.optJSONObject(i).optString("id").equals(id)) mutation.accept(rows.optJSONObject(i));
            save(rows);
        }
    }
    static void put(JSONObject row, String key, Object value) {
        try { row.put(key, value); } catch (JSONException ex) { throw new IllegalStateException(ex); }
    }
}
