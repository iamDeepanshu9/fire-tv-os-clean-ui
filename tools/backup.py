#!/usr/bin/env python3
import datetime,json,os,pathlib,subprocess
from device import target
serial=target()
root=pathlib.Path(__file__).resolve().parent.parent/'backups'/datetime.datetime.now().strftime('%Y%m%d-%H%M%S');root.mkdir(parents=True)
commands={'packages.txt':'pm list packages -f','disabled-packages.txt':'pm list packages -d','getprop.txt':'getprop','settings-system.txt':'settings list system','settings-secure.txt':'settings list secure','settings-global.txt':'settings list global'}
for name,command in commands.items():
 r=subprocess.run(['adb','-s',serial,'shell',command],check=True,capture_output=True);(root/name).write_bytes(r.stdout)
(root/'manifest.json').write_text(json.dumps({'target':serial,'commands':commands},indent=2))
print(root)
