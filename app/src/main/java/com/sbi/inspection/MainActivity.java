package com.sbi.inspection;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.location.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.*;
import android.widget.*;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.zip.*;

import org.xmlpull.v1.XmlPullParser;
import android.util.Xml;

public class MainActivity extends Activity {
    private static final int BLUE = Color.rgb(0,58,112);
    private static final int DARK = Color.rgb(0,40,85);
    private static final int LIGHT = Color.rgb(234,243,250);
    private static final int BG = Color.rgb(244,247,250);
    private static final int TEXT = Color.rgb(23,32,42);
    private static final int MUTED = Color.rgb(101,116,130);
    private static final int GREEN = Color.rgb(19,138,82);
    private static final int RED = Color.rgb(179,38,30);
    private static final int BORDER = Color.rgb(216,224,231);
    private static final Typeface REPORT_FONT = Typeface.create("Arial", Typeface.NORMAL);
    private static final Typeface REPORT_FONT_BOLD = Typeface.create("Arial", Typeface.BOLD);

    private static final int REQ_CAMERA = 100;
    private static final int REQ_LOCATION = 101;
    private static final int REQ_NOTIFICATION = 102;
    private static final int REQ_GALLERY = 103;
    private static final int REQ_MASTER = 104;
    private static final int REQ_EXCEL = 105;

