package com.darkediamonds.hyrulewatch;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.text.DateFormat;
import java.util.*;

public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(12,25,22), CARD = Color.rgb(22,43,36), GOLD = Color.rgb(215,188,121), TEXT = Color.rgb(242,242,227), MUTED = Color.rgb(161,182,170);
    private Store store;
    private LinearLayout content;
    private String filter = "all";
    private boolean refreshing;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable poll = new Runnable() {
        @Override public void run() {
            if (refreshing && !Tracker.running()) { refreshing = false; render(); }
            handler.postDelayed(this, 1200);
        }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new Store(this); store.seed(); Tracker.channel(this);
        if (state != null) filter = state.getString("filter", "all");
        if (store.prefs.getBoolean("background", false)) StockJobService.schedule(this, true);
        render();
    }
    @Override protected void onResume() { super.onResume(); refreshing = Tracker.running(); render(); handler.post(poll); }
    @Override protected void onPause() { handler.removeCallbacks(poll); super.onPause(); }
    @Override protected void onSaveInstanceState(Bundle out) { out.putString("filter", filter); super.onSaveInstanceState(out); }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private LinearLayout column() { LinearLayout view = new LinearLayout(this); view.setOrientation(LinearLayout.VERTICAL); return view; }
    private LinearLayout row() { LinearLayout view = new LinearLayout(this); view.setGravity(Gravity.CENTER_VERTICAL); return view; }
    private GradientDrawable background(int color, int radius) { GradientDrawable shape = new GradientDrawable(); shape.setColor(color); shape.setCornerRadius(dp(radius)); return shape; }
    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color); view.setPadding(0,dp(4),0,dp(4)); return view;
    }
    private void gap(LinearLayout view, int size) { View space = new View(this); view.addView(space,new LinearLayout.LayoutParams(1,dp(size))); }
    private Button button(String title, Runnable action) {
        Button view = new Button(this); view.setText(title); view.setTextSize(12); view.setAllCaps(false); view.setTextColor(TEXT); view.setMinHeight(dp(48));
        view.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(39,70,55))); view.setOnClickListener(v -> action.run()); return view;
    }
    private void weighted(LinearLayout parent, View child) { parent.addView(child, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT,1)); }
    private void render() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        content = column(); content.setPadding(dp(22),dp(20),dp(22),dp(28)); scroll.addView(content);
        scroll.setOnApplyWindowInsetsListener((v, insets) -> { v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom()); return insets; });
        setContentView(scroll);
        TextView brand = text("△   HYRULE WATCH", 15, GOLD); brand.setLetterSpacing(.18f); brand.setTypeface(null,Typeface.BOLD); content.addView(brand);
        gap(content, 18);
        TextView headline = text("Find your piece\nof the legend.", 34, TEXT); headline.setTypeface(Typeface.create("serif",Typeface.NORMAL)); content.addView(headline);
        content.addView(text("THE LEGEND OF ZELDA · 40TH ANNIVERSARY",10,GOLD));
        View art = new HardwareArt(this); art.setContentDescription("Decorative console and controller illustration"); content.addView(art,new LinearLayout.LayoutParams(-1,dp(145)));
        LinearLayout location = row(); weighted(location,text("COLORADO  /  " + store.prefs.getString("zip", "80202"),12,MUTED)); location.addView(button("Settings",this::settings)); content.addView(location);
        content.addView(text("Online stock & USD prices. For Colorado pickup, set your store on the retailer’s site.",12,MUTED));
        gap(content, 12);
        LinearLayout actions = row(); Button refresh = button(refreshing ? "Checking retailers…" : "↻  Check stock",this::refresh); refresh.setEnabled(!refreshing); weighted(actions,refresh); weighted(actions,button("+ Add listing",this::addListing)); content.addView(actions);
        long last = store.prefs.getLong("lastRefresh",0);
        content.addView(text(last == 0 ? "No checks yet · tap Check stock to begin" : "Last completed check · " + time(last),11,MUTED));
        gap(content, 14);
        HorizontalScrollView filters = new HorizontalScrollView(this); filters.setHorizontalScrollBarEnabled(false); LinearLayout chips = row();
        String[] ids = {"all","console","controller","stand"}; String[] labels = {"All hardware","Console","Controller","With stand"};
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i]; Button chip = button(labels[i],() -> { filter = id; render(); });
            if (filter.equals(id)) { chip.setTextColor(BG); chip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(GOLD)); }
            chips.addView(chip);
        }
        filters.addView(chips); content.addView(filters); gap(content, 12);
        JSONArray rows = store.listings(); int count = 0;
        for (int i = 0; i < rows.length(); i++) {
            JSONObject listing = rows.optJSONObject(i);
            if (!filter.equals("all") && !filter.equals(listing.optString("variant"))) continue;
            listingCard(listing); count++;
        }
        if (count == 0) content.addView(text("No listings here yet. Add a retailer product page to start tracking.",16,TEXT));
        gap(content, 10); content.addView(text("Keep an eye on the quest.",23,GOLD));
        content.addView(text("Checks use retailer product data when available. Unknown means unverified, and older observations may be stale. Always confirm price, seller, shipping and pickup at checkout.\n\nIndependent fan project · Not affiliated with Nintendo.",12,MUTED));
    }
    private void listingCard(JSONObject listing) {
        LinearLayout card = column(); card.setPadding(dp(16),dp(13),dp(16),dp(14)); card.setBackground(background(CARD,16));
        LinearLayout top = row(); TextView retailer = text(listing.optString("retailer").toUpperCase(Locale.US),11,GOLD); retailer.setLetterSpacing(.12f); weighted(top,retailer);
        String stock = listing.optString("stock","Unknown");
        TextView badge = text(stock,12,stock.equals("In stock") ? Color.rgb(143,226,170) : MUTED); top.addView(badge); card.addView(top);
        card.addView(text(Store.title(listing.optString("variant")),21,TEXT)); card.addView(text("Zelda 40th Anniversary Edition",12,MUTED));
        String price = listing.optString("price");
        card.addView(text(price.isEmpty() ? "Price unverified" : "$" + price + " USD",price.isEmpty() ? 15 : 26,TEXT));
        if (!listing.optString("target").isEmpty()) card.addView(text("Price alert at $" + listing.optString("target") + " or less",12,GOLD));
        long checked = listing.optLong("checked",0);
        card.addView(text(checked == 0 ? "Not checked yet" : "Observed " + time(checked),11,MUTED));
        String note = listing.optString("note"); if (!note.isEmpty()) card.addView(text(note,11,MUTED));
        if (!listing.optBoolean("watch",true)) card.addView(text("Tracking paused",12,GOLD));
        LinearLayout buttons = row(); weighted(buttons,button("Open retailer ↗",() -> open(listing.optString("url")))); weighted(buttons,button("Manage",() -> manage(listing))); card.addView(buttons);
        content.addView(card,new LinearLayout.LayoutParams(-1,-2)); gap(content,12);
    }
    private String time(long value) { return DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(value)); }
    private void open(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(url))); }
        catch (ActivityNotFoundException ex) { Toast.makeText(this,"Install a browser to open retailer pages",Toast.LENGTH_LONG).show(); }
    }
    private void refresh() {
        if (refreshing || Tracker.running()) return;
        refreshing = true; render();
        Context app = getApplicationContext();
        new Thread(() -> {
            try { Tracker.refresh(app); }
            catch (RuntimeException ex) { runOnUiThread(() -> Toast.makeText(this,"Check could not complete. Please try again.",Toast.LENGTH_LONG).show()); }
            finally { runOnUiThread(() -> { refreshing = false; if (!isFinishing() && !isDestroyed()) render(); }); }
        },"manual-stock-check").start();
    }
    private EditText input(String hint, String value, int type) {
        EditText field = new EditText(this); field.setHint(hint); field.setText(value); field.setTextColor(TEXT); field.setHintTextColor(MUTED); field.setInputType(type); field.setTextSize(15); return field;
    }
    private LinearLayout form() { LinearLayout view = column(); view.setPadding(dp(22),dp(8),dp(22),0); return view; }
    private void settings() {
        LinearLayout form = form(); form.addView(text("Colorado location",14,GOLD));
        EditText zip = input("ZIP code",store.prefs.getString("zip","80202"),InputType.TYPE_CLASS_NUMBER); form.addView(zip);
        form.addView(text("A location reminder for pickup checks. Select this ZIP on each retailer’s site; the app cannot verify store-level inventory.",12,MUTED));
        Switch background = new Switch(this); background.setText(R.string.background_checks); background.setTextColor(TEXT); background.setChecked(store.prefs.getBoolean("background",false)); form.addView(background);
        Switch alerts = new Switch(this); alerts.setText(R.string.alerts); alerts.setTextColor(TEXT); alerts.setChecked(store.prefs.getBoolean("alerts",false)); form.addView(alerts);
        form.addView(text("Android controls timing and may delay checks. Alerts require background checks or a manual refresh. No account, analytics, or server; listings stay on this device.",12,MUTED));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Watch settings").setView(form).setNegativeButton("Cancel",null).setNeutralButton("Export history",(d,w) -> export()).setPositiveButton("Save",null).create();
        dialog.setOnShowListener(d -> dialog.getButton(-1).setOnClickListener(v -> {
            String value = zip.getText().toString().trim();
            if (!value.matches("\\d{5}")) { zip.setError("Enter a five-digit ZIP code"); return; }
            try { StockJobService.schedule(this,background.isChecked()); }
            catch (IllegalStateException ex) { Toast.makeText(this,ex.getMessage(),Toast.LENGTH_LONG).show(); return; }
            store.prefs.edit().putString("zip",value).putBoolean("background",background.isChecked()).putBoolean("alerts",alerts.isChecked()).apply();
            if (alerts.isChecked() && Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},40);
            dialog.dismiss(); render();
        })); dialog.show();
    }
    private void addListing() {
        LinearLayout form = form(); Spinner variant = new Spinner(this); variant.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,Store.TITLES)); form.addView(variant);
        EditText url = input("https:// retailer product URL","",InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI); form.addView(url);
        form.addView(text("Paste a product page from Nintendo, Best Buy, GameStop, Target or Walmart. Search pages and pages without product data need a browser check.",12,MUTED));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Track another listing").setView(form).setNegativeButton("Cancel",null).setPositiveButton("Add",null).create();
        dialog.setOnShowListener(d -> dialog.getButton(-1).setOnClickListener(v -> {
            String address = url.getText().toString().trim();
            if (!Store.validUrl(address)) { url.setError("Use an HTTPS product page from a supported retailer"); return; }
            synchronized (Store.LOCK) {
                JSONArray rows = store.listings();
                if (rows.length() >= 30) { url.setError("Maximum 30 listings. Remove a listing first."); return; }
                for (int i=0;i<rows.length();i++) if (rows.optJSONObject(i).optString("url").equals(address)) { url.setError("Already tracked"); return; }
                String host = java.net.URI.create(address).getHost().replace("www.","");
                Store.add(rows,host,Store.VARIANTS[variant.getSelectedItemPosition()],address); store.save(rows);
            }
            dialog.dismiss(); render();
        })); dialog.show();
    }
    private void manage(JSONObject listing) {
        String id = listing.optString("id");
        String[] choices = {listing.optBoolean("watch",true) ? "Pause tracking" : "Resume tracking","Set target price","View recent history","Remove listing"};
        new AlertDialog.Builder(this).setTitle(listing.optString("retailer") + " · " + Store.title(listing.optString("variant"))).setItems(choices,(d,which) -> {
            if (which == 0) { store.change(id,row -> Store.put(row,"watch",!row.optBoolean("watch",true))); render(); }
            if (which == 1) priceTarget(listing);
            if (which == 2) history(listing);
            if (which == 3) new AlertDialog.Builder(this).setMessage("Remove this listing and its history?").setNegativeButton("Cancel",null).setPositiveButton("Remove",(a,b) -> {
                synchronized (Store.LOCK) { JSONArray rows = store.listings(); for (int i=rows.length()-1;i>=0;i--) if (rows.optJSONObject(i).optString("id").equals(id)) rows.remove(i); store.save(rows); } render();
            }).show();
        }).show();
    }
    private void priceTarget(JSONObject listing) {
        LinearLayout form = form(); EditText price = input("USD · blank disables target",listing.optString("target"),InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL); form.addView(price);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Price alert").setView(form).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        dialog.setOnShowListener(d -> dialog.getButton(-1).setOnClickListener(v -> {
            String value = price.getText().toString().trim();
            if (!value.isEmpty() && (!value.matches("\\d{1,4}(\\.\\d{1,2})?") || new java.math.BigDecimal(value).signum() <= 0)) { price.setError("Enter a positive USD amount below 10,000"); return; }
            store.change(listing.optString("id"),row -> Store.put(row,"target",value)); dialog.dismiss(); render();
        })); dialog.show();
    }
    private void history(JSONObject listing) {
        JSONArray history = listing.optJSONArray("history"); StringBuilder message = new StringBuilder();
        if (history != null) for (int i=history.length()-1;i>=0;i--) {
            JSONObject point = history.optJSONObject(i); String price = point.optString("price");
            message.append(time(point.optLong("time"))).append("\n").append(point.optString("stock")).append(price.isEmpty() ? "" : " · $"+price).append("\n\n");
        }
        new AlertDialog.Builder(this).setTitle("Last 40 observations").setMessage(message.length() == 0 ? "No checks yet." : message.toString()).setPositiveButton("Done",null).show();
    }
    private void export() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json").putExtra(Intent.EXTRA_TITLE,"hyrule-watch-history.json");
        startActivityForResult(intent,41);
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if (request == 41 && result == RESULT_OK && data != null && data.getData() != null) {
            try (java.io.OutputStream stream = getContentResolver().openOutputStream(data.getData())) {
                if (stream == null) throw new java.io.IOException();
                stream.write(store.listings().toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8)); Toast.makeText(this,"History exported",Toast.LENGTH_SHORT).show();
            } catch (Exception ex) { Toast.makeText(this,"Could not export history",Toast.LENGTH_LONG).show(); }
        }
    }
    private final class HardwareArt extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        HardwareArt(Context context) { super(context); }
        private void rect(Canvas canvas,float l,float t,float r,float b,int color,float radius) { paint.setColor(color); canvas.drawRoundRect(l,t,r,b,radius,radius,paint); }
        @Override protected void onDraw(Canvas original) {
            Canvas canvas = original; canvas.save(); canvas.scale(getWidth()/340f,getHeight()/145f);
            rect(canvas,20,14,263,131,Color.rgb(16,34,28),16);
            rect(canvas,44,24,242,110,Color.rgb(46,83,59),12);
            rect(canvas,66,29,220,104,Color.rgb(8,22,20),4);
            paint.setColor(GOLD); paint.setStrokeWidth(2); canvas.drawLine(135,44,95,93,paint);canvas.drawLine(135,44,175,93,paint);canvas.drawLine(95,93,175,93,paint);
            paint.setColor(Color.rgb(81,120,87));canvas.drawCircle(55,51,6,paint);canvas.drawCircle(230,80,6,paint);
            rect(canvas,179,69,313,121,Color.rgb(43,69,46),20);
            rect(canvas,181,87,210,138,Color.rgb(43,69,46),12);rect(canvas,282,87,311,138,Color.rgb(43,69,46),12);
            paint.setColor(GOLD);canvas.drawCircle(204,85,8,paint);canvas.drawCircle(268,107,7,paint);
            paint.setColor(MUTED);for(int i=0;i<4;i++)canvas.drawCircle(290+(float)Math.cos(i*Math.PI/2)*8,86+(float)Math.sin(i*Math.PI/2)*8,3,paint);
            paint.setColor(GOLD);paint.setTextSize(18);paint.setTypeface(Typeface.create("serif",Typeface.BOLD));canvas.drawText("40",231,97,paint);canvas.restore();
        }
    }
}
