#!/usr/bin/env python3
"""A refusal confirms ADB is closed; timeouts/privacy blocks are inconclusive."""
import argparse, socket, sys, errno
p=argparse.ArgumentParser();p.add_argument('host');p.add_argument('--port',type=int,default=5555);a=p.parse_args()
try:
 with socket.create_connection((a.host,a.port),timeout=4): pass
except ConnectionRefusedError:
 print(f'CONFIRMED: {a.host}:{a.port} refused the connection.');sys.exit(0)
except OSError as e:
 print(f'INCONCLUSIVE: {e}. Timeout, no route or privacy denial does not prove closure.');sys.exit(2)
print(f'OPEN: {a.host}:{a.port} still accepts connections.');sys.exit(1)
