package dev.clean.tvhome;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.tv.*;
import android.net.*;
import android.net.wifi.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.view.animation.OvershootInterpolator;
import android.widget.*;
import androidx.recyclerview.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** Local-only TV home. Cached local wallpaper blur; no webviews or polling; on-demand LAN wallpaper uploads. */
public class MainActivity extends Activity {
    final Handler handler = new Handler(Looper.getMainLooper());
    final ArrayList<App> apps = new ArrayList<>(), drawerApps=new ArrayList<>();
    final HashSet<String> hiddenApps=new HashSet<>();
    int appColumns=6;
    boolean movingApp,okHeld,okLongPress;
    Scene scene;
    int columns, selected, topIndex;
    boolean top, gameFocused, idle, resumed, allApps;
    final Runnable appOptionsTask=()->{if(okHeld&&allApps){okLongPress=true;showAppOptions();}};
    int favouriteSelection;
    long wallpaperStamp=-1;

    Typeface font, thin;
    PopupWindow popup;
    final Runnable idleTask = () -> {if(popup==null||!popup.isShowing())setIdle(true);};
    final Runnable clockTask = new Runnable() { public void run() {
        if (scene != null) scene.updateClock();
        if (resumed) handler.postDelayed(this, 60000 - System.currentTimeMillis()%60000);
    }};
    static class App {
        String pkg, name; Intent intent; Bitmap banner, icon, reflection; int color=0xff6589c4;
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(5894);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        columns=Math.max(4,Math.min(7,getResources().getInteger(R.integer.columns)));
        font=Typeface.create("sans-serif",Typeface.NORMAL);
        thin=Typeface.create(getResources().getFont(R.font.outfit),200,false);
        discover();
        appColumns=Math.max(4,Math.min(7,getPreferences(0).getInt("app_columns",6)));
        hiddenApps.addAll(getPreferences(0).getStringSet("hidden_apps",Collections.emptySet()));
        rebuildDrawer();
        String last=getPreferences(0).getString("last_app","");
        for(int i=0;i<Math.min(columns,apps.size());i++) if(apps.get(i).pkg.equals(last)) selected=i;
        allApps=false;
        favouriteSelection=Math.min(selected,Math.max(0,columns-1));
        scene=new Scene();setContentView(scene);
    }
    @Override protected void onResume() {
        super.onResume();resumed=true;idle=false;
        if(scene!=null) {long stamp=WallpaperStore.file(this).lastModified();if(stamp!=wallpaperStamp){scene.wallpaper.setImageBitmap(WallpaperStore.load(this,1));scene.blurredWallpaper.setImageBitmap(makeBlurredWallpaper());wallpaperStamp=stamp;}setIdle(false);scene.sync(false);scene.updateNetwork();}
        handler.removeCallbacks(clockTask);handler.post(clockTask);touch();
    }
    @Override protected void onPause() {
        super.onPause();resumed=false;handler.removeCallbacks(clockTask);handler.removeCallbacks(idleTask);
        okHeld=false;handler.removeCallbacks(appOptionsTask);
        if(popup!=null)popup.dismiss();
    }
    @Override protected void onDestroy(){super.onDestroy();handler.removeCallbacksAndMessages(null);}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIdle(false);touch();}
    void touch(){handler.removeCallbacks(idleTask);if(resumed)handler.postDelayed(idleTask,getResources().getInteger(R.integer.idle_seconds)*1000L);}
    void setIdle(boolean value){
        if(scene==null)return;idle=value;
        scene.ui.animate().alpha(value?0:1).setDuration(650).start();
        scene.shade.animate().alpha(value?0:1).setDuration(850).start();
        scene.blurredWallpaper.animate().alpha(!value&&allApps?1:0).setDuration(350).start();
        scene.glass.animate().alpha(value?0:1).setDuration(350).start();
        scene.wallpaper.animate().scaleX(value?1.015f:1f).scaleY(value?1.015f:1f).setDuration(1000).start();
    }
    void discover(){
        PackageManager pm=getPackageManager();LinkedHashMap<String,ResolveInfo> found=new LinkedHashMap<>();
        for(String cat:new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER}) {
            Intent q=new Intent(Intent.ACTION_MAIN).addCategory(cat);
            for(ResolveInfo r:pm.queryIntentActivities(q,0)) {
                String p=r.activityInfo.packageName;
                if(p.equals(getPackageName())||p.equals("com.amazon.tv.launcher")||p.equals("com.amazon.firehomestarter")||p.startsWith("com.mediatek.")||p.startsWith("com.vewd.")||p.startsWith("com.android.")||p.contains("earlyaccess")||p.contains("servicemenu")||p.matches("(?i).*(factory|production|fac.menu|debug).*"))continue;
                if(!found.containsKey(p)) found.put(p,r);
            }
        }
        ArrayList<String> order=new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.dock_apps)));
        ArrayList<String> other=new ArrayList<>(found.keySet());other.removeAll(order);
        other.sort((a,b)->label(a,found.get(a)).compareToIgnoreCase(label(b,found.get(b))));order.addAll(other);
        for(String p:order) {
            ResolveInfo r=found.get(p);if(r==null)continue;
            App app=new App();app.pkg=p;app.name=label(p,r);
            app.intent=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER).setComponent(new ComponentName(p,r.activityInfo.name)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            Drawable d=null;
            try {d=r.activityInfo.loadBanner(pm);if(d==null)d=pm.getApplicationBanner(p);}catch(Exception ignored){}
            boolean hasBanner=d!=null;
            if(d==null){
                d=r.loadIcon(pm);app.icon=Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888);
                d.setBounds(0,0,128,128);d.draw(new Canvas(app.icon));
            }
            app.banner=Bitmap.createBitmap(320,180,Bitmap.Config.ARGB_8888);
            Canvas c=new Canvas(app.banner);c.drawColor(0xff151821);
            if(d!=null){if(hasBanner)d.setBounds(0,0,320,180);else d.setBounds(100,30,220,150);d.draw(c);}
            long rr=0,gg=0,bb=0,n=0;
            for(int y=0;y<180;y+=12)for(int x=0;x<320;x+=12){int color=app.banner.getPixel(x,y);if(Color.red(color)+Color.green(color)+Color.blue(color)>100){rr+=Color.red(color);gg+=Color.green(color);bb+=Color.blue(color);n++;}}
            if(n>0)app.color=Color.rgb((int)(rr/n),(int)(gg/n),(int)(bb/n));
            apps.add(app);
        }
    }
    String label(String p,ResolveInfo r){
        switch(p){case "com.amazon.firetv.youtube":return "YouTube";case "com.amazon.firebat":return "Prime Video";case "in.startv.hotstar":return "JioHotstar";case "com.netflix.ninja":return "Netflix";case "com.tataskymore.open.uat":return "Tata Play";case "com.zee5.amazon":return "ZEE5";case "tv.airtel.xstream.tvapp":return "Airtel Xstream";}
        return String.valueOf(r.loadLabel(getPackageManager()));
    }
    @Override public boolean dispatchKeyEvent(KeyEvent e){
        int k=e.getKeyCode();
        if(popup!=null&&popup.isShowing()){touch();return super.dispatchKeyEvent(e);}
        if(allApps&&k==KeyEvent.KEYCODE_MENU){if(e.getAction()==KeyEvent.ACTION_DOWN&&e.getRepeatCount()==0){touch();setIdle(false);showAppOptions();}return true;}
        if(allApps&&(popup==null||!popup.isShowing())&&(k==KeyEvent.KEYCODE_DPAD_CENTER||k==KeyEvent.KEYCODE_ENTER)){
            touch();
            if(idle){if(e.getAction()==KeyEvent.ACTION_DOWN)setIdle(false);return true;}
            if(e.getAction()==KeyEvent.ACTION_DOWN&&e.getRepeatCount()==0){okHeld=true;okLongPress=false;handler.postDelayed(appOptionsTask,550);}
            if(e.getAction()==KeyEvent.ACTION_DOWN&&e.isLongPress()&&okHeld&&!okLongPress){okLongPress=true;handler.removeCallbacks(appOptionsTask);showAppOptions();}
            if(e.getAction()==KeyEvent.ACTION_UP){handler.removeCallbacks(appOptionsTask);boolean launch=okHeld&&!okLongPress;okHeld=false;if(launch){if(movingApp){movingApp=false;scene.sync(true);}else if(!drawerApps.isEmpty())safeStart(drawerApps.get(selected).intent);else showAppOptions();}}
            return true;
        }
        boolean nav=k==KeyEvent.KEYCODE_DPAD_UP||k==KeyEvent.KEYCODE_DPAD_DOWN||k==KeyEvent.KEYCODE_DPAD_LEFT||k==KeyEvent.KEYCODE_DPAD_RIGHT||k==KeyEvent.KEYCODE_DPAD_CENTER||k==KeyEvent.KEYCODE_ENTER||k==KeyEvent.KEYCODE_BACK;
        if(!nav)return super.dispatchKeyEvent(e);
        if(e.getAction()!=KeyEvent.ACTION_DOWN)return true;
        touch();if(idle){setIdle(false);return true;}
        if(popup!=null&&popup.isShowing())return super.dispatchKeyEvent(e);
        if(k==KeyEvent.KEYCODE_BACK&&movingApp){movingApp=false;scene.sync(true);return true;}
        if(k==KeyEvent.KEYCODE_BACK){top=false;gameFocused=false;setAppsMode(false);scene.sync(true);return true;}
        if(k==KeyEvent.KEYCODE_DPAD_CENTER||k==KeyEvent.KEYCODE_ENTER){
            if(e.getRepeatCount()==0){
                if(top){if(topIndex==0)showInputs();else openSettings(topIndex==1);}
                else if(gameFocused)playPS5();
                else if(!currentApps().isEmpty())safeStart(currentApps().get(selected).intent);
            }return true;
        }
        if(top){
            if(k==KeyEvent.KEYCODE_DPAD_LEFT)topIndex=Math.max(0,topIndex-1);
            if(k==KeyEvent.KEYCODE_DPAD_RIGHT)topIndex=Math.min(2,topIndex+1);
            if(k==KeyEvent.KEYCODE_DPAD_DOWN){top=false;gameFocused=true;}
        }else if(gameFocused){
            if(k==KeyEvent.KEYCODE_DPAD_UP){gameFocused=false;top=true;}
            if(k==KeyEvent.KEYCODE_DPAD_DOWN){gameFocused=false;selected=Math.min(selected,Math.max(0,columns-1));}
        }else if(!currentApps().isEmpty()){
            if(!allApps){
                if(k==KeyEvent.KEYCODE_DPAD_LEFT&&selected>0)selected--;
                if(k==KeyEvent.KEYCODE_DPAD_RIGHT&&selected+1<Math.min(columns,apps.size()))selected++;
                favouriteSelection=selected;
                if(k==KeyEvent.KEYCODE_DPAD_UP){top=true;topIndex=0;}
                if(k==KeyEvent.KEYCODE_DPAD_DOWN)setAppsMode(true);
            }else{
                int previous=selected;int row=selected/appColumns,col=selected%appColumns;
                if(k==KeyEvent.KEYCODE_DPAD_LEFT&&col>0)selected--;
                if(k==KeyEvent.KEYCODE_DPAD_RIGHT&&col<appColumns-1&&selected+1<drawerApps.size())selected++;
                if(k==KeyEvent.KEYCODE_DPAD_UP){if(row==0){if(!movingApp)setAppsMode(false);}else selected-=appColumns;}
                if(k==KeyEvent.KEYCODE_DPAD_DOWN&&(row+1)*appColumns<drawerApps.size())selected=Math.min(selected+appColumns,drawerApps.size()-1);
                if(movingApp&&selected!=previous){Collections.swap(drawerApps,previous,selected);saveDrawerOrder();scene.grid.getAdapter().notifyDataSetChanged();}
            }
            if(!currentApps().isEmpty())getPreferences(0).edit().putString("last_app",currentApps().get(selected).pkg).apply();
        }
        scene.sync(true);return true;
    }
    ArrayList<App> currentApps(){return allApps?drawerApps:apps;}
    void setAppsMode(boolean open){
        if(open){String pkg=apps.isEmpty()?"":apps.get(Math.min(selected,apps.size()-1)).pkg;allApps=true;selected=0;for(int i=0;i<drawerApps.size();i++)if(drawerApps.get(i).pkg.equals(pkg))selected=i;}
        else{allApps=false;movingApp=false;selected=Math.min(favouriteSelection,Math.max(0,Math.min(columns,apps.size())-1));}
    }
    void rebuildDrawer(){
        drawerApps.clear();
        ArrayList<String> order=new ArrayList<>(Arrays.asList(getPreferences(0).getString("app_order","").split("\n")));
        for(App app:apps)if(!order.contains(app.pkg))order.add(app.pkg);
        for(String pkg:order)for(App app:apps)if(app.pkg.equals(pkg)&&!hiddenApps.contains(pkg)){drawerApps.add(app);break;}
    }
    void saveDrawerOrder(){
        ArrayList<String> order=new ArrayList<>();for(App app:drawerApps)order.add(app.pkg);
        // Retain hidden positions after the visible order so restoring never loses an app.
        for(String pkg:getPreferences(0).getString("app_order","").split("\n"))if(!pkg.isEmpty()&&!order.contains(pkg))order.add(pkg);
        getPreferences(0).edit().putString("app_order",String.join("\n",order)).apply();
    }
    void refreshDrawer(){selected=Math.max(0,Math.min(selected,drawerApps.size()-1));scene.firstVisibleRow=-1;scene.sync(false);}
    void showAppMenu(String title,LinearLayout list){
        okHeld=false;handler.removeCallbacks(appOptionsTask);handler.removeCallbacks(idleTask);
        TextView heading=new TextView(this);heading.setText(title);heading.setTextColor(0xffd4def1);heading.setTextSize(15);heading.setPadding(24,16,24,16);list.addView(heading,0);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.addView(list);scroll.setBackground(round(0xf0222939,22));
        popup=new PopupWindow(scroll,Math.round(350*scene.s),Math.min(Math.round(440*scene.s),list.getChildCount()*Math.round(43*scene.s)),true);
        popup.setBackgroundDrawable(round(0xf0222939,22));popup.setElevation(24*scene.s);popup.setOutsideTouchable(true);
        popup.showAtLocation(scene,Gravity.CENTER,0,0);if(list.getChildCount()>1)list.getChildAt(1).requestFocus();
        popup.setOnDismissListener(()->{touch();scene.sync(false);});
    }
    void showAppOptions(){
        if(popup!=null&&popup.isShowing())return;
        LinearLayout list=menuList();
        if(!drawerApps.isEmpty()){
            App app=drawerApps.get(selected);
            addInput(list,"Move "+app.name,()->{movingApp=true;scene.sync(false);Toast.makeText(this,"Use arrows to move · OK to finish",Toast.LENGTH_LONG).show();});
            addInput(list,"Hide "+app.name,()->{hiddenApps.add(app.pkg);getPreferences(0).edit().putStringSet("hidden_apps",new HashSet<>(hiddenApps)).apply();rebuildDrawer();refreshDrawer();});
        }
        addInput(list,"Grid layout · "+appColumns+" columns",()->showGridOptions());
        addInput(list,"Hidden apps · "+hiddenApps.size(),()->showHiddenApps());
        addInput(list,"Done",()->{});
        showAppMenu("Customize All Apps",list);
    }
    void showGridOptions(){
        LinearLayout list=menuList();for(int count=4;count<=7;count++){final int value=count;addInput(list,(count==appColumns?"✓  ":"")+count+" columns",()->{appColumns=value;getPreferences(0).edit().putInt("app_columns",value).apply();refreshDrawer();});}
        showAppMenu("Grid layout",list);
    }
    void showHiddenApps(){
        LinearLayout list=menuList();
        for(App app:apps)if(hiddenApps.contains(app.pkg))addInput(list,"Show "+app.name,()->{hiddenApps.remove(app.pkg);getPreferences(0).edit().putStringSet("hidden_apps",new HashSet<>(hiddenApps)).apply();rebuildDrawer();refreshDrawer();showHiddenApps();});
        if(!hiddenApps.isEmpty())addInput(list,"Show all apps",()->{hiddenApps.clear();getPreferences(0).edit().remove("hidden_apps").apply();rebuildDrawer();refreshDrawer();});
        addInput(list,"Done",()->{});showAppMenu(hiddenApps.isEmpty()?"No hidden apps":"Hidden apps",list);
    }
    void safeStart(Intent i){try{startActivity(i);}catch(Exception e){android.util.Log.e("CleanTV","Cannot open "+i,e);Toast.makeText(this,"This app is unavailable",Toast.LENGTH_SHORT).show();}}
    void playPS5(){
        TvInputManager manager=(TvInputManager)getSystemService(TV_INPUT_SERVICE);
        try{for(TvInputInfo input:manager.getTvInputList()){
            if(input.getType()==TvInputInfo.TYPE_HDMI&&input.getParentId()==null&&input.getId().endsWith("/HW4")){switchInput(input.getId(),false);return;}
        }}catch(Exception e){android.util.Log.e("CleanTV","HDMI 3 discovery",e);}
        Toast.makeText(this,"HDMI 3 is unavailable",Toast.LENGTH_LONG).show();
    }
    void openSettings(boolean wifi){
        if(wifi){launchNativeSettings("com.amazon.tv.settings.v2.tv.network.NetworkActivity");return;}
        LinearLayout list=menuList();
        String[][] entries={
            {"Network","com.amazon.tv.settings.v2.tv.network.NetworkActivity"},
            {"Display & Sound","com.amazon.tv.settings.v2.tv.display_sounds.DisplayAndSoundsActivity"},
            {"Remotes & Bluetooth","com.amazon.tv.settings.v2.tv.controllers_bluetooth_devices.ControllersAndBluetoothActivity"},
            {"Applications","com.amazon.tv.settings.v2.tv.applications.ApplicationsActivity"},
            {"Preferences","com.amazon.tv.settings.v2.tv.preferences.PreferencesActivity"},
            {"Device & Software","com.amazon.tv.settings.v2.tv.device.DeviceActivity"}
        };
        for(String[] item:entries)addInput(list,item[0],()->launchNativeSettings(item[1]));
        addInput(list,"Wallpaper",()->safeStart(new Intent(this,WallpaperActivity.class)));
        addInput(list,"Restart…",()->safeStart(new Intent().setComponent(new ComponentName("com.amazon.tv.settings.v2","com.amazon.tv.settings.v2.tv.device.DeviceActivity")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK).putExtra("scrollTo","Restart")));
        showMenu(list,scene.pills[2],230);
    }
    void launchNativeSettings(String activity){
        Intent i=new Intent().setComponent(new ComponentName("com.amazon.tv.settings.v2",activity)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        safeStart(i);
    }
    LinearLayout menuList(){LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(12,12,12,12);list.setBackground(round(0xf026293b,18));return list;}
    void showMenu(LinearLayout list,View anchor,int width){
        popup=new PopupWindow(list,(int)(width*scene.s),WindowManager.LayoutParams.WRAP_CONTENT,true);
        popup.setBackgroundDrawable(round(0xf026293b,18));popup.setElevation(14*scene.s);popup.setOutsideTouchable(true);
        popup.showAsDropDown(anchor,Math.min(0,anchor.getWidth()-(int)(width*scene.s)),(int)(9*scene.s));
        if(list.getChildCount()>0)list.getChildAt(0).requestFocus();popup.setOnDismissListener(()->scene.sync(false));
    }
    void showInputs(){
        LinearLayout list=menuList();
        TvInputManager manager=(TvInputManager)getSystemService(TV_INPUT_SERVICE);
        ArrayList<TvInputInfo> inputs=new ArrayList<>();
        try{for(TvInputInfo info:manager.getTvInputList())if(info.getParentId()==null&&(info.getType()==TvInputInfo.TYPE_HDMI||info.getType()==TvInputInfo.TYPE_COMPOSITE||(info.getType()==TvInputInfo.TYPE_TUNER&&info.getServiceInfo().packageName.equals("com.mediatek.tis"))))inputs.add(info);}catch(Exception ignored){}
        inputs.sort(Comparator.comparing(TvInputInfo::getId));
        for(TvInputInfo info:inputs){
            String name=String.valueOf(info.loadLabel(this));if(info.getType()==TvInputInfo.TYPE_COMPOSITE)name="AV";if(info.getType()==TvInputInfo.TYPE_TUNER)name="Live TV";
            final String id=info.getId();final boolean tuner=info.getType()==TvInputInfo.TYPE_TUNER;addInput(list,name,()->switchInput(id,tuner));
        }
        if(inputs.stream().noneMatch(x->x.getType()==TvInputInfo.TYPE_TUNER))addInput(list,"Live TV",()->{
            Intent i=getPackageManager().getLaunchIntentForPackage("com.amazon.tv.livetv");if(i!=null)safeStart(i);else safeStart(new Intent(Intent.ACTION_VIEW,Uri.parse("content://android.media.tv/channel")));
        });
        showMenu(list,scene.pills[0],210);
    }
    void switchInput(String id,boolean tuner){
        if(tuner){safeStart(new Intent(Intent.ACTION_VIEW,TvContract.Channels.CONTENT_URI).setComponent(new ComponentName("com.amazon.tv.livetv","com.amazon.tv.livetv.TvChannelsPlayerActivityAlias")));return;}
        safeStart(new Intent(this,InputActivity.class).putExtra("input_id",id).putExtra("label",id.contains("Hdmi")?"HDMI "+(id.endsWith("HW2")?"1":id.endsWith("HW3")?"2":"3"):"AV"));
    }
    void addInput(LinearLayout list,String name,Runnable action){
        TextView row=new TextView(this);row.setText(name);row.setTextColor(Color.WHITE);row.setTypeface(font);row.setTextSize(16);row.setPadding(24,18,24,18);row.setFocusable(true);row.setClickable(true);
        row.setOnFocusChangeListener((v,f)->row.setBackground(round(f?0xff505c72:Color.TRANSPARENT,10)));
        row.setOnClickListener(v->{popup.dismiss();action.run();});list.addView(row);
    }
    GradientDrawable round(int color,float radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius*(scene==null?2:scene.s));return d;}

    Bitmap makeBlurredWallpaper(){
        BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=8;
        Bitmap source=WallpaperStore.load(this,8);
        Bitmap small=Bitmap.createBitmap(240,135,Bitmap.Config.ARGB_8888);
        float scale=Math.max(240f/source.getWidth(),135f/source.getHeight());
        float width=source.getWidth()*scale,height=source.getHeight()*scale;
        new Canvas(small).drawBitmap(source,null,new RectF((240-width)/2,(135-height)/2,(240+width)/2,(135+height)/2),new Paint(3));
        if(source!=small)source.recycle();
        int w=small.getWidth(),h=small.getHeight();int[] pixels=new int[w*h],temp=new int[w*h];small.getPixels(pixels,0,w,0,0,w,h);
        int radius=7;
        for(int pass=0;pass<3;pass++){
            for(int y=0;y<h;y++)for(int x=0;x<w;x++){
                int r=0,g=0,b=0;
                for(int d=-radius;d<=radius;d++){int color=pixels[y*w+Math.max(0,Math.min(w-1,x+d))];r+=Color.red(color);g+=Color.green(color);b+=Color.blue(color);}
                temp[y*w+x]=Color.rgb(r/15,g/15,b/15);
            }
            for(int y=0;y<h;y++)for(int x=0;x<w;x++){
                int r=0,g=0,b=0;
                for(int d=-radius;d<=radius;d++){int color=temp[Math.max(0,Math.min(h-1,y+d))*w+x];r+=Color.red(color);g+=Color.green(color);b+=Color.blue(color);}
                pixels[y*w+x]=Color.rgb(r/15,g/15,b/15);
            }
        }
        // Dark blue tint leaves the app artwork and focus ring as the brightest elements.
        for(int i=0;i<pixels.length;i++){int color=pixels[i];pixels[i]=Color.rgb(Color.red(color)/3+4,Color.green(color)/3+8,Color.blue(color)/3+14);}
        Bitmap result=Bitmap.createBitmap(pixels,w,h,Bitmap.Config.ARGB_8888);small.recycle();return result;
    }

    class Scene extends FrameLayout {
        float s=2;
        ImageView wallpaper,blurredWallpaper;View shade;FrameLayout ui;Glass glass;RecyclerView grid;GridLayoutManager layout;
        TextView greeting,clock,date,eyebrow,hint;TextView[] pills=new TextView[3];
        View band;Reflections reflections;GameCard game;int firstVisibleRow=-1;boolean showingApps;
        Scene(){super(MainActivity.this);setBackgroundColor(Color.BLACK);setClipChildren(true);
            wallpaper=new ImageView(getContext());wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);wallpaper.setImageBitmap(WallpaperStore.load(MainActivity.this,1));wallpaperStamp=WallpaperStore.file(MainActivity.this).lastModified();addView(wallpaper,new LayoutParams(-1,-1));
            blurredWallpaper=new ImageView(getContext());blurredWallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);blurredWallpaper.setImageBitmap(makeBlurredWallpaper());blurredWallpaper.setAlpha(0f);addView(blurredWallpaper,new LayoutParams(-1,-1));
            shade=new View(getContext());shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x18000000,0x08000000,0x40000000}));addView(shade,new LayoutParams(-1,-1));
            ui=new FrameLayout(getContext());ui.setClipChildren(false);addView(ui,new LayoutParams(-1,-1));
            greeting=text(getString(R.string.greeting),17,font,0xffeeeeF6);
            greeting.setText(getString(R.string.greeting));clock=text("",85,thin,Color.WHITE);date=text("",18,font,0xffc6c9da);
            for(int i=0;i<3;i++){
                final int index=i;pills[i]=text(new String[]{"Inputs","Wi-Fi","Settings"}[i],12,font,Color.WHITE);
                pills[i].setGravity(Gravity.CENTER);pills[i].setBackground(panel(0xc03b394e,0xb0202030,28));
                pills[i].setOnClickListener(v->{top=true;gameFocused=false;topIndex=index;sync(false);if(index==0)showInputs();else openSettings(index==1);});
            }
            game=new GameCard();ui.addView(game);game.setOnClickListener(v->playPS5());
            eyebrow=text("Your Favourites",14,font,0xffeeedf4);
            reflections=new Reflections();ui.addView(reflections);
            band=new View(getContext());band.setBackground(panel(0xc047455a,0xb01b1e30,29));ui.addView(band);
            grid=new RecyclerView(getContext());layout=new GridLayoutManager(getContext(),columns);grid.setLayoutManager(layout);grid.setAdapter(new Tiles());grid.setItemAnimator(null);grid.setClipChildren(false);grid.setClipToPadding(false);grid.setOverScrollMode(OVER_SCROLL_NEVER);grid.setFocusable(false);grid.setItemViewCacheSize(14);ui.addView(grid);
            grid.addOnScrollListener(new RecyclerView.OnScrollListener(){@Override public void onScrolled(RecyclerView r,int dx,int dy){updateHighlight(false);updateBand();reflections.invalidate();}});
            hint=text("⌄",14,font,0x99e3e4ef);hint.setGravity(Gravity.CENTER);
            glass=new Glass();addView(glass,new LayoutParams(-1,-1));glass.setClickable(false);setFocusable(true);requestFocus();updateClock();
        }
        GradientDrawable panel(int start,int end,float radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{start,end});d.setCornerRadius(radius*s);d.setStroke(Math.max(1,(int)(.8f*s)),0x556b6d86);return d;}
        TextView text(String value,float size,Typeface face,int color){TextView t=new TextView(getContext());t.setText(value);t.setTextColor(color);t.setTypeface(face);t.setIncludeFontPadding(false);t.setSingleLine();t.setTag(size);ui.addView(t);return t;}
        void place(View v,float x,float y,float w,float h){LayoutParams p=new LayoutParams(Math.round(w*s),Math.round(h*s));p.leftMargin=Math.round(x*s);p.topMargin=Math.round(y*s);v.setLayoutParams(p);if(v instanceof TextView){Object tag=v.getTag();if(tag instanceof Float)((TextView)v).setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,(Float)tag*s);}}
        @Override protected void onSizeChanged(int w,int h,int ow,int oh){super.onSizeChanged(w,h,ow,oh);s=w/960f;arrange();grid.getAdapter().notifyDataSetChanged();post(()->sync(false));}
        void arrange(){
            showingApps=allApps;band.setTranslationY(0);
            layout.setSpanCount(allApps?appColumns:columns);
            place(greeting,51,50,475,25);place(clock,46,83,400,98);place(date,51,170,420,29);
            greeting.setVisibility(allApps?INVISIBLE:VISIBLE);clock.setVisibility(allApps?INVISIBLE:VISIBLE);date.setVisibility(allApps?INVISIBLE:VISIBLE);
            place(pills[0],614,39,85,35);place(pills[1],711,39,101,35);place(pills[2],824,39,93,35);
            for(int i=0;i<3;i++){android.graphics.drawable.Drawable icon=getResources().getDrawable(new int[]{R.drawable.ic_inputs,R.drawable.ic_wifi,R.drawable.ic_settings}[i],null);icon.setBounds(0,0,(int)(15*s),(int)(15*s));pills[i].setCompoundDrawables(icon,null,null,null);pills[i].setCompoundDrawablePadding((int)(6*s));pills[i].setPadding((int)(10*s),0,(int)(10*s),0);pills[i].setVisibility(allApps?INVISIBLE:VISIBLE);}
            place(game,505,123,414,193);game.setVisibility(allApps?INVISIBLE:VISIBLE);
            place(eyebrow,55,348,700,23);eyebrow.setVisibility(allApps?INVISIBLE:VISIBLE);
            place(band,allApps?34:38,allApps?44:370,allApps?892:884,allApps?tileHeight()+50:111);
            band.setVisibility(VISIBLE);band.setBackground(allApps?panel(0x607f96af,0x30323e50,26):panel(0xc047455a,0xb01b1e30,29));
            place(reflections,51,484,858,56);reflections.setVisibility(allApps?INVISIBLE:VISIBLE);
            place(grid,allApps?42:51,allApps?62:379,allApps?876:858,allApps?454:111);
            place(hint,48,504,864,20);hint.setVisibility(allApps&&(movingApp||drawerApps.isEmpty())?VISIBLE:INVISIBLE);hint.setText(movingApp?"Move with arrows · OK to finish":drawerApps.isEmpty()?"No apps shown · Hold OK or press Menu to customize":"");
            firstVisibleRow=0;layout.scrollToPositionWithOffset(0,0);
            blurredWallpaper.animate().alpha(allApps?1:0).setDuration(320).start();
            for(TextView pill:pills)pill.setBackground(panel(0xc03b394e,0xb0202030,28));
        }
        void sync(boolean animate){
            if(getWidth()==0)return;
            if(showingApps!=allApps||firstVisibleRow<0){arrange();grid.getAdapter().notifyDataSetChanged();}
            if(allApps){
                int row=selected/appColumns;
                int visibleRows=Math.max(1,(int)(454/(tileHeight()+37)));
                int next=firstVisibleRow;
                if(row<next)next=row;
                if(row>=next+visibleRows)next=row-visibleRows+1;
                if(next!=firstVisibleRow){firstVisibleRow=next;layout.scrollToPositionWithOffset(next*appColumns,0);}
                updateBand();
            }
            for(int i=0;i<grid.getChildCount();i++){View v=grid.getChildAt(i);int p=grid.getChildAdapterPosition(v);if(v instanceof Tile&&p>=0&&p<currentApps().size())((Tile)v).bind(p);}
            hint.setVisibility(allApps&&(movingApp||drawerApps.isEmpty())?VISIBLE:INVISIBLE);hint.setText(movingApp?"Move with arrows · OK to finish":"No apps shown · Hold OK or press Menu to customize");
            game.invalidate();reflections.invalidate();post(()->updateHighlight(animate));
        }
        void updateBand(){
            if(!allApps)return;
            View first=layout.findViewByPosition(0);
            band.setVisibility(first!=null&&!drawerApps.isEmpty()?VISIBLE:INVISIBLE);
            band.setTranslationY(first==null?0:first.getTop());
        }
        float tileHeight(){return (876f/appColumns-20)*.60f;}
        void updateHighlight(boolean animate){
            glass.setVisibility(allApps&&drawerApps.isEmpty()?INVISIBLE:VISIBLE);
            if(top){View p=pills[topIndex];glass.move(new RectF(p.getLeft()-3*s,p.getTop()-3*s,p.getRight()+3*s,p.getBottom()+3*s),true,animate);return;}
            if(gameFocused){glass.move(new RectF(game.getLeft()-3*s,game.getTop()-3*s,game.getRight()+3*s,game.getBottom()+3*s),false,animate);return;}
            View child=layout.findViewByPosition(selected);if(child instanceof Tile){Tile tile=(Tile)child;float x=grid.getLeft()+tile.getLeft()+tile.picture.getLeft(),y=grid.getTop()+tile.getTop()+tile.picture.getTop();float inset=(allApps?2:5)*s;glass.move(new RectF(x-inset,y-inset,x+tile.picture.getWidth()+inset,y+tile.picture.getHeight()+inset),false,animate);}
        }
        void updateClock(){Date now=new Date();clock.setText(new SimpleDateFormat("h:mm",Locale.getDefault()).format(now));date.setText(new SimpleDateFormat("EEEE, d MMMM",Locale.getDefault()).format(now));}
        void updateNetwork(){
            String name="Wi-Fi";
            try{ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);NetworkCapabilities cap=cm.getNetworkCapabilities(cm.getActiveNetwork());
                if(cap==null)name="Offline";else if(cap.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))name="Ethernet";
                else{WifiManager wm=(WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);String ssid=wm.getConnectionInfo().getSSID();if(ssid!=null&&!ssid.equals("<unknown ssid>"))name=ssid.replace("\"","");}
            }catch(Exception ignored){}
            pills[1].setText(name);
        }
        class GameCard extends View {
            final Paint p=new Paint(3);final Bitmap controller;
            final RectF box=new RectF();final Path clip=new Path();
            GameCard(){super(Scene.this.getContext());BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=2;controller=BitmapFactory.decodeResource(getResources(),R.drawable.gamepad,opts);}
            void word(Canvas c,String value,float x,float y,float size,int weight,int color){p.setShader(null);p.setColor(color);p.setTypeface(Typeface.create(font,weight,false));p.setTextSize(size);c.drawText(value,x,y,p);}
            @Override protected void onDraw(Canvas original){Canvas c=original;int save=c.save();c.scale(getWidth()/414f,getHeight()/193f);box.set(0,0,414,193);clip.reset();clip.addRoundRect(box,28,28,Path.Direction.CW);c.clipPath(clip);
                p.setAlpha(255);p.setShader(new LinearGradient(0,0,414,193,new int[]{0xf344455e,0xed191c31,0xe532253a},null,Shader.TileMode.CLAMP));c.drawRoundRect(box,28,28,p);p.setShader(new RadialGradient(326,84,162,0x995e4dce,0x005e4dce,Shader.TileMode.CLAMP));c.drawRect(box,p);p.setShader(null);
                p.setAlpha(255);c.drawBitmap(controller,null,new RectF(177,35,407,180),p);
                word(c,"READY TO PLAY",27,52,10,400,0xffd0d0e2);word(c,"Game On",26,87,28,600,Color.WHITE);word(c,"Pick up where you left off",27,111,12,400,0xffd4d3e7);
                p.setColor(gameFocused?0xc08e89b9:0x997b789b);c.drawRoundRect(26,135,150,171,20,20,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.8f);p.setColor(0x88dad8f4);c.drawRoundRect(26,135,150,171,20,20,p);p.setStyle(Paint.Style.FILL);
                word(c,"▶",42,158,13,500,Color.WHITE);word(c,"Press to Play",61,158,12,400,Color.WHITE);
                c.restoreToCount(save);p.setColor(0x776e6f94);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(s*.8f);box.set(1,1,getWidth()-1,getHeight()-1);c.drawRoundRect(box,28*s,28*s,p);p.setStyle(Paint.Style.FILL);
            }
        }
        class Tiles extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent,int type){return new RecyclerView.ViewHolder(new Tile()){};}
            public int getItemCount(){return allApps?drawerApps.size():Math.min(columns,apps.size());}
            public void onBindViewHolder(RecyclerView.ViewHolder holder,int position){((Tile)holder.itemView).bind(position);}
        }
        class Tile extends FrameLayout {
            BannerView picture;TextView label;final Paint surface=new Paint(3);final Paint mirrorFade=new Paint(3);
            Tile(){super(Scene.this.getContext());picture=new BannerView();addView(picture);label=new TextView(getContext());label.setGravity(Gravity.CENTER);label.setTextColor(0xffeeeeF5);label.setTypeface(font);label.setSingleLine();label.setIncludeFontPadding(false);addView(label);setFocusable(false);setClipChildren(false);mirrorFade.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));}
            @Override protected void dispatchDraw(Canvas c){
                if(allApps&&picture.getWidth()>0){
                    float x=picture.getLeft(),y=picture.getTop(),w=picture.getWidth(),h=picture.getHeight();int tint=picture.app==null?0xff6488b0:picture.app.color;
                    int glow=c.save();c.translate(x+w*.5f,y+h+9*s);c.scale(w*.65f,30*s);surface.setColor(Color.WHITE);surface.setShader(new RadialGradient(0,0,1,Color.argb(55,Color.red(tint),Color.green(tint),Color.blue(tint)),0x00000000,Shader.TileMode.CLAMP));c.drawCircle(0,0,1,surface);c.restoreToCount(glow);surface.setShader(null);
                    surface.setColor(0x18000000);for(int i=5;i>0;i--)c.drawRoundRect(x-i*s*.5f,y+3*s-i*s*.4f,x+w+i*s*.5f,y+h+6*s+i*s*.5f,12*s,12*s,surface);
                    float bottom=y+h+4*s,depth=23*s;int layer=c.saveLayer(x,bottom,x+w,bottom+depth,null);int reflected=c.save();c.translate(x,bottom);c.scale(1,-.40f);c.translate(0,-h);
                    surface.setColor(Color.WHITE);surface.setFilterBitmap(true);c.drawBitmap(reflection(),null,new RectF(0,0,w,h),surface);c.restoreToCount(reflected);
                    mirrorFade.setShader(new LinearGradient(0,bottom,0,bottom+depth,new int[]{0x48ffffff,0x00ffffff},null,Shader.TileMode.CLAMP));c.drawRect(x,bottom,x+w,bottom+depth,mirrorFade);c.restoreToCount(layer);
                }
                super.dispatchDraw(c);
            }
            Bitmap reflection(){
                if(picture.app.reflection!=null)return picture.app.reflection;
                Bitmap bitmap=Bitmap.createBitmap(98,41,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.scale(98f/picture.getWidth(),41f/picture.getHeight());picture.draw(canvas);
                int w=98,h=41;int[] pixels=new int[w*h],blurred=new int[w*h];bitmap.getPixels(pixels,0,w,0,0,w,h);
                for(int pass=0;pass<2;pass++){
                    for(int y=0;y<h;y++)for(int x=0;x<w;x++){
                        int a=0,r=0,g=0,b=0;
                        for(int yy=-2;yy<=2;yy++)for(int xx=-2;xx<=2;xx++){int color=pixels[Math.max(0,Math.min(h-1,y+yy))*w+Math.max(0,Math.min(w-1,x+xx))];a+=Color.alpha(color);r+=Color.red(color);g+=Color.green(color);b+=Color.blue(color);}
                        blurred[y*w+x]=Color.argb(a/25,r/25,g/25,b/25);
                    }
                    int[] swap=pixels;pixels=blurred;blurred=swap;
                }
                bitmap.setPixels(pixels,0,w,0,0,w,h);picture.app.reflection=bitmap;return bitmap;
            }
            void bind(int p){float cell=allApps?876f/appColumns:858f/columns;float tw=cell-20,th=allApps?tileHeight():82;
                ViewGroup.LayoutParams cellParams=getLayoutParams();if(cellParams==null)cellParams=new RecyclerView.LayoutParams(Math.round(cell*s),Math.round((th+(allApps?37:31))*s));cellParams.width=Math.round(cell*s);cellParams.height=Math.round((th+(allApps?37:31))*s);setLayoutParams(cellParams);
                FrameLayout.LayoutParams a=new FrameLayout.LayoutParams(Math.round(tw*s),Math.round(th*s));a.leftMargin=Math.round(10*s);a.topMargin=Math.round(6*s);picture.setLayoutParams(a);
                FrameLayout.LayoutParams b=new FrameLayout.LayoutParams(Math.round(cell*s),Math.round(20*s));b.topMargin=Math.round((th+13)*s);label.setLayoutParams(b);label.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,11*s);
                App app=currentApps().get(p);picture.app=app;picture.dock=app.pkg.equals("com.amazon.firetv.youtube")||app.pkg.equals("com.netflix.ninja")||app.pkg.equals("com.amazon.firebat")||app.pkg.equals("in.startv.hotstar");picture.frosted=allApps;picture.invalidate();label.setText(app.name);boolean focused=p==selected&&!top&&!gameFocused;label.setAlpha(focused&&allApps?1f:0f);picture.animate().scaleX(focused?1.035f:1).scaleY(focused?1.035f:1).setDuration(170).start();
            }
        }
        /** Reuses the dock artwork; a small compositing layer fades the mirrored tiles. */
        class Reflections extends View {
            final Paint fade=new Paint(3);
            Reflections(){super(Scene.this.getContext());fade.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));}
            @Override protected void onSizeChanged(int w,int h,int ow,int oh){
                fade.setShader(new LinearGradient(0,0,0,h,new int[]{0x88ffffff,0x30ffffff,0x00ffffff},new float[]{0,.45f,1},Shader.TileMode.CLAMP));
            }
            @Override protected void onDraw(Canvas c){
                int layer=c.saveLayer(0,0,getWidth(),getHeight(),null);
                for(int i=0;i<columns;i++){
                    View child=layout.findViewByPosition(i);if(!(child instanceof Tile))continue;
                    BannerView picture=((Tile)child).picture;
                    int saved=c.save();
                    c.translate(child.getLeft()+picture.getLeft(),0);
                    c.scale(1,-1);c.translate(0,-picture.getHeight());
                    picture.draw(c);c.restoreToCount(saved);
                }
                c.drawRect(0,0,getWidth(),getHeight(),fade);c.restoreToCount(layer);
            }
        }
        class BannerView extends View {
            App app;boolean dock,frosted;final Paint p=new Paint(3);final RectF rect=new RectF();final Path path=new Path();
            BannerView(){super(Scene.this.getContext());}
            void word(Canvas c,String text,float x,float y,float size,Typeface face,int color){p.setShader(null);p.setTypeface(face);p.setTextSize(size);p.setColor(color);c.drawText(text,x,y,p);}
            @Override protected void onDraw(Canvas c){if(app==null)return;p.setColor(Color.WHITE);p.setAlpha(255);int save=c.save();c.scale(getWidth()/196f,getHeight()/82f);rect.set(0,0,196,82);path.reset();path.addRoundRect(rect,14,14,Path.Direction.CW);c.clipPath(path);
                if(!dock){
                    if(frosted){
                        int tint=app.color;
                        p.setShader(new LinearGradient(0,0,196,82,new int[]{Color.argb(220,Math.min(255,Color.red(tint)+32),Math.min(255,Color.green(tint)+32),Math.min(255,Color.blue(tint)+32)),0xe509111e},null,Shader.TileMode.CLAMP));c.drawRoundRect(rect,14,14,p);p.setShader(null);
                        // Transparent icons sit on tinted glass; TV banners keep their full artwork.
                        if(app.icon!=null)c.drawBitmap(app.icon,null,new RectF(68,11,128,71),p);
                        else c.drawBitmap(app.banner,null,rect,p);
                    }else c.drawBitmap(app.banner,null,rect,p);
                    if(frosted)drawFrost(c);
                    c.restoreToCount(save);return;
                }
                int[] colors=app.pkg.equals("com.amazon.firebat")?new int[]{0xff073b62,0xff071b35}:app.pkg.equals("in.startv.hotstar")?new int[]{0xff008deb,0xff8000ef,0xffe018a4}:new int[]{0xdd555561,0xe51c1d2b};
                p.setAlpha(255);p.setShader(new LinearGradient(0,0,196,82,colors,null,Shader.TileMode.CLAMP));c.drawRoundRect(rect,14,14,p);p.setShader(null);
                Typeface bold=Typeface.create("sans-serif",Typeface.BOLD);
                if(app.pkg.equals("com.amazon.firetv.youtube")){
                    p.setColor(0xffff0000);c.drawRoundRect(34,26,78,57,9,9,p);p.setColor(Color.WHITE);path.reset();path.moveTo(52,33);path.lineTo(52,50);path.lineTo(66,41.5f);path.close();c.drawPath(path,p);word(c,"YouTube",83,53,26,Typeface.create("sans-serif-condensed",Typeface.BOLD),Color.WHITE);
                }else if(app.pkg.equals("com.netflix.ninja")){
                    String text="NETFLIX";p.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD));p.setTextSize(35);p.setColor(0xfff01424);c.drawText(text,(196-p.measureText(text))/2,56,p);
                }else if(app.pkg.equals("com.amazon.firebat")){
                    word(c,"prime",35,42,24,bold,0xff00aeef);word(c,"video",98,42,24,bold,Color.WHITE);p.setColor(0xff00aeef);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.5f);c.drawArc(69,37,144,62,18,126,false,p);p.setStyle(Paint.Style.FILL);
                }else if(app.pkg.equals("in.startv.hotstar")){
                    p.setColor(Color.WHITE);path.reset();for(int i=0;i<10;i++){double angle=-Math.PI/2+i*Math.PI/5;float r=i%2==0?20:6;float x=33+(float)Math.cos(angle)*r,y=42+(float)Math.sin(angle)*r;if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}path.close();c.drawPath(path,p);word(c,"JioHotstar",56,50,23,bold,Color.WHITE);
                }else{c.drawBitmap(app.banner,null,rect,p);}
                if(frosted)drawFrost(c);
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(frosted?0x8095b6d6:0x555e7da7);c.drawRoundRect(1,1,195,81,14,14,p);p.setStyle(Paint.Style.FILL);c.restoreToCount(save);
            }
            void drawFrost(Canvas c){
                p.setShader(new LinearGradient(0,0,0,82,new int[]{0x50ffffff,0x0cffffff,0x00000000,0x30000000},new float[]{0,.36f,.65f,1},Shader.TileMode.CLAMP));c.drawRoundRect(rect,14,14,p);p.setShader(null);
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(0x99b9ccdf);c.drawRoundRect(1,1,195,81,14,14,p);p.setStyle(Paint.Style.FILL);
            }
        }
        class Glass extends View {
            final Paint paint=new Paint(3);final RectF rect=new RectF();ValueAnimator motion;boolean pill;
            final LinearGradient sheen=new LinearGradient(0,0,1,1,0x38ffffff,0x04ffffff,Shader.TileMode.CLAMP);final Matrix matrix=new Matrix();
            Glass(){super(Scene.this.getContext());setLayerType(View.LAYER_TYPE_HARDWARE,null);}
            void move(RectF to,boolean isPill,boolean animate){pill=isPill;if(motion!=null)motion.cancel();RectF from=new RectF(rect);if(!animate||rect.isEmpty()){rect.set(to);invalidate();return;}motion=ValueAnimator.ofFloat(0,1);motion.setDuration(310);motion.setInterpolator(new OvershootInterpolator(.6f));motion.addUpdateListener(a->{float f=(float)a.getAnimatedValue();rect.set(from.left+(to.left-from.left)*f,from.top+(to.top-from.top)*f,from.right+(to.right-from.right)*f,from.bottom+(to.bottom-from.bottom)*f);invalidate();});motion.start();}
            @Override protected void onDraw(Canvas c){if(rect.isEmpty())return;float radius=(pill?25:allApps?12:18)*s;paint.setStyle(Paint.Style.FILL);matrix.setScale(rect.width(),rect.height());matrix.postTranslate(rect.left,rect.top);sheen.setLocalMatrix(matrix);paint.setShader(sheen);c.drawRoundRect(rect,radius,radius,paint);paint.setShader(null);paint.setStyle(Paint.Style.STROKE);if(allApps){for(int i=7;i>0;i--){paint.setStrokeWidth((2+i*1.5f)*s);paint.setColor(Color.argb(5+(7-i)*2,200,230,255));c.drawRoundRect(rect,radius,radius,paint);}}paint.setStrokeWidth((allApps?2f:1.4f)*s);paint.setColor(movingApp?0xffb7f3ff:0xfff5faff);paint.setShadowLayer((allApps?12:8)*s,0,0,0xddcceaff);c.drawRoundRect(rect,radius,radius,paint);paint.clearShadowLayer();paint.setStyle(Paint.Style.FILL);}
        }
    }
}
