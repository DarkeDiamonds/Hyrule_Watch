package com.darkediamonds.hyrulewatch;

import android.app.*;
import android.content.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

final class Tracker {
    private static final AtomicBoolean RUNNING = new AtomicBoolean();
    static boolean running() { return RUNNING.get(); }
    static void channel(Context context) {
        context.getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("stock", "Restocks and price drops", NotificationManager.IMPORTANCE_DEFAULT));
    }
    static void refresh(Context context) {
        if (!RUNNING.compareAndSet(false, true)) return;
        try {
            Store store = new Store(context);
            JSONArray snapshot;
            synchronized (Store.LOCK) { snapshot = store.listings(); }
            for (int i = 0; i < snapshot.length() && !Thread.currentThread().isInterrupted(); i++) {
                JSONObject listing = snapshot.optJSONObject(i);
                if (!listing.optBoolean("watch", true)) continue;
                OfferParser.Offer offer = OfferParser.UNKNOWN;
                String note = "";
                try {
                    offer = OfferParser.parse(fetch(listing.optString("url")), listing.optString("variant"));
                    if (offer.equals(OfferParser.UNKNOWN)) note = "Needs browser check · no unambiguous product offer";
                } catch (IOException ex) { note = "Needs browser check · " + ex.getMessage(); }
                final OfferParser.Offer result = offer;
                final String detail = note;
                store.change(listing.optString("id"), current -> {
                    long now = System.currentTimeMillis();
                    Store.put(current, "stock", result.stock());
                    Store.put(current, "price", result.price());
                    Store.put(current, "checked", now);
                    Store.put(current, "note", detail);
                    JSONArray history = current.optJSONArray("history");
                    if (history == null) history = new JSONArray();
                    JSONObject point = new JSONObject();
                    Store.put(point, "time", now); Store.put(point, "stock", result.stock()); Store.put(point, "price", result.price());
                    history.put(point);
                    while (history.length() > 40) history.remove(0);
                    Store.put(current, "history", history);
                    // Remember the last confirmed stock across transient network/markup failures.
                    String lastConfirmed = current.optString("confirmedStock");
                    boolean alert = current.optBoolean("watch", true) && shouldAlert(lastConfirmed,
                            current.optString("confirmedPrice"), result.stock(), result.price(), current.optString("target"));
                    if (!result.stock().equals("Unknown")) Store.put(current, "confirmedStock", result.stock());
                    if (!result.price().isEmpty()) Store.put(current, "confirmedPrice", result.price());
                    if (alert && store.prefs.getBoolean("alerts", false)) notify(context, current);
                });
            }
            store.prefs.edit().putLong("lastRefresh", System.currentTimeMillis()).apply();
        } finally { RUNNING.set(false); }
    }
    static boolean shouldAlert(String confirmedStock, String confirmedPrice, String stock, String price, String target) {
        boolean buyable = stock.equals("In stock") || stock.equals("Preorder");
        boolean wasBuyable = confirmedStock.equals("In stock") || confirmedStock.equals("Preorder");
        return buyable && (!wasBuyable || (below(price, target) && !below(confirmedPrice, target)));
    }
    private static boolean below(String price, String target) {
        try { return !price.isEmpty() && !target.isEmpty() && new java.math.BigDecimal(price).compareTo(new java.math.BigDecimal(target)) <= 0; }
        catch (NumberFormatException ex) { return false; }
    }
    static String fetch(String address) throws IOException {
        // Bounded HTTPS requests to supported retailers; never bypass bot challenges.
        for (int redirects = 0; redirects <= 4; redirects++) {
            if (!Store.validUrl(address)) throw new IOException("unsupported retailer URL");
            HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
            connection.setConnectTimeout(12000); connection.setReadTimeout(12000); connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent", "HyruleWatch/1.0 (personal stock tracker)");
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml");
            try {
                int code = connection.getResponseCode();
                if (code >= 300 && code < 400) {
                    String location = connection.getHeaderField("Location");
                    if (location == null) throw new IOException("redirect missing destination");
                    address = new URL(new URL(address), location).toString(); continue;
                }
                if (code != 200) throw new IOException("retailer returned HTTP " + code);
                String type = connection.getContentType();
                if (type == null || !type.toLowerCase(java.util.Locale.US).contains("text/html")) throw new IOException("retailer did not return HTML");
                try (InputStream stream = connection.getInputStream(); ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                    byte[] bytes = new byte[8192]; int count;
                    while ((count = stream.read(bytes)) != -1) {
                        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("check interrupted");
                        if (buffer.size() + count > 4 * 1024 * 1024) throw new IOException("page exceeds size limit");
                        buffer.write(bytes, 0, count);
                    }
                    return buffer.toString(StandardCharsets.UTF_8.name());
                }
            } finally { connection.disconnect(); }
        }
        throw new IOException("too many redirects");
    }
    private static void notify(Context context, JSONObject listing) {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String price = listing.optString("price");
        Notification notification = new Notification.Builder(context, "stock").setSmallIcon(com.darkediamonds.hyrulewatch.R.drawable.ic_watch)
                .setContentTitle(Store.title(listing.optString("variant")) + " · " + listing.optString("stock"))
                .setContentText(listing.optString("retailer") + (price.isEmpty() ? "" : " · $" + price) + " · Verify at checkout")
                .setContentIntent(pending).setAutoCancel(true).build();
        try { context.getSystemService(NotificationManager.class).notify(listing.optString("id").hashCode(), notification); }
        catch (SecurityException ignored) { /* User denied OS notification permission. */ }
    }
}
