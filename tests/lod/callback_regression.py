"""Actual world-update -> both LOD meshes -> screenshot regression, beyond 8 near chunks."""
import json, pathlib, runpy, subprocess, time
s=runpy.run_path(str(pathlib.Path(__file__).with_name('staged_distance.py')))
command,server,report,wait,log=(s[k] for k in ('command','server','report','wait','log'))
game,out=s['game'],s['out']
java='E:/AI/Phone-MCP-zh/tools/jdk/jdk-17.0.21.0.2+1/bin/java.exe'

def snapshot(label):
    ident=command('screenshot');path=game/'screenshots'/('twoseat-'+ident+'.png')
    wait(path.exists,30,'native screenshot');time.sleep(.3)
    result=subprocess.check_output([java,str(pathlib.Path(__file__).with_name('MarkerPixels.java')),str(path)],text=True)
    counts=json.loads(result)
    (out/(label+'.json')).write_text(json.dumps(dict(screenshot=ident,pixels=counts),indent=2),encoding='utf-8')
    return counts

wait(lambda:'logged in with entity id' in log(),120,'host join')
command('join');time.sleep(3)
server('gamemode spectator @a')
server('tp TwoSeatHost -128 160 0 -90 25');server('tp TwoSeatGuest 128 160 0 0 25')
command('lodDistance',chunks=32);time.sleep(8)
state=report('lodInspect')
assert state['host']['dirtyListeners']==2==state['guest']['dirtyListeners'],state
assert state['host']['biomeListeners']==2==state['guest']['biomeListeners'],state
initial=snapshot('callbacks-before')
assert initial['host']['magenta']>100 and initial['guest']['cyan']>100,initial
for phase,a,b in [('changed','yellow_concrete','red_concrete'),('restored','magenta_concrete','cyan_concrete')]:
    server('forceload add 124 316 132 324');time.sleep(1)
    server('fill 190 130 -4 198 175 4 '+a);server('fill 124 130 316 132 175 324 '+b)
    server('forceload remove 124 316 132 324')
    ident=command('saveWorld');wait(lambda:ident+' worldSaved' in log(),120,'flush')
    offset=len(log());command('clientCommand',command='voxy import current')
    wait(lambda:'Voxy world import finished' in log()[offset:],120,'import')
    time.sleep(8)
    pixels=snapshot('callbacks-'+phase)
    if phase=='changed':
        assert pixels['host']['yellow']>100 and pixels['host']['magenta']<10,pixels
        assert pixels['guest']['red']>100 and pixels['guest']['cyan']<10,pixels
    else:
        assert pixels['host']['magenta']>100 and pixels['host']['yellow']<10,pixels
        assert pixels['guest']['cyan']>100 and pixels['guest']['red']<10,pixels
    print('PASS: both distant markers '+phase,flush=True)
(out/'callback-regression-summary.json').write_text(json.dumps(dict(passed=True,dirtyListeners=2,biomeListeners=2,distanceBlocks=320,nearDistanceBlocks=128,phases=['changed','restored']),indent=2),encoding='utf-8')
