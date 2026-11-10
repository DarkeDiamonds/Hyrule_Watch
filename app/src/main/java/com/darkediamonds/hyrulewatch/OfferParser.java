package com.darkediamonds.hyrulewatch;

import org.json.*;
import java.math.BigDecimal;
import java.net.URI;
import java.util.*;
import java.util.regex.*;

/** Listing metadata never proves ZIP-specific delivery or pickup availability. */
public final class OfferParser {
    public record Offer(String stock, String price) {}
    public static final Offer UNKNOWN = new Offer("Unknown", "");
    public static final String UNVERIFIED = "Availability unverified";
    public static final String ONLINE = "Online stock reported";
    public static final String PREORDER = "Online preorder reported";
    private static final Pattern SCRIPT = Pattern.compile("<script\\b([^>]*)>(.*?)</script\\s*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern JSON_LD = Pattern.compile("\\btype\\s*=\\s*['\"]application/ld\\+json['\"]", Pattern.CASE_INSENSITIVE);
    private static final Pattern NEXT_DATA = Pattern.compile("\\bid\\s*=\\s*['\"]__NEXT_DATA__['\"]", Pattern.CASE_INSENSITIVE);

    public static Offer parse(String html, String variant) { return parse(html, variant, ""); }

    public static Offer parse(String html, String variant, String address) {
        Set<Offer> offers = new HashSet<>();
        JSONObject nextData = null;
        Matcher matcher = SCRIPT.matcher(html);
        while (matcher.find()) {
            try {
                if (JSON_LD.matcher(matcher.group(1)).find()) products(new JSONTokener(matcher.group(2).trim()).nextValue(), variant, offers);
                if (NEXT_DATA.matcher(matcher.group(1)).find()) nextData = new JSONObject(matcher.group(2));
            } catch (JSONException ignored) { /* Malformed blocks are not stock evidence. */ }
        }
        Offer raw = offers.size() == 1 ? offers.iterator().next() : UNKNOWN;
        Boolean salable = nintendoSalable(nextData, address, variant);
        if (Boolean.FALSE.equals(salable)) return new Offer("Not orderable online", raw.price());
        // A positive listing flag alone can be stale or refer to a different fulfillment method.
        String stock = switch (raw.stock()) {
            case "In stock" -> Boolean.TRUE.equals(salable) ? ONLINE : UNVERIFIED;
            case "Preorder" -> Boolean.TRUE.equals(salable) ? PREORDER : UNVERIFIED;
            case "Out of stock" -> "Reported out of stock";
            case "Backorder" -> "Backorder reported";
            default -> raw.stock();
        };
        return new Offer(stock, raw.price());
    }

    private static Boolean nintendoSalable(JSONObject data, String address, String variant) {
        if (data == null) return null;
        try {
            URI uri = URI.create(address);
            if (!"https".equals(uri.getScheme()) || !("www.nintendo.com".equals(uri.getHost()) || "nintendo.com".equals(uri.getHost()))) return null;
            Matcher skuMatch = Pattern.compile("^/us/store/products/[^/]+-(\\d+)/?$").matcher(uri.getPath());
            if (!skuMatch.matches()) return null;
            String sku = skuMatch.group(1);
            JSONObject props = data.getJSONObject("props").getJSONObject("pageProps");
            // Bind the exact URL SKU, main-page identity, and orderability record together.
            JSONObject identity = props.getJSONObject("analytics").getJSONObject("product");
            JSONObject product = props.getJSONObject("initialApolloState").getJSONObject("Product:{\"sku\":\"" + sku + "\"}");
            if (!sku.equals(identity.optString("sku")) || !sku.equals(product.optString("sku"))
                    || !matchesName(identity.optString("name"), variant) || !matchesName(product.optString("name"), variant)) return null;
            Object flag = product.opt("isSalableQty");
            return flag instanceof Boolean ? (Boolean) flag : null;
        } catch (JSONException | IllegalArgumentException ex) { return null; }
    }

    private static boolean matchesName(String value, String variant) {
        String name = value.toLowerCase(Locale.US);
        boolean controller = name.contains("controller");
        boolean stand = name.contains("stand");
        return name.contains("zelda") && name.contains("40th") && name.contains("switch") && name.contains("2")
                && (variant.equals("console") ? !controller && !name.contains("case") && !name.contains("protector")
                : (variant.equals("controller") || variant.equals("stand")) && controller && stand == variant.equals("stand"));
    }

    private static void products(Object node, String variant, Set<Offer> found) {
        if (node instanceof JSONArray array) {
            for (int i = 0; i < array.length(); i++) products(array.opt(i), variant, found);
        } else if (node instanceof JSONObject object) {
            String type = object.optString("@type");
            boolean product = type.equals("Product") || type.contains("\"Product\"");
            if (product && matchesName(object.optString("name"), variant)) collectOffers(object.opt("offers"), found);
            // Related/recommended products are deliberately excluded from a Product node.
            if (!product) {
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
