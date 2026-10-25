#!/usr/bin/env python3
"""Disable one inspected package at a time and journal verified changes for restore."""
import datetime, json, os, pathlib, re, subprocess, sys
root=pathlib.Path(__file__).resolve().parent.parent
from device import target
serial=target()
protected=re.compile(r'^(android($|\.)|com\.android\.|com\.google\.android\.|com\.mediatek\.|com\.cvte\.)')
for p in sys.argv[1:]:
 if not re.fullmatch(r'[A-Za-z0-9_.]+',p) or protected.match(p): sys.exit('Refusing core/board package: '+p)
 if p in {'com.amazon.tv.launcher','com.amazon.firehomestarter','com.amazon.tv.settings.v2','com.amazon.tv.settings.core','com.amazon.venezia','com.amazon.fireinputdevices','com.amazon.tv.ime','com.amazon.tv.livetv','com.amazon.tv.inputpreference.service'}: sys.exit('Refusing essential/home package: '+p)
 before=subprocess.check_output(['adb','-s',serial,'shell','pm list packages -d'],text=True).splitlines()
 if 'package:'+p in before: print(p, 'already disabled; untouched');continue
 result=subprocess.run(['adb','-s',serial,'shell','pm disable-user --user 0 '+p],capture_output=True,text=True)
 after=subprocess.check_output(['adb','-s',serial,'shell','pm list packages -d'],text=True).splitlines()
 ok='package:'+p in after
 with (root/'change-log.jsonl').open('a') as f:f.write(json.dumps({'time':datetime.datetime.now().isoformat(),'package':p,'disabled_verified':ok,'stdout':result.stdout,'stderr':result.stderr})+'\n')
 if ok:
  with (root/'disabled-by-us.txt').open('a') as f:f.write(p+'\n')
  print(p, 'DISABLED and recorded')
 else:print(p,'UNCHANGED',result.stderr[:160])
