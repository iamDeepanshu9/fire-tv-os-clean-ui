#!/usr/bin/env python3
"""Enable/disable only this project's accessibility helper; preserve other services."""
import argparse,json,os,pathlib,subprocess
p=argparse.ArgumentParser();p.add_argument('action',choices=['enable','disable']);a=p.parse_args()
root=pathlib.Path(__file__).resolve().parent.parent
from device import target
serial=target()
def shell(command):return subprocess.check_output(['adb','-s',serial,'shell',command],text=True).strip()
component='dev.clean.tvhome/dev.clean.tvhome.HomeRedirectService'
current=shell('settings get secure enabled_accessibility_services')
flag=shell('settings get secure accessibility_enabled')
services=[] if current in ('','null') else current.split(':')
state=root/'backups'/'home-helper-before.json'
if a.action=='enable':
 state.parent.mkdir(parents=True,exist_ok=True)
 if not state.exists():state.write_text(json.dumps({'enabled_accessibility_services':current,'accessibility_enabled':flag},indent=2)+'\n')
 services=[x for x in services if not x.startswith('dev.clean.tvhome/')]+[component]
 shell('settings put secure enabled_accessibility_services '+':'.join(services))
 shell('settings put secure accessibility_enabled 1')
else:
 services=[x for x in services if not x.startswith('dev.clean.tvhome/')]
 if services:shell('settings put secure enabled_accessibility_services '+':'.join(services))
 else:
  shell('settings delete secure enabled_accessibility_services')
  if state.exists() and json.loads(state.read_text())['accessibility_enabled'] in ('0','null',''):shell('settings put secure accessibility_enabled 0')
print('enabled_accessibility_services:',shell('settings get secure enabled_accessibility_services'))
print('accessibility_enabled:',shell('settings get secure accessibility_enabled'))
