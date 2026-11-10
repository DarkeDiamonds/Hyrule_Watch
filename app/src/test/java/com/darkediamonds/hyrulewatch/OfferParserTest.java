package com.darkediamonds.hyrulewatch;

import org.junit.Test;
import static org.junit.Assert.*;

public class OfferParserTest {
    private String page(String name, String offers) {
        return "<script type='application/ld+json'>{\"@type\":\"Product\",\"name\":\"" + name + "\",\"offers\":" + offers + "}</script>";
    }
    private final String console = "Nintendo Switch 2 The Legend of Zelda 40th Anniversary Edition";
    private final String available = "{\"@type\":\"Offer\",\"price\":\"519.99\",\"priceCurrency\":\"USD\",\"availability\":\"https://schema.org/InStock\"}";
    @Test public void readsProductStockAndUsdPrice() {
        assertEquals(new OfferParser.Offer(OfferParser.UNVERIFIED,"519.99"), OfferParser.parse(page(console,available),"console"));
    }
    @Test public void avoidsWrongProductAndWrongVariant() {
        assertEquals(OfferParser.UNKNOWN, OfferParser.parse(page("Nintendo Switch 2 standard console",available),"console"));
        assertEquals(OfferParser.UNKNOWN, OfferParser.parse(page(console + " Pro Controller + display stand",available),"controller"));
        assertEquals(OfferParser.UNKNOWN, OfferParser.parse(page(console + " carrying case",available),"console"));
        assertEquals(new OfferParser.Offer(OfferParser.UNVERIFIED,"519.99"), OfferParser.parse(page(console + " Pro Controller + display stand",available),"stand"));
    }
    @Test public void neverInfersStockFromMarketingOrBotPage() {
        assertEquals(OfferParser.UNKNOWN, OfferParser.parse("<button>Buy now</button> In stock $519.99", "console"));
        assertEquals(OfferParser.UNKNOWN, OfferParser.parse("<h1>Verify you are human</h1>", "console"));
        assertEquals(OfferParser.UNKNOWN, OfferParser.parse("<script type='application/ld+json'>{invalid</script>", "console"));
    }
    @Test public void ambiguousSellerOffersAreUnknown() {
        assertEquals(OfferParser.UNKNOWN, OfferParser.parse(page(console,"["+available+","+available.replace("InStock","OutOfStock")+"]"),"console"));
    }
    @Test public void readsGraphAndDoesNotMistakeForeignPriceForDollars() {
        String nested = "<script type=\"application/ld+json\">{\"@graph\":[{\"@type\":\"Product\",\"name\":\""+console+"\",\"offers\":"+available.replace("USD","CAD")+"}]}</script>";
        assertEquals(new OfferParser.Offer(OfferParser.UNVERIFIED,""),OfferParser.parse(nested,"console"));
    }
    @Test public void preorderAndOutOfStockAreDistinct() {
        assertEquals(OfferParser.UNVERIFIED,OfferParser.parse(page(console,available.replace("InStock","PreOrder")),"console").stock());
        assertEquals("Reported out of stock",OfferParser.parse(page(console,available.replace("InStock","OutOfStock")),"console").stock());
    }
    @Test public void relatedProductsNeverOverrideMainProduct() {
        String html = page("Wrong console",available).replace("\"offers\":", "\"isRelatedTo\":{\"@type\":\"Product\",\"name\":\""+console+"\",\"offers\":"+available+"},\"offers\":");
        assertEquals(OfferParser.UNKNOWN,OfferParser.parse(html,"console"));
    }
    @Test public void retailerUrlValidation() {
        assertTrue(Store.validUrl("https://www.bestbuy.com/product/123"));
        assertFalse(Store.validUrl("https://bestbuy.com.evil.example/product"));
        assertFalse(Store.validUrl("http://www.target.com/p/123"));
        assertFalse(Store.validUrl("https://user@www.nintendo.com/us/"));
        assertFalse(Store.validUrl("https://127.0.0.1/product"));
    }
    @Test public void restockAlertsOnlyOnConfirmedTransitions() {
        assertTrue(Tracker.shouldAlert("Out of stock","99.99",OfferParser.ONLINE,"99.99",""));
        assertFalse(Tracker.shouldAlert(OfferParser.ONLINE,"99.99","Unknown","",""));
        assertFalse(Tracker.shouldAlert(OfferParser.ONLINE,"99.99",OfferParser.ONLINE,"99.99",""));
        assertTrue(Tracker.shouldAlert("","",OfferParser.PREORDER,"99.99",""));
    }
    @Test public void targetPriceAlertsAtCrossingAndOnlyWhenBuyable() {
        assertTrue(Tracker.shouldAlert(OfferParser.ONLINE,"110.00",OfferParser.ONLINE,"100.00","100"));
        assertFalse(Tracker.shouldAlert(OfferParser.ONLINE,"100.00",OfferParser.ONLINE,"99.00","100"));
        assertFalse(Tracker.shouldAlert("Out of stock","110.00","Out of stock","100.00","100"));
    }
    @Test public void invalidAndMissingPricesCannotTriggerTargetAlerts() {
        assertFalse(Tracker.shouldAlert(OfferParser.ONLINE,"110.00",OfferParser.ONLINE,"","100"));
        assertFalse(Tracker.shouldAlert(OfferParser.ONLINE,"110.00",OfferParser.ONLINE,"99.99","invalid"));
    }

