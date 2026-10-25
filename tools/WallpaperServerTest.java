package dev.clean.tvhome;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
public class WallpaperServerTest {
 static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);System.out.println("PASS "+label);}
 static int send(String url,String method,byte[] body,String origin)throws Exception{
  HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setRequestMethod(method);c.setConnectTimeout(2000);c.setReadTimeout(2000);
  if(origin!=null)c.setRequestProperty("Origin",origin);
  if(body!=null){c.setDoOutput(true);c.setFixedLengthStreamingMode(body.length);c.getOutputStream().write(body);}
  int status=c.getResponseCode();c.disconnect();return status;
 }
 public static void main(String[] args)throws Exception{
  int[] received={0};WallpaperServer s=new WallpaperServer(InetAddress.getByName("127.0.0.1"),bytes->{if(bytes[0]==0)throw new IOException();received[0]++;});s.start();String url=s.url();
  check(send(url,"GET",null,null)==200,"upload page");
  check(send(url+"bad","POST",new byte[]{1},null)==404,"wrong token rejected");
  check(send(url,"POST",new byte[]{1},"http://evil.test")==403,"cross-origin rejected");
  check(send(url,"POST",new byte[]{0},null)==422,"invalid image callback rejected");
  check(send(url,"POST",new byte[]{1,2,3},null)==200&&received[0]==1,"binary upload delivered");
  check(send(url,"POST",new byte[WallpaperServer.MAX_BYTES+1],null)==413,"oversize rejected");
  s.close();try{send(url,"GET",null,null);throw new AssertionError("still open");}catch(IOException expected){System.out.println("PASS endpoint closes");}
 }
}