    private LinearLayout body;
    private DB database;
    private String photoPath = "";
    private String currentGps = "";
    private TextView photoStatus;
    private TextView gpsStatus;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private int photoTargetRequest = REQ_GALLERY;
    private int editingId = 0;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(DARK);
        getWindow().setNavigationBarColor(Color.WHITE);
        showHome();
    }

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable rounded(int color, int radius) { GradientDrawable g = new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radius)); return g; }
    private GradientDrawable outlined(int fill, int stroke, int radius) { GradientDrawable g = rounded(fill,radius); g.setStroke(dp(1),stroke); return g; }

    private TextView text(String s, float size, int color) {
        TextView v = new TextView(this); v.setText(s); v.setTextSize(size); v.setTextColor(color); v.setFontFeatureSettings("kern"); return v;
    }
    private Button button(String label, boolean primary) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(dp(48));
        b.setMinimumHeight(dp(48));
        b.setPadding(dp(16), dp(4), dp(16), dp(4));
        b.setTypeface(Typeface.create("Arial", Typeface.BOLD));
        b.setLetterSpacing(0.01f);
        b.setTextColor(primary ? Color.WHITE : BLUE);
        GradientDrawable bg = primary
                ? rounded(BLUE, 16)
                : outlined(Color.WHITE, Color.rgb(190, 204, 216), 16);
        b.setBackground(bg);
        b.setStateListAnimator(null);
        b.setElevation(dp(2));
        b.setIncludeFontPadding(true);
        return b;
    }
    private Space gap(int h) { Space s = new Space(this); s.setLayoutParams(new LinearLayout.LayoutParams(1,dp(h))); return s; }

    private void showHome() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        // Modern banking-style top app bar
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(20), dp(18), dp(20), dp(18));
        header.setBackground(rounded(DARK, 0));

        LinearLayout brandRow = new LinearLayout(this);
        brandRow.setGravity(Gravity.CENTER_VERTICAL);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.sbi_original);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(46), dp(46));
        brandRow.addView(logo, logoLp);

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setPadding(dp(12), 0, 0, 0);
        TextView bank = text("STATE BANK OF INDIA", 18, Color.WHITE);
        bank.setTypeface(Typeface.create("Arial", Typeface.BOLD));
        brand.addView(bank);
        TextView app = text("FIELD INSPECTION • WORKFLOW", 11, Color.rgb(195,215,236));
        app.setTypeface(Typeface.create("Arial", Typeface.BOLD));
        brand.addView(app);
        brandRow.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));

        TextView status = text("●  ACTIVE", 11, Color.rgb(160,225,190));
        status.setTypeface(Typeface.create("Arial", Typeface.BOLD));
        status.setGravity(Gravity.CENTER);
        brandRow.addView(status, new LinearLayout.LayoutParams(dp(78), dp(40)));
        header.addView(brandRow);
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(18), dp(16), dp(90));

        TextView title = text("Inspection Dashboard", 24, TEXT);
        title.setTypeface(Typeface.create("Arial", Typeface.BOLD));
        content.addView(title);
        TextView subtitle = text("Plan visits, capture customer interactions and generate reports.", 13, MUTED);
        content.addView(subtitle);
        content.addView(gap(14));

        addDashboardStats(content);
        content.addView(gap(16));

        // Quick actions as compact modern controls
        LinearLayout quickCard = new LinearLayout(this);
        quickCard.setOrientation(LinearLayout.VERTICAL);
        quickCard.setPadding(dp(14), dp(14), dp(14), dp(14));
        quickCard.setBackground(outlined(Color.WHITE, BORDER, 18));
        TextView qh = text("QUICK ACTIONS", 11, BLUE);
        qh.setTypeface(Typeface.create("Arial", Typeface.BOLD));
        quickCard.addView(qh);
        LinearLayout qrow = new LinearLayout(this);
        qrow.setPadding(0, dp(10), 0, 0);
        addQuick(qrow, "＋  NEW VISIT", () -> newVisitMenu());
        addQuick(qrow, "▣  REPORTS", () -> reports());
        quickCard.addView(qrow, new LinearLayout.LayoutParams(-1, dp(58)));
        content.addView(quickCard);
        content.addView(gap(18));

        TextView mh = text("WORKSPACE", 11, BLUE);
        mh.setTypeface(Typeface.create("Arial", Typeface.BOLD));
        content.addView(mh);
        content.addView(gap(8));

        // Two-column modern module grid
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        addModulePair(grid,
                "GOVERNMENT\nOFFICE VISITS", "Visits • Leads • PDF", "01", () -> govHub(),
                "CROSS\nSELLING", "Life • General • MF", "02", () -> crossHub());
        addModulePair(grid,
                "LOANS &\nADVANCES", "Loans • SMA • NPA", "03", () -> loanHub(),
                "CUSTOMER\n360", "Search • History • Contact", "04", () -> customer360());
        addModulePair(grid,
                "PENDING\nWORK", "Tasks • Due • Complete", "05", () -> pendingWork(),
                "MASTER\nCUSTOMER FILE", "Excel • Customer master", "06", () -> masterFile());
        addModulePair(grid,
                "REPORT\nCENTRE", "Generate • Open • Share", "07", () -> reports(),
                "SETTINGS", "Branch • Official details", "08", () -> settingsPage());
        content.addView(grid);

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        // Fixed bottom navigation style action bar, kept above Android navigation area.
        LinearLayout bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setPadding(dp(12), dp(8), dp(12), dp(10));
        bottom.setBackground(outlined(Color.WHITE, BORDER, 0));
        Button home = button("⌂  HOME", true);
        home.setEnabled(false);
        Button reports = button("▣  REPORTS", false);
        Button pending = button("✓  TASKS", false);
        bottom.addView(home, new LinearLayout.LayoutParams(0, dp(48), 1));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, dp(48), 1);
        rp.setMargins(dp(6), 0, 0, 0);
        bottom.addView(reports, rp);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(0, dp(48), 1);
        pp.setMargins(dp(6), 0, 0, 0);
        bottom.addView(pending, pp);
        reports.setOnClickListener(v -> reports());
        pending.setOnClickListener(v -> pendingWork());
        root.addView(bottom);

        setContentView(root);
    }

    private void addModulePair(LinearLayout parent,
                               String t1, String s1, String n1, Runnable r1,
                               String t2, String s2, String n2, Runnable r2) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        addModernModule(row, t1, s1, n1, r1);
        addModernModule(row, t2, s2, n2, r2);
        parent.addView(row, new LinearLayout.LayoutParams(-1, dp(142)));
    }

    private void addModernModule(LinearLayout row, String title, String subtitle, String number, Runnable action) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(13), dp(12), dp(12));
        card.setBackground(outlined(Color.WHITE, BORDER, 18));
        card.setElevation(dp(2));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView badge = text(number, 10, BLUE);
        badge.setTypeface(Typeface.create("Arial", Typeface.BOLD));
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(rounded(LIGHT, 10));
        top.addView(badge, new LinearLayout.LayoutParams(dp(30), dp(30)));
        TextView arrow = text("›", 25, BLUE);
        arrow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(dp(28), dp(30));
        alp.gravity = Gravity.RIGHT;
        top.addView(arrow, alp);
        card.addView(top);

        TextView t = text(title, 14, TEXT);
        t.setTypeface(Typeface.create("Arial", Typeface.BOLD));
        t.setMaxLines(2);
        t.setGravity(Gravity.LEFT);
        card.addView(t);

        TextView sub = text(subtitle, 11, MUTED);
        sub.setMaxLines(2);
        card.addView(sub);

        card.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1);
        lp.setMargins(dp(3), dp(3), dp(3), dp(7));
        row.addView(card, lp);
    }

    private void addDashboardStats(LinearLayout p) {
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        // Do not initialize database during application startup; open it only after Home is rendered.
        addStat(row,"VISITS","—"); addStat(row,"CUSTOMERS","—"); addStat(row,"PENDING","—"); p.addView(row,new LinearLayout.LayoutParams(-1,dp(78)));
        row.postDelayed(() -> { if (row.getParent()!=null) { row.removeAllViews(); addStat(row,"VISITS",String.valueOf(db().count("visits"))); addStat(row,"CUSTOMERS",String.valueOf(db().count("customers"))); addStat(row,"PENDING",String.valueOf(db().pendingCount())); } },250);
    }
    private void addStat(LinearLayout p,String label,String value) { LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setGravity(Gravity.CENTER); c.setBackground(rounded(Color.WHITE,14)); TextView n=text(value,21,DARK); n.setGravity(Gravity.CENTER); n.setTypeface(null,Typeface.BOLD); c.addView(n); TextView l=text(label,10,MUTED); l.setGravity(Gravity.CENTER); l.setTypeface(null,Typeface.BOLD); c.addView(l); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1); lp.setMargins(dp(3),0,dp(3),0); p.addView(c,lp); }
    private void addQuick(LinearLayout p,String label,Runnable r){Button b=button(label,true); b.setTextSize(13); b.setOnClickListener(v->r.run()); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(52),1); lp.setMargins(dp(4),0,dp(4),0); p.addView(b,lp);}
    private void homeCard(LinearLayout p,String title,String sub,String number,Runnable r){
        LinearLayout card=new LinearLayout(this); card.setGravity(Gravity.CENTER_VERTICAL); card.setPadding(dp(14),dp(12),dp(10),dp(12)); card.setBackground(outlined(Color.WHITE,BORDER,16));
        TextView num=text(number,13,BLUE); num.setGravity(Gravity.CENTER); num.setTypeface(null,Typeface.BOLD); num.setBackground(rounded(LIGHT,13)); card.addView(num,new LinearLayout.LayoutParams(dp(44),dp(56)));
        LinearLayout info=new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL); info.setPadding(dp(12),0,dp(8),0); TextView t=text(title,15,TEXT); t.setTypeface(null,Typeface.BOLD); t.setMaxLines(2); info.addView(t); TextView s=text(sub,12,MUTED); s.setMaxLines(2); info.addView(s); card.addView(info,new LinearLayout.LayoutParams(0,dp(56),1)); TextView ar=text("›",30,BLUE); ar.setGravity(Gravity.CENTER); card.addView(ar,new LinearLayout.LayoutParams(dp(32),dp(56))); card.setOnClickListener(v->r.run()); LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(82)); cp.setMargins(0,0,0,dp(10)); p.addView(card,cp);
    }

    private void base(String title) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(14), dp(10), dp(14), dp(10));
        bar.setBackgroundColor(DARK);

        Button back = button("‹  BACK", false);
        back.setTextColor(Color.WHITE);
        back.setBackground(rounded(Color.rgb(24, 65, 105), 14));
        back.setTextSize(14);
        back.setOnClickListener(v -> showHome());
        bar.addView(back, new LinearLayout.LayoutParams(dp(104), dp(48)));

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setPadding(dp(12), 0, 0, 0);
        TextView t = text(title, 18, Color.WHITE);
        t.setTypeface(Typeface.create("Arial", Typeface.BOLD));
        titleBox.addView(t);
        TextView small = text("SBI INSPECTION", 10, Color.rgb(185, 208, 231));
        small.setTypeface(Typeface.create("Arial", Typeface.BOLD));
        titleBox.addView(small);
        bar.addView(titleBox, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(bar);

        ScrollView scroll = new ScrollView(this);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18), dp(16), dp(18), dp(50));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }
    private TextView section(String s){TextView v=text(s,13,BLUE);v.setTypeface(null,Typeface.BOLD);v.setPadding(0,dp(10),0,dp(8));body.addView(v);return v;}
    private Button action(String s,boolean primary,Runnable r){Button b=button(s,primary);b.setOnClickListener(v->r.run());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(54));lp.setMargins(0,0,0,dp(10));body.addView(b,lp);return b;}
    private EditText field(String hint){
        EditText e=new EditText(this);
        e.setHint(hint);
        e.setTextSize(15);
        e.setTextColor(TEXT);
        e.setHintTextColor(MUTED);
        e.setPadding(dp(14),dp(7),dp(14),dp(7));
        e.setBackground(outlined(Color.WHITE,BORDER,12));
        e.setSingleLine(false);
        e.setMinLines(1);
        e.setGravity(Gravity.TOP|Gravity.START);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(56));
        lp.setMargins(0,0,0,dp(10));
        body.addView(e,lp);
        return e;
    }

    private EditText remarksField(String hint){
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12),dp(10),dp(12),dp(10));
        box.setBackground(outlined(Color.WHITE,BORDER,16));
        TextView label=text(hint.toUpperCase(Locale.US),12,BLUE);
        label.setTypeface(null,Typeface.BOLD);
        box.addView(label);
        EditText e=new EditText(this);
        e.setHint("Enter detailed remarks — maximum 3000 words");
        e.setTextSize(15);
        e.setTextColor(TEXT);
        e.setHintTextColor(MUTED);
        e.setGravity(Gravity.TOP|Gravity.START);
        e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        e.setSingleLine(false);
        e.setMinLines(8);
        e.setMaxLines(16);
        e.setVerticalScrollBarEnabled(true);
        e.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        e.setPadding(dp(12),dp(12),dp(12),dp(12));
        e.setBackground(outlined(Color.rgb(248,250,252),BORDER,12));
        e.setFilters(new InputFilter[]{new WordLimitFilter(3000)});
        box.addView(e,new LinearLayout.LayoutParams(-1,dp(190)));
        TextView counter=text("0 / 3000 words",11,MUTED);
        counter.setGravity(Gravity.END);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(28));
        cp.topMargin=dp(4);
        box.addView(counter,cp);
        e.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            @Override public void onTextChanged(CharSequence s,int st,int before,int count){counter.setText(wordCount(s.toString())+" / 3000 words");}
            @Override public void afterTextChanged(Editable e){}
        });
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,0,0,dp(12));
        body.addView(box,lp);
        return e;
    }

    private int wordCount(String s){
        s=nvl(s).trim();
        if(s.isEmpty()) return 0;
        return s.split("\\s+").length;
    }

    private static class WordLimitFilter implements InputFilter {
        private final int maxWords;
        WordLimitFilter(int maxWords){this.maxWords=maxWords;}
        @Override public CharSequence filter(CharSequence source,int start,int end,Spanned dest,int dstart,int dend){
            String proposed=dest.subSequence(0,dstart).toString()+source.subSequence(start,end)+dest.subSequence(dend,dest.length());
            String trimmed=proposed.trim();
            if(trimmed.isEmpty()) return null;
            int words=trimmed.split("\\s+").length;
            return words<=maxWords?null:"";
        }
    }
    private Spinner spinner(String[] values){Spinner s=new Spinner(this);ArrayAdapter<String>a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,values);s.setAdapter(a);s.setBackground(outlined(Color.WHITE,BORDER,12));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(56));lp.setMargins(0,0,0,dp(10));body.addView(s,lp);return s;}
    private TextView infoBox(String s){TextView v=text(s,13,MUTED);v.setPadding(dp(14),dp(12),dp(14),dp(12));v.setBackground(outlined(LIGHT,BORDER,12));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(12));body.addView(v,lp);return v;}

    private void hub(String title,String subtitle){base(title);infoBox(subtitle);}
    private void govHub(){hub("Government Office Visits","Record department visits with GPS, date/time, photograph, official remarks and professional PDF reporting.");section("QUICK ACTIONS");action("＋  CREATE NEW GOVERNMENT OFFICE LEAD",true,()->govForm(0));action("VIEW & MANAGE LEADS",false,()->govList());action("GENERATE GOVERNMENT OFFICE PDF",false,()->generateHeadAndShow("Government Office Visits"));}
    private void crossHub(){hub("Cross Selling","Choose one SBI product, capture customer interest and preserve the visit history.");section("SELECT PRODUCT");action("SBI LIFE",true,()->crossForm(0,"SBI LIFE"));action("SBI GENERAL",false,()->crossForm(0,"SBI GENERAL"));action("SBI MUTUAL FUND",false,()->crossForm(0,"SBI MUTUAL FUND"));section("VISITS & REPORTS");action("VIEW & MANAGE CROSS SELLING VISITS",false,()->crossList());action("GENERATE CROSS SELLING PDF",false,()->generateHeadAndShow("Cross Selling"));}
    private void loanHub(){hub("Loans & Advances","Select LOANS, SMA or NPA. IRAC status values are displayed exactly as required.");section("INSPECTION TYPE");action("LOANS",true,()->loanForm(0,"LOANS"));action("SMA",false,()->loanForm(0,"SMA"));action("NPA",false,()->loanForm(0,"NPA"));section("VISITS & REPORTS");action("VIEW & MANAGE LOAN VISITS",false,()->loanList());action("GENERATE LOANS & ADVANCES PDF",false,()->generateHeadAndShow("Loans & Advances"));}
    private void newVisitMenu(){hub("New Visit","Choose the inspection category to begin a field visit.");section("SELECT MODULE");action("GOVERNMENT OFFICE VISIT",true,()->govForm(0));action("CROSS SELLING",false,()->crossForm(0,"SBI LIFE"));action("LOANS & ADVANCES",false,()->loanForm(0,"LOANS"));}

    private void govForm(int id){
        editingId=id; photoPath=""; currentGps=""; base(id==0?"Create Government Office Lead":"Edit Government Office Lead");
        section("VISIT DETAILS");
        EditText dept=field("Department Name"); EditText account=field("Account number"); EditText controlling=field("Controlling person Name"); EditText phone=field("contact number"); EditText purpose=field("Visit Purpose"); EditText remarks=remarksField("Visiting Officials remarks");
        TextView date=infoBox("Date & Time: "+now());
        LinearLayout photo=photoControls(); TextView gps=gpsControls();
        if(id>0)loadVisitFields(id,dept,account,controlling,phone,null,purpose,remarks,null,date,null);
        action("SAVE GOVERNMENT OFFICE LEAD",true,()->{if(req(dept,"Department Name")&&req(account,"Account number")){db().saveVisit(id,"Government Office Visits","Government Office",dept.getText().toString(),account.getText().toString(),phone.getText().toString(),"",purpose.getText().toString(),"",remarks.getText().toString(),currentGps,photoPath,controlling.getText().toString(),System.currentTimeMillis());toast("Lead saved successfully");govList();}});
    }

    private void crossForm(int id,String product){
        editingId=id; photoPath=""; currentGps=""; base(id==0?"Cross Selling — "+product:"Edit Cross Selling Visit");
        section("CUSTOMER DETAILS"); EditText name=field("Name of customer"); EditText account=field("Account Number"); EditText phone=field("contact number"); EditText address=field("address"); EditText purpose=field("Purpose of visit"); Spinner interest=spinner(new String[]{"Interested","Not interested"}); EditText remarks=remarksField("Visiting officials remarks"); TextView dt=infoBox("Date & Time: "+now()); photoControls(); gpsControls();
        if(id>0)loadVisitFields(id,name,account,null,phone,address,purpose,remarks,interest,dt,null);
        action("SAVE CROSS SELLING VISIT",true,()->{if(req(name,"Name of customer")&&req(account,"Account Number")){db().saveVisit(id,"Cross Selling",product,name.getText().toString(),account.getText().toString(),phone.getText().toString(),address.getText().toString(),purpose.getText().toString(),interest.getSelectedItem().toString(),remarks.getText().toString(),currentGps,photoPath,"",System.currentTimeMillis());toast("Cross selling visit saved");crossList();}});
    }

    private void loanForm(int id,String subhead){
        editingId=id; photoPath=""; currentGps=""; base(id==0?"Loans & Advances — "+subhead:"Edit Loans & Advances Visit");
        section("CUSTOMER DETAILS"); EditText name=field("Name of customer"); EditText account=field("Account Number"); EditText phone=field("contact number"); EditText address=field("address"); EditText purpose=field("Purpose of visit");
        section("IRAC STATUS"); Spinner irac=spinner(new String[]{"1.8","1.7","1.6","1.5","1.4","1.3","1.2","0.1"}); EditText remarks=remarksField("Visiting officials remarks"); infoBox("Date & Time: "+now()); photoControls(); gpsControls();
        if(id>0)loadVisitFields(id,name,account,null,phone,address,purpose,remarks,null,null,irac);
        action("SAVE LOANS & ADVANCES VISIT",true,()->{if(req(name,"Name of customer")&&req(account,"Account Number")){db().saveVisit(id,"Loans & Advances",subhead,name.getText().toString(),account.getText().toString(),phone.getText().toString(),address.getText().toString(),purpose.getText().toString(),irac.getSelectedItem().toString(),remarks.getText().toString(),currentGps,photoPath,"",System.currentTimeMillis());toast("Visit saved successfully");loanList();}});
    }

    private boolean req(EditText e,String label){if(e.getText().toString().trim().isEmpty()){e.setError(label+" is required");e.requestFocus();return false;}return true;}
    private String now(){return new SimpleDateFormat("dd-MM-yyyy HH:mm",Locale.US).format(new Date());}
    private void loadVisitFields(int id,EditText name,EditText account,EditText controlling,EditText phone,EditText address,EditText purpose,EditText remarks,Spinner interest,TextView date,Spinner irac){Cursor c=db().one(id);if(c.moveToFirst()){if(name!=null)name.setText(c.getString(c.getColumnIndexOrThrow("name")));if(account!=null)account.setText(c.getString(c.getColumnIndexOrThrow("account")));if(controlling!=null)controlling.setText(c.getString(c.getColumnIndexOrThrow("controlling")));if(phone!=null)phone.setText(c.getString(c.getColumnIndexOrThrow("phone")));if(address!=null)address.setText(c.getString(c.getColumnIndexOrThrow("address")));if(purpose!=null)purpose.setText(c.getString(c.getColumnIndexOrThrow("purpose")));if(remarks!=null)remarks.setText(c.getString(c.getColumnIndexOrThrow("remarks")));if(interest!=null)interest.setSelection(Math.max(0,interest.getSelectedItemPosition()));if(irac!=null){String x=c.getString(c.getColumnIndexOrThrow("interest"));for(int i=0;i<irac.getCount();i++)if(irac.getItemAtPosition(i).toString().equals(x))irac.setSelection(i);}if(date!=null)date.setText("Date & Time: "+new SimpleDateFormat("dd-MM-yyyy HH:mm",Locale.US).format(new Date(c.getLong(c.getColumnIndexOrThrow("dt")))));photoPath=nvl(c.getString(c.getColumnIndexOrThrow("photo")));currentGps=nvl(c.getString(c.getColumnIndexOrThrow("gps")));}c.close();}

    private LinearLayout photoControls(){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(12),dp(10),dp(12),dp(10));box.setBackground(outlined(Color.WHITE,BORDER,12));TextView label=text("PHOTOGRAPH",12,BLUE);label.setTypeface(null,Typeface.BOLD);box.addView(label);photoStatus=text(photoPath.isEmpty()?"No photograph attached":"Photograph attached",13,MUTED);box.addView(photoStatus);LinearLayout row=new LinearLayout(this);Button take=button("TAKE PHOTO",true);Button choose=button("CHOOSE GALLERY",false);take.setOnClickListener(v->takePhoto());choose.setOnClickListener(v->choosePhoto());row.addView(take,new LinearLayout.LayoutParams(0,dp(48),1));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(48),1);cp.setMargins(dp(8),0,0,0);row.addView(choose,cp);box.addView(row);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(10));body.addView(box,lp);return box;}
    private TextView gpsControls(){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(12),dp(10),dp(12),dp(10));box.setBackground(outlined(Color.WHITE,BORDER,12));TextView label=text("GPS LOCATION",12,BLUE);label.setTypeface(null,Typeface.BOLD);box.addView(label);gpsStatus=text(currentGps.isEmpty()?"Location not captured":currentGps,13,MUTED);box.addView(gpsStatus);Button get=button("CAPTURE CURRENT GPS",false);get.setOnClickListener(v->captureGps());box.addView(get);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(12));body.addView(box,lp);return gpsStatus;}

    private void takePhoto(){photoTargetRequest=REQ_CAMERA;if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.CAMERA},REQ_CAMERA);return;}Intent i=new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);startActivityForResult(i,REQ_CAMERA);}
    private void choosePhoto(){photoTargetRequest=REQ_GALLERY;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,REQ_GALLERY);}
    private void captureGps(){if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQ_LOCATION);return;}try{locationManager=(LocationManager)getSystemService(LOCATION_SERVICE);locationListener=new LocationListener(){@Override public void onLocationChanged(Location l){currentGps=String.format(Locale.US,"%.6f, %.6f",l.getLatitude(),l.getLongitude());if(gpsStatus!=null)gpsStatus.setText("Captured: "+currentGps);try{locationManager.removeUpdates(this);}catch(Exception ignored){}}};if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)&&!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){gpsStatus.setText("Location services are disabled");startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));return;}String provider=locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)?LocationManager.GPS_PROVIDER:LocationManager.NETWORK_PROVIDER;locationManager.requestLocationUpdates(provider,1000,1,locationListener);Location last=locationManager.getLastKnownLocation(provider);if(last!=null){currentGps=String.format(Locale.US,"%.6f, %.6f",last.getLatitude(),last.getLongitude());gpsStatus.setText("Captured: "+currentGps);}else gpsStatus.setText("Waiting for current GPS fix…");}catch(Exception e){gpsStatus.setText("GPS unavailable: "+e.getMessage());}}

    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){super.onRequestPermissionsResult(requestCode,permissions,results);if(requestCode==REQ_CAMERA&&results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)takePhoto();if(requestCode==REQ_LOCATION&&results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)captureGps();if(requestCode==REQ_NOTIFICATION&&results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)toast("Notifications enabled");}

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(resultCode!=RESULT_OK||data==null)return;if(requestCode==REQ_CAMERA){Object raw=data.getExtras()==null?null:data.getExtras().get("data");if(raw instanceof Bitmap){try{File dir=new File(getExternalFilesDir(null),"photos");if(!dir.exists())dir.mkdirs();File f=new File(dir,"visit_"+System.currentTimeMillis()+".jpg");FileOutputStream out=new FileOutputStream(f);((Bitmap)raw).compress(Bitmap.CompressFormat.JPEG,92,out);out.close();photoPath=f.getAbsolutePath();if(photoStatus!=null)photoStatus.setText("Photograph attached: "+f.getName());}catch(Exception e){toast("Unable to save photograph");}}}else if(requestCode==REQ_GALLERY){saveGallery(data.getData());}else if(requestCode==REQ_MASTER||requestCode==REQ_EXCEL){importExcel(data.getData());}}
    private void saveGallery(Uri uri){try{File dir=new File(getExternalFilesDir(null),"photos");if(!dir.exists())dir.mkdirs();File f=new File(dir,"visit_"+System.currentTimeMillis()+".jpg");InputStream in=getContentResolver().openInputStream(uri);FileOutputStream out=new FileOutputStream(f);copy(in,out);if(in!=null)in.close();photoPath=f.getAbsolutePath();if(photoStatus!=null)photoStatus.setText("Photograph attached: "+f.getName());}catch(Exception e){toast("Unable to attach photograph");}}
    private void copy(InputStream in,OutputStream out)throws IOException{byte[]buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);out.close();}

    private void govList(){list("Government Office Visits","Government Office Visits",()->govForm(0));}
    private void crossList(){list("Cross Selling","Cross Selling",()->crossForm(0,"SBI LIFE"));}
    private void loanList(){list("Loans & Advances","Loans & Advances",()->loanForm(0,"LOANS"));}
    private void list(String title,String head,Runnable add){base(title);action("＋  CREATE NEW VISIT",true,add);action("GENERATE HEAD PDF",false,()->generateHeadAndShow(head));section("MANAGE VISITS");Cursor c=db().headVisits(head);int n=0;while(c.moveToNext()){addVisitCard(c,head);n++;}c.close();if(n==0)infoBox("No visits have been recorded under this head yet.");}
    private void addVisitCard(Cursor c,String head){int id=c.getInt(c.getColumnIndexOrThrow("id"));String name=c.getString(c.getColumnIndexOrThrow("name"));String sub=c.getString(c.getColumnIndexOrThrow("subhead"));String account=c.getString(c.getColumnIndexOrThrow("account"));long dt=c.getLong(c.getColumnIndexOrThrow("dt"));LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(14),dp(12),dp(14),dp(12));card.setBackground(outlined(Color.WHITE,BORDER,14));TextView a=text(name,16,TEXT);a.setTypeface(null,Typeface.BOLD);card.addView(a);card.addView(text("A/c: "+account+"   •   "+sub,12,MUTED));card.addView(text("Date: "+fmt(dt),12,MUTED));LinearLayout row=new LinearLayout(this);Button view=button("VIEW PDF",true);Button edit=button("EDIT",false);Button del=button("DELETE",false);row.addView(view,new LinearLayout.LayoutParams(0,dp(48),1));LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(0,dp(48),1);ep.setMargins(dp(6),0,0,0);row.addView(edit,ep);LinearLayout.LayoutParams dpv=new LinearLayout.LayoutParams(0,dp(48),1);dpv.setMargins(dp(6),0,0,0);row.addView(del,dpv);card.addView(row);view.setOnClickListener(v->{File f=createIndividualPdf(id);if(f!=null)showPdfResult(f);});edit.setOnClickListener(v->{if(head.equals("Government Office Visits"))govForm(id);else if(head.equals("Cross Selling"))crossForm(id,sub);else loanForm(id,sub);});del.setOnClickListener(v->confirmDelete(id,head));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(10));body.addView(card,lp);}
    private void confirmDelete(int id,String head){new AlertDialog.Builder(this).setTitle("Delete visit?").setMessage("This inspection record will be permanently deleted.").setNegativeButton("CANCEL",null).setPositiveButton("DELETE",(d,w)->{db().deleteVisit(id);if(head.equals("Government Office Visits"))govList();else if(head.equals("Cross Selling"))crossList();else loanList();}).show();}

    private void customer360(){
        base("Customer 360");
        infoBox("Search the Master Customer File or inspection history by Account Number, Mobile Number or Customer Name.");
        EditText query=field("Account Number / Mobile Number / Customer Name");
        Button search=button("SEARCH CUSTOMER",true);
        body.addView(search,new LinearLayout.LayoutParams(-1,dp(56)));
        LinearLayout results=new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        body.addView(results);
        query.setOnEditorActionListener((v,a,event)->{search.performClick();return true;});
        search.setOnClickListener(v->{
            results.removeAllViews();
            String q=query.getText().toString().trim();
            if(q.isEmpty()){query.setError("Enter account number, mobile number or customer name");query.requestFocus();return;}
            Cursor c=db().searchCustomer360(q);
            int count=0;
            while(c.moveToNext()){count++;addCustomerResult(results,c);}
            c.close();
            if(count==0){
                infoBox("No matching customer found. Search checks both the Master Customer File and saved inspection records. Upload the Master Customer File if the customer has not yet been imported.");
            }
        });
    }
    private void addCustomerResult(LinearLayout parent,Cursor c){String name=nvl(c.getString(c.getColumnIndexOrThrow("name")));String account=nvl(c.getString(c.getColumnIndexOrThrow("account")));String phone=nvl(c.getString(c.getColumnIndexOrThrow("phone")));String address=nvl(c.getString(c.getColumnIndexOrThrow("address")));String irac=nvl(c.getString(c.getColumnIndexOrThrow("irac")));LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(14),dp(14),dp(14),dp(14));card.setBackground(outlined(Color.WHITE,BORDER,14));TextView h=text(name.isEmpty()?"Unnamed Customer":name,18,TEXT);h.setTypeface(null,Typeface.BOLD);card.addView(h);card.addView(text("Account Number: "+account,13,MUTED));card.addView(text("Mobile: "+phone,13,MUTED));card.addView(text("Address: "+address,13,MUTED));if(!irac.isEmpty())card.addView(text("IRAC Status: "+irac,13,MUTED));LinearLayout actions=new LinearLayout(this);Button call=button("CALL",true);Button sms=button("SMS",false);Button wa=button("WHATSAPP",false);actions.addView(call,new LinearLayout.LayoutParams(0,dp(48),1));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,dp(48),1);sp.setMargins(dp(6),0,0,0);actions.addView(sms,sp);LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(0,dp(48),1);wp.setMargins(dp(6),0,0,0);actions.addView(wa,wp);card.addView(actions);Button history=button("VIEW INTERACTION HISTORY",false);card.addView(history,new LinearLayout.LayoutParams(-1,dp(48)));call.setOnClickListener(v->dial(phone));sms.setOnClickListener(v->sms(phone));wa.setOnClickListener(v->whatsapp(phone));history.setOnClickListener(v->history(account,name,phone));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(10));parent.addView(card,lp);}
    private void history(String account,String customer,String phone){base("Customer Interaction History");infoBox(customer+"\nAccount: "+account+"\nMobile: "+phone);LinearLayout comm=new LinearLayout(this);Button call=button("CALL",true);Button sms=button("SMS",false);Button wa=button("WHATSAPP",false);comm.addView(call,new LinearLayout.LayoutParams(0,dp(50),1));LinearLayout.LayoutParams p1=new LinearLayout.LayoutParams(0,dp(50),1);p1.setMargins(dp(6),0,0,0);comm.addView(sms,p1);LinearLayout.LayoutParams p2=new LinearLayout.LayoutParams(0,dp(50),1);p2.setMargins(dp(6),0,0,0);comm.addView(wa,p2);body.addView(comm);call.setOnClickListener(v->dial(phone));sms.setOnClickListener(v->sms(phone));wa.setOnClickListener(v->whatsapp(phone));section("INTERACTION HISTORY");Cursor c=db().history(account);int n=0;while(c.moveToNext()){n++;LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(14),dp(12),dp(14),dp(12));card.setBackground(outlined(Color.WHITE,BORDER,12));String head=c.getString(c.getColumnIndexOrThrow("head"));String sub=c.getString(c.getColumnIndexOrThrow("subhead"));card.addView(text(head+"  •  "+sub,15,TEXT));card.addView(text("Purpose: "+nvl(c.getString(c.getColumnIndexOrThrow("purpose"))),13,MUTED));card.addView(text("Remarks: "+nvl(c.getString(c.getColumnIndexOrThrow("remarks"))),13,MUTED));card.addView(text("GPS: "+nvl(c.getString(c.getColumnIndexOrThrow("gps"))),13,MUTED));card.addView(text("Date: "+fmt(c.getLong(c.getColumnIndexOrThrow("dt"))),12,MUTED));Button pdf=button("VIEW PDF",false);card.addView(pdf,new LinearLayout.LayoutParams(-1,dp(46)));int id=c.getInt(c.getColumnIndexOrThrow("id"));pdf.setOnClickListener(v->{File f=createIndividualPdf(id);if(f!=null)showPdfResult(f);});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(10));body.addView(card,lp);}c.close();if(n==0)infoBox("No inspection history found for this account number.");}
    private void dial(String phone){try{startActivity(new Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+cleanPhone(phone))));}catch(Exception e){toast("Dialer not available");}}
    private void sms(String phone){try{startActivity(new Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:"+cleanPhone(phone))));}catch(Exception e){toast("Messaging app not available");}}
    private void whatsapp(String phone){String p=cleanPhone(phone);if(p.length()==0){toast("Mobile number is missing");return;}try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://wa.me/"+p)));}catch(Exception e){toast("WhatsApp is not available");}}
    private String cleanPhone(String p){return nvl(p).replaceAll("[^0-9+]","").replace("+","+");}

    private void masterFile(){base("Master Customer File");infoBox("Upload the complete customer list from Excel. Updating the master never deletes or overwrites inspection history.");action("UPLOAD MASTER CUSTOMER FILE",true,()->pickExcel(REQ_MASTER));action("UPLOAD EXCEL & FETCH DATA",false,()->pickExcel(REQ_EXCEL));section("MASTER CUSTOMER SUMMARY");TextView count=text("Customers in Master: "+db().count("customers"),16,TEXT);body.addView(count);action("OPEN CUSTOMER 360",false,()->customer360());}
    private void pickExcel(int request){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,request);}
    private void importExcel(Uri uri){try{ArrayList<ArrayList<String>> rows=Xlsx.read(uri,getContentResolver());if(rows.size()<2){toast("Excel file has no usable data rows");return;}ArrayList<String> h=rows.get(0);int name=findHeader(h,"name","customer");int account=findHeader(h,"account","a/c","a/c no","account number");int phone=findHeader(h,"mobile","contact","phone");int address=findHeader(h,"address");int irac=findHeader(h,"irac");int imported=0;for(int r=1;r<rows.size();r++){ArrayList<String> row=rows.get(r);String n=cell(row,name),a=cell(row,account);if(n.trim().isEmpty()&&a.trim().isEmpty())continue;db().upsertCustomer(n,a,cell(row,phone),cell(row,address),cell(row,irac));imported++;}toast(imported+" customer rows imported/updated");masterFile();}catch(Exception e){toast("Excel import failed: "+e.getMessage());}}
    private int findHeader(ArrayList<String> h,String...keys){for(int i=0;i<h.size();i++){String x=nvl(h.get(i)).toLowerCase(Locale.US).replace("_"," ").trim();for(String k:keys)if(x.contains(k.toLowerCase(Locale.US)))return i;}return -1;}
    private String cell(ArrayList<String> row,int i){return i>=0&&i<row.size()?nvl(row.get(i)):"";}

    private void pendingWork(){base("Pending Work");infoBox("Create, modify, complete or delete follow-up tasks. Status is calculated from the due date/time and local notifications can be scheduled.");action("＋  ADD PENDING TASK",true,()->taskDialog(0));section("TASKS");Cursor c=db().tasks();int n=0;while(c.moveToNext()){n++;addTaskCard(c);}c.close();if(n==0)infoBox("No pending work found.");}
    private void taskDialog(int taskId){
        boolean editing=taskId>0;LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(6),0,dp(6),0);
        EditText d=new EditText(this);d.setHint("Task description");form.addView(d,new LinearLayout.LayoutParams(-1,dp(56)));
        Spinner h=new Spinner(this);h.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Government Office Visits","Cross Selling","Loans & Advances"}));form.addView(h,new LinearLayout.LayoutParams(-1,dp(56)));
        EditText rel=new EditText(this);rel.setHint("Customer / Department");form.addView(rel,new LinearLayout.LayoutParams(-1,dp(56)));
        EditText due=new EditText(this);due.setHint("Due date/time: dd-MM-yyyy HH:mm");form.addView(due,new LinearLayout.LayoutParams(-1,dp(56)));
        EditText rem=new EditText(this);rem.setHint("Remarks");rem.setSingleLine(false);rem.setMinLines(3);form.addView(rem,new LinearLayout.LayoutParams(-1,dp(100)));
        if(editing){Cursor c=db().task(taskId);if(c.moveToFirst()){d.setText(c.getString(c.getColumnIndexOrThrow("description")));rel.setText(c.getString(c.getColumnIndexOrThrow("related")));due.setText(c.getString(c.getColumnIndexOrThrow("due")));rem.setText(c.getString(c.getColumnIndexOrThrow("remarks")));String hv=c.getString(c.getColumnIndexOrThrow("head"));for(int i=0;i<h.getCount();i++)if(h.getItemAtPosition(i).toString().equals(hv))h.setSelection(i);}c.close();}
        AlertDialog dlg=new AlertDialog.Builder(this).setTitle(editing?"Modify Pending Work":"Add Pending Work").setView(form).setNegativeButton("CANCEL",null).setPositiveButton(editing?"UPDATE":"SAVE",null).create();
        dlg.setOnShowListener(x->{dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{if(d.getText().toString().trim().isEmpty()){toast("Task description is required");return;}if(editing){db().updateTask(taskId,d.getText().toString(),h.getSelectedItem().toString(),rel.getText().toString(),due.getText().toString(),rem.getText().toString());scheduleTaskAlarm(due.getText().toString());toast("Pending work modified successfully");}else{db().addTask(d.getText().toString(),h.getSelectedItem().toString(),rel.getText().toString(),due.getText().toString(),rem.getText().toString());scheduleTaskAlarm(due.getText().toString());toast("Pending work added successfully");}dlg.dismiss();pendingWork();});});dlg.show();
    }
    private void addTaskCard(Cursor c){
        int id=c.getInt(0);String status=taskStatus(c.getString(4),c.getString(6));LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(14),dp(12),dp(14),dp(12));card.setBackground(outlined(Color.WHITE,BORDER,12));
        TextView title=text(c.getString(1),16,TEXT);title.setTypeface(null,Typeface.BOLD);card.addView(title);card.addView(text(c.getString(2)+"  •  "+c.getString(3),13,MUTED));TextView st=text(status+"  •  Due: "+c.getString(4),13,status.equals("OVERDUE")?RED:(status.equals("TODAY")?BLUE:GREEN));st.setTypeface(null,Typeface.BOLD);card.addView(st);card.addView(text("Remarks: "+nvl(c.getString(5)),13,MUTED));
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);actions.setPadding(0,dp(8),0,0);Button modify=button("MODIFY",false);Button delete=button("DELETE",false);LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(0,dp(48),1);actions.addView(modify,mp);LinearLayout.LayoutParams xp=new LinearLayout.LayoutParams(0,dp(48),1);xp.setMargins(dp(8),0,0,0);actions.addView(delete,xp);
        if(!"COMPLETED".equals(status)){Button done=button("MARK COMPLETE",true);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(48),1);cp.setMargins(dp(8),0,0,0);actions.addView(done,cp);done.setOnClickListener(v->{db().completeTask(id);pendingWork();});}
        modify.setOnClickListener(v->taskDialog(id));delete.setOnClickListener(v->confirmDeleteTask(id));card.addView(actions);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(10));body.addView(card,lp);
    }
    private void confirmDeleteTask(int id){new AlertDialog.Builder(this).setTitle("Delete pending work?").setMessage("This task will be permanently deleted.").setNegativeButton("CANCEL",null).setPositiveButton("DELETE",(d,w)->{db().deleteTask(id);toast("Pending work deleted");pendingWork();}).show();}
    private String taskStatus(String due,String stored){if("COMPLETED".equals(stored))return "COMPLETED";try{Date d=new SimpleDateFormat("dd-MM-yyyy HH:mm",Locale.US).parse(due);if(d==null)return "UPCOMING";long now=System.currentTimeMillis(),day=24*60*60*1000L;if(d.getTime()<now)return "OVERDUE";if(d.getTime()<now+day)return "TODAY";return "UPCOMING";}catch(Exception e){return "UPCOMING";}}
    private void scheduleTaskAlarm(String due){try{Date d=new SimpleDateFormat("dd-MM-yyyy HH:mm",Locale.US).parse(due);if(d==null||d.getTime()<=System.currentTimeMillis())return;AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);Intent i=new Intent(this,TaskReceiver.class);PendingIntent pi=PendingIntent.getBroadcast(this,(int)(d.getTime()%Integer.MAX_VALUE),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,d.getTime(),pi);if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQ_NOTIFICATION);}catch(Exception ignored){}}

    private void reports(){base("Reports");infoBox("All generated PDFs are stored in the app Reports folder and remain visible here. Each record provides the PDF path and an OPEN PDF action.");section("GENERATE REPORTS");action("GOVERNMENT OFFICE VISITS — PDF",true,()->generateHeadAndShow("Government Office Visits"));action("CROSS SELLING — PDF",false,()->generateHeadAndShow("Cross Selling"));action("LOANS & ADVANCES — PDF",false,()->generateHeadAndShow("Loans & Advances"));action("CONSOLIDATED PDF — ALL VISITS",false,()->{Cursor all=db().allVisits();File f=createReportPdf(all,"CONSOLIDATED VISIT REPORT");all.close();if(f!=null)showPdfResult(f);});action("REFRESH GENERATED REPORTS",false,()->reports());section("GENERATED PDF FILES");File dir=reportsDir();File[] files=dir.listFiles((d,n)->n.toLowerCase(Locale.US).endsWith(".pdf"));if(files==null||files.length==0){infoBox("No generated reports yet.");return;}Arrays.sort(files,(a,b)->Long.compare(b.lastModified(),a.lastModified()));for(File f:files)addReportFile(f);}
    private void addReportFile(File f){LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(14),dp(12),dp(14),dp(12));card.setBackground(outlined(Color.WHITE,BORDER,12));card.addView(text(f.getName(),15,TEXT));card.addView(text("Saved: "+f.getAbsolutePath(),11,MUTED));card.addView(text("Size: "+(f.length()/1024)+" KB",11,MUTED));Button open=button("OPEN PDF",true);card.addView(open,new LinearLayout.LayoutParams(-1,dp(48)));open.setOnClickListener(v->openPdf(f));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(10));body.addView(card,lp);}
    private void generateHeadAndShow(String head){Cursor c=db().headVisits(head);File f=createReportPdf(c,head.toUpperCase(Locale.US)+" REPORT");c.close();if(f!=null)showPdfResult(f);}
    private void showPdfResult(File f){new AlertDialog.Builder(this).setTitle("PDF GENERATED SUCCESSFULLY").setMessage("File:\n"+f.getName()+"\n\nSaved to:\n"+f.getAbsolutePath()).setNegativeButton("CLOSE",null).setPositiveButton("OPEN PDF",(d,w)->openPdf(f)).show();}
    private void openPdf(File f){try{Uri uri=ReportFileProvider.uriFor(f);Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(uri,"application/pdf");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(i);}catch(Exception e){toast("No PDF viewer is installed. Saved path:\n"+f.getAbsolutePath());}}

    private File createIndividualPdf(int id){Cursor c=db().one(id);if(!c.moveToFirst()){c.close();toast("Visit not found");return null;}File f=createSinglePdf(c);c.close();return f;}
    private File createSinglePdf(Cursor c){
        try{
            File out=newReportFile("VISIT_REPORT_"+System.currentTimeMillis()+".pdf");
            android.graphics.pdf.PdfDocument pdf=new android.graphics.pdf.PdfDocument();
            PdfPageBuilder page=new PdfPageBuilder(pdf,1);
            drawHeader(page.canvas,page.paint,"VISIT REPORT");
            String[][] rows={
                {"Report Reference", "Individual Inspection"},
                {"Name / Department",nvl(c.getString(c.getColumnIndexOrThrow("name")))},
                {"Account Number",nvl(c.getString(c.getColumnIndexOrThrow("account")))},
                {"Contact Number",nvl(c.getString(c.getColumnIndexOrThrow("phone")))},
                {"Address",nvl(c.getString(c.getColumnIndexOrThrow("address")))},
                {"Purpose of Visit",nvl(c.getString(c.getColumnIndexOrThrow("purpose")))},
                {"Interest / IRAC",nvl(c.getString(c.getColumnIndexOrThrow("interest")))},
                {"GPS",nvl(c.getString(c.getColumnIndexOrThrow("gps")))},
                {"Date & Time",new SimpleDateFormat("dd-MM-yyyy HH:mm",Locale.US).format(new Date(c.getLong(c.getColumnIndexOrThrow("dt"))))},
                {"Visiting Officials Remarks",nvl(c.getString(c.getColumnIndexOrThrow("remarks")))}
            };
            page=drawDetailRows(pdf,page,rows,"VISIT REPORT");
            drawFooter(page.canvas,page.paint);
            pdf.finishPage(page.page);
            addPhotoPageIfPresent(pdf,page.paint,nvl(c.getString(c.getColumnIndexOrThrow("photo"))),pdf.getPages().size()+1);
            pdf.writeTo(new FileOutputStream(out));
            pdf.close();
            return out;
        }catch(Exception e){toast("PDF generation failed: "+e.getMessage());return null;}
    }

    private PdfPageBuilder drawDetailRows(android.graphics.pdf.PdfDocument pdf,PdfPageBuilder page,String[][] rows,String title){
        int y=188;
        drawDetailTableHeader(page.canvas,page.paint,y);
        y+=44;
        for(String[] row:rows){
            String label=nvl(row[0]);
            ArrayList<String> lines=wrapPdfText(nvl(row[1]),12,360);
            int lineIndex=0;
            boolean firstChunk=true;
            while(lineIndex<lines.size()){
                int available=Math.max(1,(780-y-18)/18);
                if(available<=0){
                    drawFooter(page.canvas,page.paint);
                    pdf.finishPage(page.page);
                    page=new PdfPageBuilder(pdf,pdf.getPages().size()+1);
                    drawHeader(page.canvas,page.paint,title+" — CONTINUED");
                    y=188;
                    drawDetailTableHeader(page.canvas,page.paint,y);
                    y+=44;
                    available=Math.max(1,(780-y-18)/18);
                }
                int take=Math.min(available,lines.size()-lineIndex);
                int rowH=take*18+18;
                Canvas c=page.canvas;
                Paint p=page.paint;
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1);
                p.setColor(BORDER);
                c.drawRect(35,y,560,y+rowH,p);
                c.drawLine(185,y,185,y+rowH,p);
                p.setStyle(Paint.Style.FILL);
                p.setColor(TEXT);
                p.setTypeface(REPORT_FONT_BOLD);
                p.setTextSize(12);
                c.drawText(firstChunk?label:label+" (continued)",43,y+22,p);
                p.setTypeface(REPORT_FONT);
                p.setTextSize(12);
                int ty=y+22;
                for(int i=0;i<take;i++){
                    c.drawText(lines.get(lineIndex+i),193,ty,p);
                    ty+=18;
                }
                y+=rowH;
                lineIndex+=take;
                firstChunk=false;
                if(lineIndex<lines.size()){
                    drawFooter(c,p);
                    pdf.finishPage(page.page);
                    page=new PdfPageBuilder(pdf,pdf.getPages().size()+1);
                    drawHeader(page.canvas,page.paint,title+" — CONTINUED");
                    y=188;
                    drawDetailTableHeader(page.canvas,page.paint,y);
                    y+=44;
                }
            }
        }
        return page;
    }

    private void drawDetailTableHeader(Canvas c,Paint p,int y){
        p.setStyle(Paint.Style.FILL);
        p.setColor(LIGHT);
        c.drawRect(35,y,560,y+44,p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1);
        p.setColor(BORDER);
        c.drawRect(35,y,560,y+44,p);
        c.drawLine(185,y,185,y+44,p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(TEXT);
        p.setTypeface(REPORT_FONT_BOLD);
        p.setTextSize(12);
        c.drawText("FIELD",43,y+27,p);
        c.drawText("DETAILS",193,y+27,p);
    }

    private ArrayList<String> wrapPdfText(String value,float size,float maxWidth){
        ArrayList<String> out=new ArrayList<>();
        String normalized=nvl(value).replace("\r"," ").replace("\n"," ").trim();
        if(normalized.isEmpty()){out.add("");return out;}
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setTypeface(REPORT_FONT);p.setTextSize(size);
        StringBuilder line=new StringBuilder();
        for(String word:normalized.split("\\s+")){
            String candidate=line.length()==0?word:line+" "+word;
            if(p.measureText(candidate)<=maxWidth){line.setLength(0);line.append(candidate);}
            else{if(line.length()>0)out.add(line.toString());line.setLength(0);line.append(word);}
        }
        if(line.length()>0)out.add(line.toString());
        return out;
    }

    private File createReportPdf(Cursor c,String title){
        try{
            File out=newReportFile(title.replaceAll("[^A-Za-z0-9]+","_")+"_"+System.currentTimeMillis()+".pdf");
            android.graphics.pdf.PdfDocument pdf=new android.graphics.pdf.PdfDocument();
            int count=0;
            while(c.moveToNext()){
                int pageNo=pdf.getPages().size()+1;
                PdfPageBuilder page=new PdfPageBuilder(pdf,pageNo);
                Paint paint=page.paint;
                drawHeader(page.canvas,paint,title);
                String[][] rows={
                    {"Visit Number",String.valueOf(count+1)},
                    {"Name / Department",nvl(c.getString(c.getColumnIndexOrThrow("name")))},
                    {"Controlling Person",nvl(c.getString(c.getColumnIndexOrThrow("controlling")))},
                    {"Account Number",nvl(c.getString(c.getColumnIndexOrThrow("account")))},
                    {"Contact Number",nvl(c.getString(c.getColumnIndexOrThrow("phone")))},
                    {"Address",nvl(c.getString(c.getColumnIndexOrThrow("address")))},
                    {"Purpose of Visit",nvl(c.getString(c.getColumnIndexOrThrow("purpose")))},
                    {"Interest / IRAC Status",nvl(c.getString(c.getColumnIndexOrThrow("interest")))},
                    {"GPS Location",nvl(c.getString(c.getColumnIndexOrThrow("gps")))},
                    {"Date & Time",new SimpleDateFormat("dd-MM-yyyy HH:mm",Locale.US).format(new Date(c.getLong(c.getColumnIndexOrThrow("dt"))))},
                    {"Visiting Officials Remarks",nvl(c.getString(c.getColumnIndexOrThrow("remarks")))}
                };
                page=drawDetailRows(pdf,page,rows,title+" — VISIT "+(count+1));
                drawFooter(page.canvas,page.paint);
                pdf.finishPage(page.page);
                count++;
            }
            if(count==0){toast("No visits available for this report");pdf.close();out.delete();return null;}
            pdf.writeTo(new FileOutputStream(out));
            pdf.close();
            return out;
        }catch(Exception e){
            toast("PDF generation failed: "+e.getMessage());
            return null;
        }
    }
    private void addPhotoPageIfPresent(android.graphics.pdf.PdfDocument pdf,Paint p,String path,int pageNo){if(path==null||path.isEmpty())return;File f=new File(path);if(!f.exists())return;Bitmap b=BitmapFactory.decodeFile(path);if(b==null)return;PdfPageBuilder pg=new PdfPageBuilder(pdf,pageNo);Canvas c=pg.canvas;drawHeader(c,p,"INSPECTION PHOTOGRAPH");Paint border=new Paint(Paint.ANTI_ALIAS_FLAG);border.setStyle(Paint.Style.STROKE);border.setStrokeWidth(2);c.drawRect(45,170,550,690,border);Rect src=new Rect(0,0,b.getWidth(),b.getHeight());RectF dst=fitRect(b,45,170,505,520);c.drawBitmap(b,src,dst,p);drawFooter(c,p);pdf.finishPage(pg.page);b.recycle();}
    private RectF fitRect(Bitmap b,float x,float y,float w,float h){float scale=Math.min(w/b.getWidth(),h/b.getHeight());float nw=b.getWidth()*scale,nh=b.getHeight()*scale;return new RectF(x+(w-nw)/2,y+(h-nh)/2,x+(w-nw)/2+nw,y+(h-nh)/2+nh);}
    private void drawHeader(Canvas c,Paint p,String title){
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.WHITE);
        c.drawRect(0,0,595,842,p);
        p.setColor(BLUE);
        c.drawRect(0,0,595,7,p);

        drawOriginalSbiPdfLogo(c,p);

        p.setTextAlign(Paint.Align.CENTER);
        p.setColor(DARK);
        p.setTypeface(REPORT_FONT_BOLD);
        p.setTextSize(18);
        c.drawText("STATE BANK OF INDIA",298,96,p);

        p.setTypeface(REPORT_FONT);
        p.setTextSize(12);
        p.setColor(MUTED);
        c.drawText(db().setting("branch","BRANCH NAME"),298,116,p);
        c.drawText("CODE: "+db().setting("code","------"),298,134,p);

        p.setColor(BLUE);
        p.setTypeface(REPORT_FONT_BOLD);
        p.setTextSize(16);
        c.drawText(title,298,160,p);
        p.setTextAlign(Paint.Align.LEFT);
    }

    private void drawOriginalSbiPdfLogo(Canvas c, Paint p){
        Bitmap logo = BitmapFactory.decodeResource(getResources(), R.drawable.sbi_original);
        if(logo==null)return;
        float maxW=175f, maxH=38f;
        Rect src=new Rect(0,0,logo.getWidth(),logo.getHeight());
        RectF dst=fitRect(logo,210,26,maxW,maxH);
        Paint lp=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG|Paint.DITHER_FLAG);
        c.drawBitmap(logo,src,dst,lp);
        logo.recycle();
    }

    private int drawTable(Canvas c,Paint p,String[][] rows,int y,int width){
        for(String[] r:rows){
            ArrayList<String> lines=wrapPdfText(nvl(r[1]),12,350);
            int h=Math.max(46,lines.size()*18+18);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1);
            p.setColor(BORDER);
            c.drawRect(35,y,560,y+h,p);
            c.drawLine(185,y,185,y+h,p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(TEXT);
            p.setTypeface(REPORT_FONT_BOLD);
            p.setTextSize(12);
            c.drawText(r[0],43,y+23,p);
            p.setTypeface(REPORT_FONT);
            p.setTextSize(12);
            int ty=y+23;
            for(String line:lines){c.drawText(line,193,ty,p);ty+=18;}
            y+=h;
        }
        return y;
    }

    private void drawGridHeader(Canvas c,Paint p,String[] h,int y){
        float[] x={30,92,225,385,470,565};
        int rh=44;
        p.setStyle(Paint.Style.FILL);
        p.setColor(LIGHT);
        c.drawRect(30,y,565,y+rh,p);
        p.setStyle(Paint.Style.STROKE);
        p.setColor(BORDER);
        for(int i=0;i<x.length-1;i++)c.drawRect(x[i],y,x[i+1],y+rh,p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(TEXT);
        p.setTypeface(REPORT_FONT_BOLD);
        p.setTextSize(12);
        for(int i=0;i<h.length;i++){
            ArrayList<String> lines=wrapPdfText(h[i],12,x[i+1]-x[i]-8);
            int ty=y+19;
            for(String line:lines){c.drawText(line,x[i]+4,ty,p);ty+=15;}
        }
    }

    private int drawGridRow(Canvas c,Paint p,String[] v,int y){
        float[] x={30,92,225,385,470,565};
        ArrayList<ArrayList<String>> all=new ArrayList<>();
        int max=1;
        for(int i=0;i<v.length;i++){
            ArrayList<String> lines=wrapPdfText(v[i],12,x[i+1]-x[i]-8);
            all.add(lines);
            max=Math.max(max,lines.size());
        }
        int h=Math.max(48,max*18+16);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1);
        p.setColor(BORDER);
        for(int i=0;i<x.length-1;i++)c.drawRect(x[i],y,x[i+1],y+h,p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(TEXT);
        p.setTypeface(REPORT_FONT);
        p.setTextSize(12);
        for(int i=0;i<all.size();i++){
            int ty=y+21;
            for(String line:all.get(i)){c.drawText(line,x[i]+4,ty,p);ty+=18;}
        }
        return h;
    }

    private String trimForPdf(String s,int chars){s=nvl(s).replace("\n"," ");return s.length()<=chars?s:s.substring(0,Math.max(0,chars-1))+"…";}
    private void drawFooter(Canvas c,Paint p){
        p.setColor(MUTED);
        p.setTypeface(REPORT_FONT);
        p.setTextSize(11);
        c.drawText("Visiting Official: "+db().setting("official_name",""),30,795,p);
        c.drawText("Designation: "+db().setting("designation",""),30,812,p);
        c.drawText("PF ID: "+db().setting("pfid",""),30,829,p);
        c.drawText("Signature: __________________________",350,812,p);
    }
    private static class PdfPageBuilder { android.graphics.pdf.PdfDocument.Page page; Canvas canvas; Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG); PdfPageBuilder(android.graphics.pdf.PdfDocument pdf,int n){page=pdf.startPage(new android.graphics.pdf.PdfDocument.PageInfo.Builder(595,842,n).create());canvas=page.getCanvas();} }

    private void settingsPage(){base("Settings");infoBox("Set the branch and visiting official details used in PDF reports. No security/PIN module is included.");EditText branch=field("Branch Name");EditText code=field("Branch Code");EditText name=field("Visiting Official Name");EditText designation=field("Designation");EditText pfid=field("PF ID");branch.setText(db().setting("branch",""));code.setText(db().setting("code",""));name.setText(db().setting("official_name",""));designation.setText(db().setting("designation",""));pfid.setText(db().setting("pfid",""));action("SAVE SETTINGS",true,()->{db().saveSetting("branch",branch.getText().toString());db().saveSetting("code",code.getText().toString());db().saveSetting("official_name",name.getText().toString());db().saveSetting("designation",designation.getText().toString());db().saveSetting("pfid",pfid.getText().toString());toast("Settings saved");});action("DELETE ALL VISIT LEADS",false,()->new AlertDialog.Builder(this).setTitle("Delete all visits?").setMessage("This removes inspection history but keeps the Master Customer File.").setNegativeButton("CANCEL",null).setPositiveButton("DELETE",(d,w)->{db().deleteAllVisits();toast("All visit leads deleted");}).show());}

    private void showPdfResultFromPath(File f){showPdfResult(f);}
    private File reportsDir(){File d=new File(getExternalFilesDir(null),"reports");if(!d.exists())d.mkdirs();return d;}
    private File newReportFile(String name){File d=reportsDir();return new File(d,name);}
    private String fmt(long x){return new SimpleDateFormat("dd-MM-yyyy HH:mm",Locale.US).format(new Date(x));}
    private String nvl(String x){return x==null?"":x;}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private DB db(){if(database==null)database=new DB(this);return database;}

    public static class TaskReceiver extends BroadcastReceiver { @Override public void onReceive(Context c,Intent i){if(Build.VERSION.SDK_INT>=26){NotificationManager nm=(NotificationManager)c.getSystemService(NOTIFICATION_SERVICE);nm.createNotificationChannel(new NotificationChannel("sbi_tasks","Pending Work",NotificationManager.IMPORTANCE_DEFAULT));}Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,"sbi_tasks"):new Notification.Builder(c);b.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("SBI Inspection App").setContentText("Pending work is due").setAutoCancel(true);((NotificationManager)c.getSystemService(NOTIFICATION_SERVICE)).notify((int)(System.currentTimeMillis()%Integer.MAX_VALUE),b.build());}}

    public static class ReportFileProvider extends ContentProvider {
        public static Uri uriFor(File f){return Uri.parse("content://com.sbi.inspection.files/"+Uri.encode(f.getAbsolutePath(),""));}
        @Override public boolean onCreate(){return true;}
        @Override public String getType(Uri uri){return "application/pdf";}
        @Override public Cursor query(Uri uri,String[] projection,String selection,String[] selectionArgs,String sortOrder){return null;}
        @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
        @Override public int delete(Uri uri,String selection,String[] selectionArgs){throw new UnsupportedOperationException();}
        @Override public int update(Uri uri,ContentValues values,String selection,String[] selectionArgs){throw new UnsupportedOperationException();}
        @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{String path=Uri.decode(uri.getPath().substring(1));return ParcelFileDescriptor.open(new File(path),ParcelFileDescriptor.MODE_READ_ONLY);}
    }

    private static class DB extends SQLiteOpenHelper {
        DB(Context c){super(c,"sbi_inspection.db",null,4);}
        @Override public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE visits(id INTEGER PRIMARY KEY AUTOINCREMENT,head TEXT,subhead TEXT,name TEXT,account TEXT,phone TEXT,address TEXT,purpose TEXT,interest TEXT,remarks TEXT,gps TEXT,photo TEXT,controlling TEXT,dt INTEGER)");d.execSQL("CREATE TABLE customers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,account TEXT UNIQUE,phone TEXT,address TEXT,irac TEXT)");d.execSQL("CREATE TABLE tasks(id INTEGER PRIMARY KEY AUTOINCREMENT,description TEXT,head TEXT,related TEXT,due TEXT,remarks TEXT,status TEXT)");d.execSQL("CREATE TABLE settings(k TEXT PRIMARY KEY,v TEXT)");}
        @Override public void onUpgrade(SQLiteDatabase d,int oldV,int newV){if(oldV<4){try{d.execSQL("ALTER TABLE visits ADD COLUMN controlling TEXT");}catch(Exception ignored){}}}
        SQLiteDatabase w(){return getWritableDatabase();}
        Cursor one(int id){return w().query("visits",null,"id=?",new String[]{String.valueOf(id)},null,null,null);}
        Cursor headVisits(String head){return w().query("visits",null,"head=?",new String[]{head},null,null,"dt DESC");}
        Cursor allVisits(){return w().query("visits",null,null,null,null,null,"head ASC, subhead ASC, dt DESC");}
        Cursor history(String account){return w().query("visits",null,"account=?",new String[]{account},null,null,"dt DESC");}
        void saveVisit(int id,String head,String sub,String name,String account,String phone,String address,String purpose,String interest,String remarks,String gps,String photo,String controlling,long dt){ContentValues v=new ContentValues();v.put("head",head);v.put("subhead",sub);v.put("name",name);v.put("account",account);v.put("phone",phone);v.put("address",address);v.put("purpose",purpose);v.put("interest",interest);v.put("remarks",remarks);v.put("gps",gps);v.put("photo",photo);v.put("controlling",controlling);v.put("dt",dt);if(id==0)w().insert("visits",null,v);else w().update("visits",v,"id=?",new String[]{String.valueOf(id)});}
        void deleteVisit(int id){w().delete("visits","id=?",new String[]{String.valueOf(id)});}void deleteAllVisits(){w().delete("visits",null,null);}
        int count(String table){Cursor c=w().rawQuery("SELECT COUNT(*) FROM "+table,null);c.moveToFirst();int n=c.getInt(0);c.close();return n;}int pendingCount(){Cursor c=w().rawQuery("SELECT COUNT(*) FROM tasks WHERE status IS NULL OR status<>?",new String[]{"COMPLETED"});c.moveToFirst();int n=c.getInt(0);c.close();return n;}
        void upsertCustomer(String n,String a,String p,String ad,String ir){if(a==null)a="";a=a.trim();ContentValues v=new ContentValues();v.put("name",n);v.put("account",a);v.put("phone",p);v.put("address",ad);v.put("irac",ir);if(a.isEmpty()){w().insert("customers",null,v);}else w().insertWithOnConflict("customers",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
        Cursor searchCustomer360(String q){
            String z=q.trim().toLowerCase(Locale.US);
            String like="%"+z+"%";
            String digits=z.replaceAll("[^0-9]","");
            String digitLike="%"+digits+"%";
            String sql=
                "SELECT name,account,phone,address,irac FROM customers WHERE " +
                "lower(name) LIKE ? OR lower(account) LIKE ? OR lower(phone) LIKE ? OR " +
                "(? <> '' AND replace(replace(replace(phone,' ',''),'-',''),'+' ,'') LIKE ?) " +
                "UNION " +
                "SELECT name,account,phone,address,interest AS irac FROM visits WHERE " +
                "lower(name) LIKE ? OR lower(account) LIKE ? OR lower(phone) LIKE ? OR " +
                "(? <> '' AND replace(replace(replace(phone,' ',''),'-',''),'+' ,'') LIKE ?) " +
                "ORDER BY name COLLATE NOCASE ASC";
            return w().rawQuery(sql,new String[]{like,like,like,digits,digitLike,like,like,like,digits,digitLike});
        }
        Cursor searchCustomers(String q){return searchCustomer360(q);}        void addTask(String d,String h,String rel,String due,String rem){ContentValues v=new ContentValues();v.put("description",d);v.put("head",h);v.put("related",rel);v.put("due",due);v.put("remarks",rem);v.put("status","PENDING");w().insert("tasks",null,v);}
        Cursor tasks(){return w().query("tasks",null,null,null,null,null,"id DESC");}
        Cursor task(int id){return w().query("tasks",null,"id=?",new String[]{String.valueOf(id)},null,null,null);}
        void updateTask(int id,String d,String h,String rel,String due,String rem){ContentValues v=new ContentValues();v.put("description",d);v.put("head",h);v.put("related",rel);v.put("due",due);v.put("remarks",rem);w().update("tasks",v,"id=?",new String[]{String.valueOf(id)});}
        void deleteTask(int id){w().delete("tasks","id=?",new String[]{String.valueOf(id)});}
        void completeTask(int id){ContentValues v=new ContentValues();v.put("status","COMPLETED");w().update("tasks",v,"id=?",new String[]{String.valueOf(id)});}
        String setting(String k,String def){Cursor c=w().query("settings",new String[]{"v"},"k=?",new String[]{k},null,null,null);String x=def;if(c.moveToFirst())x=nvlStatic(c.getString(0));c.close();return x;}void saveSetting(String k,String v){ContentValues x=new ContentValues();x.put("k",k);x.put("v",v);w().insertWithOnConflict("settings",null,x,SQLiteDatabase.CONFLICT_REPLACE);}
        private static String nvlStatic(String x){return x==null?"":x;}
    }

    private static class Xlsx {
        static ArrayList<ArrayList<String>> read(Uri uri,ContentResolver cr)throws Exception{File tmp=File.createTempFile("sbi_import_",".xlsx");InputStream in=cr.openInputStream(uri);FileOutputStream out=new FileOutputStream(tmp);byte[]buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);in.close();out.close();ZipFile zip=new ZipFile(tmp);ArrayList<String> shared=new ArrayList<>();ZipEntry ss=zip.getEntry("xl/sharedStrings.xml");if(ss!=null)readShared(zip.getInputStream(ss),shared);ArrayList<ArrayList<String>> best=new ArrayList<>();for(int i=1;i<=20;i++){ZipEntry e=zip.getEntry("xl/worksheets/sheet"+i+".xml");if(e==null)break;ArrayList<ArrayList<String>> rows=parseSheet(zip.getInputStream(e),shared);if(rows.size()>best.size())best=rows;}zip.close();tmp.delete();return best;}
        static void readShared(InputStream in,ArrayList<String> shared)throws Exception{XmlPullParser p=Xml.newPullParser();p.setInput(in,"UTF-8");StringBuilder cur=new StringBuilder();boolean inSi=false;for(int e=p.getEventType();e!=XmlPullParser.END_DOCUMENT;e=p.next()){if(e==XmlPullParser.START_TAG&&"si".equals(p.getName())){inSi=true;cur.setLength(0);}else if(e==XmlPullParser.TEXT&&inSi)cur.append(p.getText());else if(e==XmlPullParser.END_TAG&&"si".equals(p.getName())){shared.add(cur.toString());inSi=false;}}in.close();}
        static ArrayList<ArrayList<String>> parseSheet(InputStream in,ArrayList<String> shared)throws Exception{
            XmlPullParser p=Xml.newPullParser();p.setInput(in,"UTF-8");
            ArrayList<ArrayList<String>> rows=new ArrayList<>();
            ArrayList<String> row=null;String value="";String type="";int col=0;
            boolean inInline=false;StringBuilder inline=new StringBuilder();
            for(int e=p.getEventType();e!=XmlPullParser.END_DOCUMENT;e=p.next()){
                if(e==XmlPullParser.START_TAG&&"row".equals(p.getName())){row=new ArrayList<>();col=0;}
                else if(e==XmlPullParser.START_TAG&&"c".equals(p.getName())){type=nvlStatic(p.getAttributeValue(null,"t"));String ref=p.getAttributeValue(null,"r");col=columnFromRef(ref);value="";}
                else if(e==XmlPullParser.START_TAG&&"is".equals(p.getName())){inInline=true;inline.setLength(0);}
                else if(e==XmlPullParser.TEXT&&inInline){inline.append(p.getText());}
                else if(e==XmlPullParser.START_TAG&&"v".equals(p.getName())){
                    value=p.nextText();
                    if("s".equals(type)){try{int ix=Integer.parseInt(value);value=ix>=0&&ix<shared.size()?shared.get(ix):value;}catch(Exception ignored){}}
                    if(row!=null){while(row.size()<=col)row.add("");row.set(col,value);}
                }
                else if(e==XmlPullParser.END_TAG&&"is".equals(p.getName())){
                    value=inline.toString();inInline=false;
                    if(row!=null){while(row.size()<=col)row.add("");row.set(col,value);}
                }
                else if(e==XmlPullParser.END_TAG&&"row".equals(p.getName())){if(row!=null&&!row.isEmpty())rows.add(row);row=null;}
            }
            in.close();return rows;
        }
        static int columnFromRef(String ref){if(ref==null)return 0;int n=0;for(int i=0;i<ref.length()&&Character.isLetter(ref.charAt(i));i++)n=n*26+(Character.toUpperCase(ref.charAt(i))-'A'+1);return Math.max(0,n-1);}static String nvlStatic(String x){return x==null?"":x;}
    }
}
