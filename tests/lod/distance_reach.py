"""Actual pixels beyond 64 chunks must appear at 128, in both independent views."""
import json,pathlib,runpy,subprocess,time
s=runpy.run_path(str(pathlib.Path(__file__).with_name('staged_distance.py')))
command,server,report,wait,log=(s[k] for k in ('command','server','report','wait','log'))
game,out=s['game'],s['out']
server('forceload add 1792 -16 1816 16');server('forceload add 112 1792 144 1816');time.sleep(3)
server('fill 1796 200 -4 1811 319 11 yellow_concrete')
server('fill 124 200 1796 139 319 1811 yellow_concrete')
server('forceload remove 1792 -16 1816 16');server('forceload remove 112 1792 144 1816')
ident=command('saveWorld');wait(lambda:ident+' worldSaved' in log(),120,'save')
start=len(log());command('clientCommand',command='voxy import current')
wait(lambda:'Voxy world import finished' in log()[start:],180,'import')
results={}
for chunks in (64,128):
    command('lodDistance',chunks=chunks);time.sleep(15)
    state=report('lodInspect');ident=command('screenshot')
    p=game/'screenshots'/('twoseat-'+ident+'.png');wait(p.exists,30,'screenshot');time.sleep(.3)
    pixels=json.loads(subprocess.check_output(['E:/AI/Phone-MCP-zh/tools/jdk/jdk-17.0.21.0.2+1/bin/java.exe',str(pathlib.Path(__file__).with_name('MarkerPixels.java')),str(p)],text=True))
    results[str(chunks)]=dict(state=state,screenshot=ident,pixels=pixels)
    (out/f'reach-{chunks}.json').write_text(json.dumps(results[str(chunks)],indent=2),encoding='utf-8')
for seat in ('host','guest'):
    assert results['64']['pixels'][seat]['yellow']<10,results['64']['pixels']
    assert results['128']['pixels'][seat]['yellow']>20,results['128']['pixels']
(out/'distance-reach-summary.json').write_text(json.dumps(dict(passed=True,hostMarkerCenterDistanceBlocks=1931,guestMarkerCenterDistanceBlocks=1803,results=results),indent=2),encoding='utf-8')
print('PASS: both >64-chunk markers absent at 64, actually visible at 128.',flush=True)