    private final String nintendoUrl = "https://www.nintendo.com/us/store/products/nintendo-switch-2-the-legend-of-zelda-40th-anniversary-edition-121642/";
    private String nintendoState(String sku, Object salable) throws Exception {
        org.json.JSONObject product = new org.json.JSONObject().put("sku",sku).put("name",console).put("isSalableQty",salable);
        org.json.JSONObject state = new org.json.JSONObject().put("Product:{\"sku\":\""+sku+"\"}",product);
        // An available recommendation must never make the main product appear available.
        state.put("Product:{\"sku\":\"other\"}",new org.json.JSONObject().put("sku","other").put("name",console).put("isSalableQty",true));
        org.json.JSONObject props = new org.json.JSONObject().put("analytics",new org.json.JSONObject().put("product",new org.json.JSONObject().put("sku",sku).put("name",console))).put("initialApolloState",state);
        return "<script id='__NEXT_DATA__' type='application/json'>" + new org.json.JSONObject().put("props",new org.json.JSONObject().put("pageProps",props)) + "</script>";
    }
    @Test public void nintendoOrderabilityOverridesStalePositiveListing() throws Exception {
        String html = page(console,available)+nintendoState("121642",false);
        assertEquals(new OfferParser.Offer("Not orderable online","519.99"),OfferParser.parse(html,"console",nintendoUrl));
    }
    @Test public void nintendoPositiveNeedsBothMatchingOrderabilityAndListing() throws Exception {
        assertEquals(OfferParser.ONLINE,OfferParser.parse(page(console,available)+nintendoState("121642",true),"console",nintendoUrl).stock());
        assertEquals("Reported out of stock",OfferParser.parse(page(console,available.replace("InStock","OutOfStock"))+nintendoState("121642",true),"console",nintendoUrl).stock());
        assertEquals(OfferParser.UNKNOWN,OfferParser.parse(nintendoState("121642",true),"console",nintendoUrl));
    }
    @Test public void wrongSkuMissingOrStringFlagCannotVerifyAvailability() throws Exception {
        assertEquals(OfferParser.UNVERIFIED,OfferParser.parse(page(console,available)+nintendoState("127074",true),"console",nintendoUrl).stock());
        assertEquals(OfferParser.UNVERIFIED,OfferParser.parse(page(console,available),"console",nintendoUrl).stock());
        assertEquals(OfferParser.UNVERIFIED,OfferParser.parse(page(console,available)+nintendoState("121642","true"),"console",nintendoUrl).stock());
    }
    @Test public void nintendoStateCannotVerifyAnotherRetailerOrHost() throws Exception {
        String html = page(console,available)+nintendoState("121642",true);
        assertEquals(OfferParser.UNVERIFIED,OfferParser.parse(html,"console","https://www.gamestop.com/product/451607.html").stock());
        assertEquals(OfferParser.UNVERIFIED,OfferParser.parse(html,"console",nintendoUrl.replace("www.nintendo.com","www.nintendo.com.evil.example")).stock());
    }
    @Test public void genericGamestopFlagIsNotZipInventoryOrAnAlert() {
        OfferParser.Offer offer = OfferParser.parse(page(console,available),"console","https://www.gamestop.com/product/451607.html");
        assertEquals(OfferParser.UNVERIFIED,offer.stock());
        assertEquals("519.99",offer.price());
        assertFalse(Tracker.shouldAlert("Out of stock","600.00",offer.stock(),offer.price(),"550"));
        // Old versions' raw stock strings must not trigger notifications either.
        assertFalse(Tracker.shouldAlert("Out of stock","600.00","In stock","519.99","550"));
    }
    @Test public void legacyStockAndHistoryAreInvalidatedWithoutLosingPrices() throws Exception {
        org.json.JSONObject row = new org.json.JSONObject().put("stock","In stock").put("price","519.99").put("confirmedStock","In stock")
                .put("history",new org.json.JSONArray().put(new org.json.JSONObject().put("stock","In stock").put("price","519.99").put("time",123)));
        Store.migrateAvailability(row);
        assertEquals("Unknown",row.getString("stock"));
        assertEquals("In stock",row.getString("legacyStock"));
        assertEquals("519.99",row.getString("price"));
        assertFalse(row.has("confirmedStock"));
        assertEquals("Legacy estimate (unverified)",row.getJSONArray("history").getJSONObject(0).getString("stock"));
        assertEquals(123,row.getJSONArray("history").getJSONObject(0).getInt("time"));
        String migrated = row.toString(); Store.migrateAvailability(row); assertEquals(migrated,row.toString());
    }
}
