"""P2 leave/rejoin must preserve P1's live shared-cache updates and release old listeners."""
import json,pathlib,runpy,subprocess,time
s=runpy.run_path(str(pathlib.Path(__file__).with_name('staged_distance.py')))
command,server,report,wait,log=(s[k] for k in ('command','server','report','wait','log'))
game,out=s['game'],s['out']
java='E:/AI/Phone-MCP-zh/tools/jdk/jdk-17.0.21.0.2+1/bin/java.exe'

def screenshot(label,single=False):
    ident=command('screenshot');p=game/'screenshots'/('twoseat-'+ident+'.png')
    wait(p.exists,30,'screenshot');time.sleep(.3)
    args=[java,str(pathlib.Path(__file__).with_name('MarkerPixels.java')),str(p)]
    if single:args.append('single')
    args.append('near') # Exclude the separate 1800+ block reach markers above this band.
    result=json.loads(subprocess.check_output(args,text=True))
    (out/(label+'.json')).write_text(json.dumps(dict(screenshot=ident,pixels=result),indent=2),encoding='utf-8')
    return result

def recolor(color):
    server('forceload add 190 -4 198 4');time.sleep(1)
    server('fill 190 130 -4 198 175 4 '+color)
    server('forceload remove 190 -4 198 4')
    ident=command('saveWorld');wait(lambda:ident+' worldSaved' in log(),120,'flush')
    offset=len(log());command('clientCommand',command='voxy import current')
    wait(lambda:'Voxy world import finished' in log()[offset:],180,'import')
    time.sleep(8)

before=report('lodInspect')
before_pixels=screenshot('lifecycle-before')
command('leave');time.sleep(2)
solo=report('lodInspect');players=report('inspect')
assert len(players['serverPlayers'])==1,players
assert solo['host']['dirtyListeners']==1==solo['host']['biomeListeners'],solo
assert solo['host']['worldEngine']==before['host']['worldEngine'],solo
solo_pixels=screenshot('lifecycle-solo-before',True)
recolor('yellow_concrete')
pixels=screenshot('lifecycle-solo-update',True)
assert pixels['host']['yellow']>solo_pixels['host']['yellow']+100 and pixels['host']['magenta']<10,pixels
command('join');time.sleep(3)
server('gamemode spectator @a');server('tp TwoSeatGuest 128 160 0 0 25');time.sleep(5)
again=report('lodInspect')
assert again['host']['dirtyListeners']==2==again['guest']['dirtyListeners'],again
assert again['host']['biomeListeners']==2==again['guest']['biomeListeners'],again
assert again['host']['worldEngine']==again['guest']['worldEngine']==solo['host']['worldEngine'],again
recolor('magenta_concrete')
pixels=screenshot('lifecycle-rejoined-update')
assert pixels['host']['magenta']>100 and abs(pixels['host']['yellow']-before_pixels['host']['yellow'])<10,pixels
assert pixels['guest']['cyan']>100,pixels
(out/'callback-lifecycle-summary.json').write_text(json.dumps(dict(passed=True,solo=solo,rejoined=again,serverPlayersBeforeRejoin=1),indent=2),encoding='utf-8')
print('PASS: P2 leave preserves live P1 LOD; rejoin restores two listeners and both views.',flush=True)
