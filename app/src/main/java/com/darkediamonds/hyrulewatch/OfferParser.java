package com.darkediamonds.hyrulewatch;

import org.json.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.*;

/** Reads product-scoped schema.org offers only. Never guesses stock from page text. */
public final class OfferParser {
    public record Offer(String stock, String price) {}
    public static final Offer UNKNOWN = new Offer("Unknown", "");
    private static final Pattern SCRIPT = Pattern.compile("<script\\b[^>]*type\\s*=\\s*['\"]application/ld\\+json['\"][^>]*>(.*?)</script\\s*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public static Offer parse(String html, String variant) {
        Set<Offer> offers = new HashSet<>();
        Matcher matcher = SCRIPT.matcher(html);
        while (matcher.find()) {
            try { products(new JSONTokener(matcher.group(1).trim()).nextValue(), variant, offers); }
            catch (JSONException ignored) { /* A malformed block is not stock evidence. */ }
        }
        return offers.size() == 1 ? offers.iterator().next() : UNKNOWN;
    }

    private static void products(Object node, String variant, Set<Offer> found) {
        if (node instanceof JSONArray array) {
            for (int i = 0; i < array.length(); i++) products(array.opt(i), variant, found);
        } else if (node instanceof JSONObject object) {
            String type = object.optString("@type");
            String name = object.optString("name").toLowerCase(Locale.US);
            boolean controller = name.contains("controller");
            boolean stand = name.contains("stand");
            boolean matches = name.contains("zelda") && name.contains("40th") && name.contains("switch") && name.contains("2")
                    && (variant.equals("console") ? !controller && !name.contains("case") && !name.contains("protector")
                    : controller && stand == variant.equals("stand"));
            if ((type.equals("Product") || type.contains("\"Product\"")) && matches) {
                collectOffers(object.opt("offers"), found);
            }
            // Related/recommended products are deliberately excluded from a Product node.
            if (!type.equals("Product") && !type.contains("\"Product\"")) {
                products(object.opt("@graph"), variant, found);
                products(object.opt("mainEntity"), variant, found);
            }
        }
    }

    private static void collectOffers(Object node, Set<Offer> found) {
        if (node instanceof JSONArray array) {
            for (int i = 0; i < array.length(); i++) collectOffers(array.opt(i), found);
        } else if (node instanceof JSONObject offer) {
            if (offer.has("offers")) { collectOffers(offer.opt("offers"), found); return; }
            String raw = offer.optString("availability").replace("https://schema.org/", "").replace("http://schema.org/", "");
            String stock = switch (raw) {
                case "InStock", "LimitedAvailability" -> "In stock";
                case "OutOfStock", "SoldOut", "Discontinued" -> "Out of stock";
                case "PreOrder", "PreSale" -> "Preorder";
                case "BackOrder" -> "Backorder";
                default -> "Unknown";
            };
            String price = "";
            if (offer.optString("priceCurrency").equals("USD")) {
                try {
                    BigDecimal value = new BigDecimal(offer.optString("price"));
                    if (value.signum() > 0 && value.compareTo(new BigDecimal("10000")) <= 0) price = value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
                } catch (NumberFormatException ignored) { }
            }
            found.add(new Offer(stock, price));
        }
    }
}
