package dev.clean.tvhome;

import android.app.Activity;
import android.graphics.*;
import android.net.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.*;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.net.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

/** Remote-friendly upload session with explicit on-TV confirmation. */
public class WallpaperActivity extends Activity {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private WallpaperServer server;
    private File selectedPhoto;
    private boolean defaultPreview;
    private String mode="gallery";
    private View gallery;
    private volatile boolean awaitingConfirmation;
    private boolean foreground;
    private int generation;
    private FrameLayout root;
    private ImageView background;
    private LinearLayout panel;
    private float density;
    private final Runnable expire=()->{stopServer();showExpired();};
    @Override public void onCreate(Bundle state){
        super.onCreate(state);density=getResources().getDisplayMetrics().density;
        getWindow().getDecorView().setSystemUiVisibility(5894);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        root=new FrameLayout(this);background=new ImageView(this);background.setScaleType(ImageView.ScaleType.CENTER_CROP);background.setImageBitmap(WallpaperStore.load(this,1));root.addView(background,new FrameLayout.LayoutParams(-1,-1));setContentView(root);
        try{WallpaperStore.migrate(this);}catch(Exception e){Toast.makeText(this,"Could not import all previous wallpapers. Please check storage.",Toast.LENGTH_LONG).show();}
    }
    @Override protected void onResume(){super.onResume();foreground=true;if(mode.equals("upload"))startSession();else if(mode.equals("preview")||mode.equals("delete"))showPreview();else showGallery();}
    @Override protected void onPause(){foreground=false;stopServer();super.onPause();}
    @Override protected void onDestroy(){stopServer();handler.removeCallbacksAndMessages(null);super.onDestroy();}
    private int dp(float value){return Math.round(value*density);}
    private void panel(String title,String explanation){
        if(gallery!=null){root.removeView(gallery);gallery=null;}
        if(panel!=null)root.removeView(panel);
        panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(24),dp(16),dp(24),dp(16));panel.setBackgroundColor(0xf01b2130);
        FrameLayout.LayoutParams params=new FrameLayout.LayoutParams(dp(480),-2,Gravity.RIGHT|Gravity.CENTER_VERTICAL);params.setMargins(dp(24),dp(16),dp(24),dp(16));root.addView(panel,params);
        text(title,24,Color.WHITE);text(explanation,14,0xffcbd4e5);
    }
    private TextView text(String value,int size,int color){TextView view=new TextView(this);view.setText(value);view.setTextSize(size);view.setTextColor(color);view.setPadding(0,0,0,dp(6));panel.addView(view);return view;}
    private Button button(String label,Runnable action){Button button=new Button(this);button.setText(label);button.setAllCaps(false);button.setTextColor(Color.WHITE);button.setBackgroundColor(0xff343f55);button.setOnFocusChangeListener((view,focused)->{button.setBackgroundColor(focused?0xffd9ddff:0xff343f55);button.setTextColor(focused?0xff15192d:Color.WHITE);});panel.addView(button,new LinearLayout.LayoutParams(-1,dp(48)));button.setOnClickListener(v->action.run());return button;}
    private InetAddress localAddress(){
        ConnectivityManager manager=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        for(Network network:manager.getAllNetworks()){
            NetworkCapabilities caps=manager.getNetworkCapabilities(network);LinkProperties props=manager.getLinkProperties(network);
            if(caps==null||props==null||(!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)&&!caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)))continue;
            for(LinkAddress link:props.getLinkAddresses())if(link.getAddress() instanceof Inet4Address&&link.getAddress().isSiteLocalAddress())return link.getAddress();
        }return null;
    }
    private void startSession(){
        stopServer();mode="upload";awaitingConfirmation=false;
        InetAddress address=localAddress();
        if(address==null){panel("Connect to Wi-Fi","Connect this TV and your iPhone or Mac to the same home network, then try again.");button("Try again",this::startSession).requestFocus();button("Back to wallpapers",this::showGallery);return;}
        final int session=generation;
        try{
            server=new WallpaperServer(address,bytes->{
                if(awaitingConfirmation)throw new IllegalStateException("Preview waiting");
                Bitmap bitmap=WallpaperStore.decode(bytes);File saved;
                try{saved=WallpaperStore.add(this,bitmap,"Photo · "+new SimpleDateFormat("d MMM, h:mm a",Locale.getDefault()).format(new Date()));}finally{bitmap.recycle();}
                awaitingConfirmation=true;
                handler.post(()->{if(!foreground||session!=generation)return;selectedPhoto=saved;defaultPreview=false;handler.removeCallbacks(expire);handler.postDelayed(expire,10*60*1000L);showPreview();});
            });
            String url=server.url();
            panel("Upload a wallpaper","Scan with your iPhone camera or open this address in Safari. Use the same Wi-Fi as your TV.");
            BitMatrix matrix=new QRCodeWriter().encode(url,BarcodeFormat.QR_CODE,360,360);
            Bitmap qr=Bitmap.createBitmap(360,360,Bitmap.Config.ARGB_8888);int[] pixels=new int[360*360];for(int y=0;y<360;y++)for(int x=0;x<360;x++)pixels[y*360+x]=matrix.get(x,y)?Color.BLACK:Color.WHITE;qr.setPixels(pixels,0,360,0,0,360,360);
            ImageView code=new ImageView(this);code.setImageBitmap(qr);LinearLayout.LayoutParams qrParams=new LinearLayout.LayoutParams(dp(128),dp(128));qrParams.gravity=Gravity.CENTER_HORIZONTAL;qrParams.bottomMargin=dp(8);panel.addView(code,qrParams);
            TextView link=text(url,13,Color.WHITE);link.setTypeface(Typeface.MONOSPACE);
            text("JPEG or PNG · 20 MB max · link expires in 10 min.",12,0xffb5c1d6);
            button("Back to wallpapers",this::showGallery).requestFocus();
            server.start();handler.postDelayed(expire,10*60*1000L);
        }catch(Exception e){stopServer();panel("Upload unavailable","Could not start photo upload. Please try again.");button("Try again",this::startSession).requestFocus();button("Back to wallpapers",this::showGallery);}
    }
    private void showGallery(){
        stopServer();mode="gallery";selectedPhoto=null;defaultPreview=false;
        background.setImageBitmap(WallpaperStore.load(this,1));
        if(panel!=null){root.removeView(panel);panel=null;}if(gallery!=null)root.removeView(gallery);
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(24),dp(14),dp(24),dp(14));content.setBackgroundColor(0xf0171e2b);gallery=content;
        FrameLayout.LayoutParams outer=new FrameLayout.LayoutParams(-1,-1);outer.setMargins(dp(24),dp(18),dp(24),dp(18));root.addView(content,outer);
        TextView title=new TextView(this);title.setText("Wallpapers");title.setTextSize(25);title.setTextColor(Color.WHITE);content.addView(title);
        TextView subtitle=new TextView(this);subtitle.setText("Choose a saved photo. Uploads stay here until you delete them.");subtitle.setTextSize(14);subtitle.setTextColor(0xffcbd4e5);content.addView(subtitle);
        LinearLayout actions=new LinearLayout(this);actions.setPadding(0,dp(10),0,dp(10));content.addView(actions);
        Button upload=galleryButton("Upload wallpaper",this::startSession);actions.addView(upload,new LinearLayout.LayoutParams(dp(240),dp(46)));
        Button done=galleryButton("Done",this::finish);LinearLayout.LayoutParams doneSize=new LinearLayout.LayoutParams(dp(130),dp(46));doneSize.leftMargin=dp(12);actions.addView(done,doneSize);
        ArrayList<File> photos=new ArrayList<>();photos.add(null);photos.addAll(WallpaperStore.entries(this));
        RecyclerView grid=new RecyclerView(this);grid.setLayoutManager(new GridLayoutManager(this,3));grid.setClipToPadding(false);grid.setPadding(0,dp(4),0,dp(10));content.addView(grid,new LinearLayout.LayoutParams(-1,0,1));
        grid.setAdapter(new RecyclerView.Adapter<PhotoHolder>(){
            public PhotoHolder onCreateViewHolder(ViewGroup parent,int type){return new PhotoHolder();}
            public int getItemCount(){return photos.size();}
            public void onBindViewHolder(PhotoHolder holder,int position){
                File photo=photos.get(position);boolean current=photo==null?!WallpaperStore.file(WallpaperActivity.this).exists():photo.getName().equals(WallpaperStore.activeId(WallpaperActivity.this));
                BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=8;
                holder.thumb.setImageBitmap(photo==null?BitmapFactory.decodeResource(getResources(),R.drawable.cyberpunk,options):BitmapFactory.decodeFile(photo.getPath(),options));
                holder.caption.setText((current?"✓ Current · ":"")+(photo==null?"Default wallpaper":WallpaperStore.title(WallpaperActivity.this,photo)));
                holder.itemView.setOnClickListener(v->{selectedPhoto=photo;defaultPreview=photo==null;showPreview();});
            }
        });
        upload.requestFocus();
    }
    private Button galleryButton(String label,Runnable action){
        Button button=new Button(this);button.setText(label);button.setAllCaps(false);button.setTextColor(Color.WHITE);button.setBackgroundColor(0xff343f55);
        button.setOnFocusChangeListener((view,focused)->{button.setBackgroundColor(focused?0xffd9ddff:0xff343f55);button.setTextColor(focused?0xff15192d:Color.WHITE);});button.setOnClickListener(v->action.run());return button;
    }
    private class PhotoHolder extends RecyclerView.ViewHolder {
        final ImageView thumb;final TextView caption;
        PhotoHolder(){super(new LinearLayout(WallpaperActivity.this));
            LinearLayout card=(LinearLayout)itemView;card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(5),dp(5),dp(5),dp(5));card.setFocusable(true);card.setClickable(true);card.setBackgroundColor(0xff303b50);
            RecyclerView.LayoutParams size=new RecyclerView.LayoutParams(-1,dp(144));size.setMargins(dp(3),dp(3),dp(3),dp(7));card.setLayoutParams(size);
            thumb=new ImageView(WallpaperActivity.this);thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);card.addView(thumb,new LinearLayout.LayoutParams(-1,dp(104)));
            caption=new TextView(WallpaperActivity.this);caption.setSingleLine(true);caption.setEllipsize(android.text.TextUtils.TruncateAt.END);caption.setTextSize(12);caption.setTextColor(Color.WHITE);caption.setPadding(dp(5),dp(5),0,0);card.addView(caption);
            card.setOnFocusChangeListener((v,focused)->{card.setBackgroundColor(focused?0xffd9ddff:0xff303b50);caption.setTextColor(focused?0xff15192d:Color.WHITE);});
        }
    }
    private void showPreview(){
        mode="preview";
        if(defaultPreview)background.setImageResource(R.drawable.cyberpunk);else background.setImageBitmap(BitmapFactory.decodeFile(selectedPhoto.getPath()));
        panel(defaultPreview?"Default wallpaper":"Saved wallpaper","Set this photo as your wallpaper. Your saved photos stay in the gallery until you delete them.");
        button("Set wallpaper",()->{
            try{WallpaperStore.select(this,defaultPreview?null:selectedPhoto);Toast.makeText(this,"Wallpaper updated",Toast.LENGTH_SHORT).show();showGallery();}
            catch(Exception e){Toast.makeText(this,"Could not set wallpaper. Please try again.",Toast.LENGTH_LONG).show();}
        }).requestFocus();
        if(!defaultPreview)button("Delete wallpaper",this::confirmDelete);
        button("Back to wallpapers",this::showGallery);
    }
    private void confirmDelete(){
        stopServer();mode="delete";boolean current=selectedPhoto.getName().equals(WallpaperStore.activeId(this));
        panel("Delete this wallpaper?",current?"This is your current wallpaper. Deleting it will switch to the default wallpaper.":"This photo will be removed from your saved wallpapers.");
        button("Keep wallpaper",this::showPreview).requestFocus();
        button("Delete wallpaper",()->{try{WallpaperStore.delete(this,selectedPhoto);showGallery();}catch(Exception e){Toast.makeText(this,"Could not delete wallpaper",Toast.LENGTH_LONG).show();}});
    }
    @Override public void onBackPressed(){if(mode.equals("gallery"))super.onBackPressed();else if(mode.equals("delete"))showPreview();else showGallery();}
    private void showExpired(){if(!foreground||!mode.equals("upload"))return;panel("Upload link expired","Open a fresh link to send your photo.");button("New upload link",this::startSession).requestFocus();button("Back to wallpapers",this::showGallery);}
    private void stopServer(){generation++;handler.removeCallbacks(expire);if(server!=null){server.close();server=null;}}
}
