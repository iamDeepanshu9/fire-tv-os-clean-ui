package dev.clean.tvhome;
import android.app.Activity;
import android.media.tv.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.graphics.Color;
/** Displays a passthrough source through Android's TvView/TvInputManager session. */
public class InputActivity extends Activity {
    TvView tv; TextView status;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);getWindow().getDecorView().setSystemUiVisibility(5894);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);
        tv=new TvView(this);root.addView(tv,new FrameLayout.LayoutParams(-1,-1));
        status=new TextView(this);status.setText("Connecting to "+getIntent().getStringExtra("label")+"…");status.setTextColor(Color.WHITE);status.setTextSize(24);status.setGravity(Gravity.CENTER);root.addView(status,new FrameLayout.LayoutParams(-1,-1));setContentView(root);
        tv.setCallback(new TvView.TvInputCallback(){
            @Override public void onVideoAvailable(String id){status.setVisibility(View.GONE);}
            @Override public void onVideoUnavailable(String id,int reason){status.setText("No video signal · "+getIntent().getStringExtra("label"));}
            @Override public void onConnectionFailed(String id){status.setText("This source requires the TV’s own input menu.");}
            @Override public void onDisconnected(String id){status.setText("Source disconnected");}
        });
        String id=getIntent().getStringExtra("input_id");
        try{tv.setStreamVolume(1f);tv.tune(id,TvContract.buildChannelUriForPassthroughInput(id));}catch(Exception e){status.setText("Use the remote Input button for this source.");android.util.Log.e("CleanTV","Input tune failed",e);}
    }
    @Override protected void onStop(){super.onStop();tv.reset();finish();}
}
