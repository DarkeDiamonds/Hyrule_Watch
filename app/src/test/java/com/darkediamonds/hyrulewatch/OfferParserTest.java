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
        assertEquals(new OfferParser.Offer("In stock","519.99"), OfferParser.parse(page(console,available),"console"));
    }
    @Test public void avoidsWrongProductAndWrongVariant() {
        assertEquals(OfferParser.UNKNOWN, OfferParser.parse(page("Nintendo Switch 2 standard console",available),"console"));
        assertEquals(OfferParser.UNKNOWN, OfferParser.parse(page(console + " Pro Controller + display stand",available),"controller"));
        assertEquals(OfferParser.UNKNOWN, OfferParser.parse(page(console + " carrying case",available),"console"));
        assertEquals(new OfferParser.Offer("In stock","519.99"), OfferParser.parse(page(console + " Pro Controller + display stand",available),"stand"));
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
        assertEquals(new OfferParser.Offer("In stock",""),OfferParser.parse(nested,"console"));
    }
    @Test public void preorderAndOutOfStockAreDistinct() {
        assertEquals("Preorder",OfferParser.parse(page(console,available.replace("InStock","PreOrder")),"console").stock());
        assertEquals("Out of stock",OfferParser.parse(page(console,available.replace("InStock","OutOfStock")),"console").stock());
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
        assertTrue(Tracker.shouldAlert("Out of stock","99.99","In stock","99.99",""));
        assertFalse(Tracker.shouldAlert("In stock","99.99","Unknown","",""));
        assertFalse(Tracker.shouldAlert("In stock","99.99","In stock","99.99",""));
        assertTrue(Tracker.shouldAlert("","","Preorder","99.99",""));
    }
    @Test public void targetPriceAlertsAtCrossingAndOnlyWhenBuyable() {
        assertTrue(Tracker.shouldAlert("In stock","110.00","In stock","100.00","100"));
        assertFalse(Tracker.shouldAlert("In stock","100.00","In stock","99.00","100"));
        assertFalse(Tracker.shouldAlert("Out of stock","110.00","Out of stock","100.00","100"));
    }
    @Test public void invalidAndMissingPricesCannotTriggerTargetAlerts() {
        assertFalse(Tracker.shouldAlert("In stock","110.00","In stock","","100"));
        assertFalse(Tracker.shouldAlert("In stock","110.00","In stock","99.99","invalid"));
    }
}
