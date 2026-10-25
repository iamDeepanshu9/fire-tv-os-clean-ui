package dev.clean.tvhome;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Short-lived, token-protected local upload endpoint. No outbound requests. */
final class WallpaperServer implements AutoCloseable {
    interface Receiver { void receive(byte[] bytes) throws Exception; }
    static final int MAX_BYTES=20*1024*1024;
    private final ServerSocket server;
    private final String path="/"+UUID.randomUUID().toString().replace("-","");
    private final Receiver receiver;
    private volatile boolean closed;
    private volatile Socket active;
    WallpaperServer(InetAddress address,Receiver receiver) throws IOException {
        this.receiver=receiver;server=new ServerSocket();server.bind(new InetSocketAddress(address,0),4);
    }
    String url(){return "http://"+server.getInetAddress().getHostAddress()+":"+server.getLocalPort()+path;}
    void start(){Thread thread=new Thread(this::serve,"wallpaper-upload");thread.setDaemon(true);thread.start();}
    private void serve(){
        while(!closed){
            try(Socket socket=server.accept()){
                active=socket;if(closed)break;socket.setSoTimeout(15000);handle(socket);
            }catch(IOException ignored){}finally{active=null;}
        }
    }
    private void handle(Socket socket) throws IOException {
        InputStream in=socket.getInputStream();OutputStream out=socket.getOutputStream();
        try{
            int[] budget={8192};String[] request=line(in,budget).split(" ");
            if(request.length!=3){reply(out,400,"Invalid request");return;}
            Map<String,String> headers=new HashMap<>();String header;
            while(!(header=line(in,budget)).isEmpty()){
                int colon=header.indexOf(':');if(colon<1)throw new IOException("Invalid header");
                String name=header.substring(0,colon).toLowerCase(Locale.ROOT);
                if(headers.put(name,header.substring(colon+1).trim())!=null)throw new IOException("Duplicate header");
            }
            if(!request[1].equals(path)){reply(out,404,"This upload link is unavailable. Open Wallpaper on your TV for a new link.");return;}
            String host=server.getInetAddress().getHostAddress()+":"+server.getLocalPort();
            if(!host.equals(headers.get("host"))){reply(out,403,"Invalid host");return;}
            if(request[0].equals("GET")){reply(out,200,page());return;}
            if(!request[0].equals("POST")){reply(out,405,"Use the upload page");return;}
            if(headers.containsKey("origin")&&!headers.get("origin").equals("http://"+host)){reply(out,403,"Open the upload link directly");return;}
            if(headers.containsKey("transfer-encoding")){reply(out,400,"Unsupported request");return;}
            long size;
            try{size=Long.parseLong(headers.getOrDefault("content-length","-1"));}catch(NumberFormatException e){size=-1;}
            if(size<1||size>MAX_BYTES){reply(out,413,"Choose a photo smaller than 20 MB");return;}
            byte[] bytes=new byte[(int)size];int offset=0;
            while(offset<bytes.length){int count=in.read(bytes,offset,bytes.length-offset);if(count<0)throw new EOFException();offset+=count;}
            try{receiver.receive(bytes);reply(out,200,"Photo saved in your TV wallpaper gallery. Select Set wallpaper on your TV to apply it.");}
            catch(Exception e){reply(out,422,"Could not open this photo, or another preview is waiting. Try a JPEG or PNG after closing the TV preview.");}
        }catch(IOException|RuntimeException e){reply(out,400,"Upload interrupted. Please try again.");}
    }
    private static String line(InputStream in,int[] budget) throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();int b;
        while((b=in.read())!=-1){if(--budget[0]<0)throw new IOException("Headers too large");if(b=='\n')return bytes.toString("US-ASCII").replace("\r","");bytes.write(b);}
        throw new EOFException();
    }
    private static void reply(OutputStream out,int status,String message) throws IOException {
        byte[] body=message.getBytes(StandardCharsets.UTF_8);
        out.write(("HTTP/1.1 "+status+" Response\r\nContent-Type: "+(message.startsWith("<!doctype")?"text/html":"text/plain")+"; charset=utf-8\r\nContent-Length: "+body.length+"\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nContent-Security-Policy: default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; connect-src 'self'; img-src blob:; form-action 'none'; frame-ancestors 'none'\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));out.write(body);out.flush();
    }
    private static String page(){return """
        <!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Clean TV · Wallpaper</title><style>
        *{box-sizing:border-box}body{margin:0;background:#101522;color:#f5f6fc;font:17px system-ui;padding:40px 24px}main{max-width:520px;margin:5vh auto}p{color:#bfc9dd;line-height:1.6}h1{font-size:36px;letter-spacing:-1px}label,button{display:block;border:0;border-radius:14px;padding:18px 22px;background:#d9ddff;color:#15192d;font:600 17px system-ui;text-align:center;margin:20px 0;cursor:pointer}input{width:100%;margin:12px 0}button{width:100%}button:disabled{opacity:.5}img{width:100%;max-height:260px;object-fit:contain;border-radius:14px}#status{min-height:54px}small{color:#8f9bb2}</style>
        <main><small>CLEAN TV / WALLPAPER</small><h1>A new view for your TV.</h1><p>Choose a photo from your iPhone or Mac. Every upload stays in your TV wallpaper gallery until you delete it. After sending, choose whether to apply it on your TV.</p>
        <input id="photo" type="file" accept="image/*"><img id="preview" alt="Selected photo preview" hidden><button id="send" disabled>Send to TV</button><p id="status" role="status" aria-live="polite">JPEG or PNG recommended · up to 20 MB.</p><small>Direct transfer over your home network. Keep the TV upload screen open.</small></main>
        <script>const photo=document.getElementById('photo'),send=document.getElementById('send'),status=document.getElementById('status'),preview=document.getElementById('preview');let objectUrl;
        photo.onchange=()=>{const f=photo.files[0];send.disabled=!f||f.size>20971520;if(objectUrl)URL.revokeObjectURL(objectUrl);preview.hidden=!f;if(f){objectUrl=URL.createObjectURL(f);preview.src=objectUrl;}status.textContent=f&&f.size>20971520?'Choose a photo smaller than 20 MB.':'Ready to send. Your TV will ask you to confirm.';};
        send.onclick=async()=>{send.disabled=true;photo.disabled=true;status.textContent='Sending photo…';try{const r=await fetch(location.pathname,{method:'POST',headers:{'Content-Type':'application/octet-stream'},body:photo.files[0]});status.textContent=await r.text();if(!r.ok)send.disabled=false;}catch(e){status.textContent='Connection lost. Keep the TV upload screen open and check both devices are on the same Wi-Fi.';send.disabled=false;}finally{photo.disabled=false;}};
        </script></html>
        """;}
    @Override public void close(){closed=true;try{server.close();}catch(IOException ignored){}Socket socket=active;if(socket!=null)try{socket.close();}catch(IOException ignored){}}
}
