"""Isolated, ordered 32 -> 64 -> 128 chunk LOD smoke/performance test.

Requires TWOSEAT_TEST_GAME and TWOSEAT_TEST_OUTPUT. Never uses the user's playtest.
Run --prepare32 while building Voxy to prepare only the first distance's terrain.
"""
import argparse, json, os, pathlib, time, uuid

game = pathlib.Path(os.environ['TWOSEAT_TEST_GAME']).resolve()
out = pathlib.Path(os.environ['TWOSEAT_TEST_OUTPUT']).resolve()
assert game.name == 'lod-playtest' and game.parent.name == 'tools'
out.mkdir(parents=True, exist_ok=True)
logfile = game/'logs/latest.log'

def log():
    return logfile.read_text(encoding='utf-8', errors='replace') if logfile.exists() else ''

def wait(test, seconds, label):
    deadline = time.monotonic()+seconds
    while time.monotonic()<deadline:
        if test(): return
        time.sleep(.25)
    raise TimeoutError(label)

def command(op, **values):
    ident = 'lod-'+uuid.uuid4().hex[:10]
    dest=game/'twoseat-command.json'; temp=dest.with_suffix('.next')
    temp.write_text(json.dumps(dict(id=ident,op=op,**values)), encoding='utf-8')
    for attempt in range(40):
        try: temp.replace(dest); break
        except PermissionError: time.sleep(.05)
    else: raise RuntimeError('Command replacement locked')
    wait(lambda:f'[TwoSeat probe] {ident} {op}' in log(), 30, f'{op} acknowledgement')
    return ident

def report(op, **values):
    ident=command(op, **values); path=game/'twoseat-evidence'/(ident+'.json')
    wait(path.exists,90,f'{op} report')
    result=json.loads(path.read_text(encoding='utf-8'))
    return result

def server(text): command('server',command=text)

def prepare(chunks):
    marker=out/f'generated-{chunks}.json'
    if marker.exists(): return
    start=len(log())
    for text in ['chunky world minecraft:overworld','chunky shape circle','chunky center 0 0',f'chunky radius {(chunks+8)*16}','chunky start']:
        server(text)
    wait(lambda:'Task finished' in log()[start:],1200,f'{chunks}-chunk pregeneration')
    # Integrated servers do not register the dedicated-server save-all command.
    # The preparation JVM is saved normally before Voxy is installed.
    marker.write_text(json.dumps({'lodChunks':chunks,'generatedRadiusBlocks':(chunks+8)*16,'completionLog':log()[start:]},indent=2),encoding='utf-8')
    print(f'Generated terrain for {chunks} chunks',flush=True)

def main():
    args=argparse.ArgumentParser();args.add_argument('--prepare32',action='store_true')
    args.add_argument('--stage',type=int,choices=(32,64,128));opts=args.parse_args()
    wait(lambda:'logged in with entity id' in log(),120,'host world join')
    server('gamerule doDaylightCycle false');server('time set noon');server('weather clear 1000000')
    server('gamemode spectator @a');server('tp TwoSeatHost -128 160 0 -90 25')
    if opts.prepare32:
        command('join');wait(lambda:'Guest ready:' in log(),60,'guest join')
        server('gamemode spectator @a');server('tp TwoSeatGuest 128 160 0 0 25')
        prepare(32)
        snapshot=report('inspect');(out/'embeddium-two-players.json').write_text(json.dumps(snapshot,indent=2),encoding='utf-8')
        assert len(snapshot['serverPlayers'])==2
        print('PASS: Embeddium supports both real players; only first-stage terrain prepared.',flush=True)
        return
    command('join');time.sleep(3)
    server('gamemode spectator @a');server('tp TwoSeatGuest 128 160 0 0 25')
    summary=out/'staged-summary.json'
    results=json.loads(summary.read_text(encoding='utf-8')) if summary.exists() and opts.stage else []
    stages=(opts.stage,) if opts.stage else (32,64,128)
    for chunks in stages:
        if chunks>32:
            assert any(r['chunks']==chunks//2 and r.get('visualVerified') for r in results), 'Previous distance must pass metrics and visual review first'
        results=[r for r in results if r['chunks']<chunks]
        command('lodDistance',chunks=chunks)
        prepare(chunks)
        ident=command('saveWorld')
        wait(lambda:f'{ident} worldSaved' in log(),120,'world flush before import')
        start=len(log())
        command('clientCommand',command='voxy import current')
        wait(lambda:'Voxy world import finished' in log()[start:],600,f'{chunks}-chunk Voxy import')
        time.sleep(20)
        state=report('lodInspect')
        assert 'error' not in state['host'] and 'error' not in state['guest'], state
        assert state['host']['configuredChunks']==chunks==state['guest']['configuredChunks']
        assert state['host']['renderer'] and state['guest']['renderer']
        assert state['host']['renderer']!=state['guest']['renderer']
        assert state['host']['viewport']!=state['guest']['viewport']
        assert state['host']['viewportWidth']==960==state['guest']['viewportWidth']
        assert state['host']['viewportHeight']==1080==state['guest']['viewportHeight']
        assert state['host']['geometrySections']>0 and state['guest']['geometrySections']>0
        assert state['host']['worldEngine']==state['guest']['worldEngine']
        assert state['host']['dirtyListeners']==2==state['guest']['dirtyListeners']
        assert state['host']['biomeListeners']==2==state['guest']['biomeListeners']
        assert state['host']['topLevelRingRadius']==state['guest']['topLevelRingRadius']
        assert abs(state['host']['cameraX']-state['guest']['cameraX'])>200
        (out/f'{chunks}-lod-state.json').write_text(json.dumps(state,indent=2),encoding='utf-8')
        samples=[]
        for repeat in (1,2):
            sample=report('benchmark',seconds=15)
            assert sample['allFramesFocused'], 'Focus lost during measurement'
            assert sample['renderDistance']==8==sample['simulationDistance'] and not sample['vsync']
            sample['lodChunks']=chunks;sample['repeat']=repeat
            (out/f'{chunks}-frames-{repeat}.json').write_text(json.dumps(sample,indent=2),encoding='utf-8')
            samples.append({k:v for k,v in sample.items() if k!='frameMs'})
        screenshot=command('screenshot')
        results.append(dict(chunks=chunks,state=state,samples=samples,screenshot=screenshot,visualVerified=False))
        (out/'staged-summary.json').write_text(json.dumps(results,indent=2),encoding='utf-8')
        print(f'PASS {chunks} chunks: '+str([round(s['fps'],1) for s in samples]),flush=True)
    print('PASS: requested ordered distance(s).',flush=True)

if __name__=='__main__': main()
