package dev.clean.tvhome;

import android.content.Context;
import android.graphics.*;
import android.util.AtomicFile;
import java.io.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.*;

final class WallpaperStore {
    static File file(Context context){return new File(context.getFilesDir(),"wallpaper.jpg");}
    static Bitmap load(Context context,int sample){
        BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=sample;
        Bitmap bitmap=BitmapFactory.decodeFile(file(context).getPath(),opts);
        return bitmap!=null?bitmap:BitmapFactory.decodeResource(context.getResources(),R.drawable.cyberpunk,opts);
    }
    static Bitmap decode(byte[] bytes) throws IOException {
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes)),(decoder,info,source)->{
            int w=info.getSize().getWidth(),h=info.getSize().getHeight();
            if(w<1||h<1||(long)w*h>100_000_000)throw new IllegalArgumentException("Image too large");
            float scale=Math.min(1f,2560f/Math.max(w,h));
            decoder.setTargetSize(Math.max(1,Math.round(w*scale)),Math.max(1,Math.round(h*scale)));
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
        });
    }
    static File directory(Context context){File dir=new File(context.getFilesDir(),"wallpapers");if(!dir.exists()&&!dir.mkdirs())throw new IllegalStateException("Cannot create wallpaper library");return dir;}
    static String activeId(Context context){return context.getSharedPreferences("wallpaper_library",0).getString("active","");}
    static java.util.List<File> entries(Context context){
        File[] files=directory(context).listFiles((dir,name)->name.matches("[a-f0-9]{64}\\.jpg"));
        ArrayList<File> result=new ArrayList<>();if(files!=null)Collections.addAll(result,files);
        result.sort((a,b)->Long.compare(b.lastModified(),a.lastModified()));return result;
    }
    static String title(Context context,File photo){return context.getSharedPreferences("wallpaper_library",0).getString(photo.getName(),"Saved photo");}
    static void migrate(Context context) throws Exception {
        android.content.SharedPreferences prefs=context.getSharedPreferences("wallpaper_library",0);
        if(prefs.getBoolean("migrated",false))return;
        if(file(context).exists()){
            byte[] bytes=java.nio.file.Files.readAllBytes(file(context).toPath());
            File saved=addBytes(context,bytes,"Your current wallpaper");
            prefs.edit().putString("active",saved.getName()).commit();
        }
        prefs.edit().putBoolean("migrated",true).commit();
    }
    static File add(Context context,Bitmap bitmap,String title) throws Exception {
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        if(!bitmap.compress(Bitmap.CompressFormat.JPEG,94,output))throw new IOException("Could not save wallpaper");
        return addBytes(context,output.toByteArray(),title);
    }
    private static File addBytes(Context context,byte[] bytes,String title) throws Exception {
        byte[] digest=MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder id=new StringBuilder();
        for(byte value:digest)id.append(String.format(Locale.ROOT,"%02x",value&255));
        File photo=new File(directory(context),id+".jpg");
        if(!photo.exists())write(photo,bytes);
        android.content.SharedPreferences prefs=context.getSharedPreferences("wallpaper_library",0);
        if(!prefs.contains(photo.getName()))prefs.edit().putString(photo.getName(),title).commit();
        return photo;
    }
    static void select(Context context,File photo) throws IOException {
        if(photo==null){new AtomicFile(file(context)).delete();context.getSharedPreferences("wallpaper_library",0).edit().remove("active").commit();return;}
        write(file(context),java.nio.file.Files.readAllBytes(photo.toPath()));
        context.getSharedPreferences("wallpaper_library",0).edit().putString("active",photo.getName()).commit();
    }
    static void delete(Context context,File photo) throws IOException {
        if(photo.getName().equals(activeId(context)))select(context,null);
        if(!photo.delete())throw new IOException("Could not delete wallpaper");
        context.getSharedPreferences("wallpaper_library",0).edit().remove(photo.getName()).commit();
    }
    private static void write(File file,byte[] bytes) throws IOException {
        AtomicFile target=new AtomicFile(file);FileOutputStream output=null;
        try{output=target.startWrite();output.write(bytes);target.finishWrite(output);}
        catch(IOException|RuntimeException e){if(output!=null)target.failWrite(output);throw e;}
    }
}
