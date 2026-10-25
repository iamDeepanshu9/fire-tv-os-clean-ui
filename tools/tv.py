#!/usr/bin/env python3
"""Run one TV shell command, or inject keys and pull a fresh on-device screenshot."""
import argparse,os,pathlib,subprocess,sys,time,uuid
p=argparse.ArgumentParser();p.add_argument('--screenshot');p.add_argument('command',nargs='*');a=p.parse_args()
from device import target
serial=target()
base=['adb','-s',serial]
def run(args,**kw):return subprocess.run(base+args,check=True,**kw)
if a.screenshot:
 for key in a.command:
  run(['shell','input','keyevent',key]);time.sleep(.8)
 remote='/data/local/tmp/clean-tv-shot-'+uuid.uuid4().hex+'.png'
 result=subprocess.run(base+['shell','screencap','-p',remote],capture_output=True)
 out=pathlib.Path(a.screenshot);out.parent.mkdir(parents=True,exist_ok=True)
 if result.returncode==0:
  run(['pull',remote,str(out)])
  valid=out.exists() and out.stat().st_size>0
 else:valid=False
 subprocess.run(base+['shell','rm','-f',remote],capture_output=True)
 if not valid:
  print('TV returned an empty/unavailable screenshot; hardware HDMI/Live TV/DRM may prevent capture.',file=sys.stderr)
  print(result.stderr.decode(errors='replace').strip(),file=sys.stderr)
  focus=subprocess.check_output(base+['shell','dumpsys activity activities | grep -E "topResumedActivity|mFocusedApp"'],text=True)
  out.with_suffix('.focused-app.txt').write_text(focus);print(focus,file=sys.stderr);sys.exit(1)
elif a.command:run(['shell',' '.join(a.command)])
else:p.error('Supply a shell command or --screenshot PATH')
