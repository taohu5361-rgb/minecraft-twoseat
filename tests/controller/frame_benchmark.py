"""1920x1080 vanilla-terrain frame-time benchmark for an isolated running client.

Uses open elevated observation platforms and the same P1 camera for single view, nearby split, distant split, and rain.
Two 15-second samples per scenario after warmup. Keep the game foreground and idle.
"""
import json, math, os, pathlib, time, uuid
game=pathlib.Path(os.environ['TWOSEAT_TEST_GAME']).resolve()
out=pathlib.Path(os.environ['TWOSEAT_TEST_OUTPUT']).resolve();out.mkdir(parents=True,exist_ok=True)
def replace(temp,target):
    # Windows may briefly deny replacement while the game/indexer reads the command.
    for attempt in range(40):
        try:
            temp.replace(target);return
        except PermissionError:
            if attempt==39:raise
            time.sleep(.05)
def command(op, delay=.25, **kwargs):
    ident='fps-'+uuid.uuid4().hex[:8];target=game/'twoseat-command.json';temp=target.with_suffix('.next')
    temp.write_text(json.dumps(dict(id=ident,op=op,**kwargs)),encoding='utf-8');replace(temp,target);time.sleep(delay);return ident
def report(ident):
    for _ in range(600):
        p=game/'twoseat-evidence'/(ident+'.json')
        if p.exists():return json.loads(p.read_text(encoding='utf-8'))
        time.sleep(.1)
    raise TimeoutError(ident)
def server(cmd):command('server',command=cmd)
command('testOptions');command('leave');time.sleep(1)
server('gamemode creative @a');server('gamerule doDaylightCycle false');server('time set noon')
def position(name,x):
    server(f'tp {name} {x} 180 48.5 0 35');time.sleep(3)
    ix=int(x)
    server(f'fill {ix-2} 159 46 {ix+2} 159 50 minecraft:air')
    server(f'setblock {ix} 159 48 minecraft:stone')
    server(f'fill {ix-2} 160 46 {ix+2} 164 50 minecraft:air')
    server(f'tp {name} {x} 160 48.5 0 35')
server('weather clear 1000000');position('TwoSeatHost',128.5);time.sleep(20)
results=[]
scenarios=os.environ.get('TWOSEAT_BENCH_SCENARIOS','single_clear,split_near_clear,split_far_clear,split_far_rain').split(',')
joined=False
for scenario in scenarios:
    if scenario!='single_clear' and not joined:
        command('join',delay=3);joined=True
    if scenario!='single_clear':
        x=136.5 if scenario=='split_near_clear' else 928.5
        position('TwoSeatGuest',x)
    if scenario=='split_far_rain':server('weather rain 1000000')
    time.sleep(12)
    for repeat in (1,2):
        before=report(command('inspect'))
        sample=report(command('benchmark',seconds=15))
        after=report(command('inspect'))
        sample['scenario']=scenario;sample['repeat']=repeat
        sample['cameraStart']={k:v for k,v in before.items() if k in ('hostPosition','hostYaw','hostPitch','guestPosition','guestYaw','guestPitch')}
        sample['cameraEnd']={k:v for k,v in after.items() if k in ('hostPosition','hostYaw','hostPitch','guestPosition','guestYaw','guestPitch')}
        def stable(seat):
            if seat+'Position' not in before:return seat+'Position' not in after
            point=lambda r:tuple(float(v) for v in r[seat+'Position'].strip('()').split(','))
            # Ignore subpixel float tails in Controlify's decaying look interpolation.
            return math.dist(point(before),point(after))<.00001 and abs(before[seat+'Yaw']-after[seat+'Yaw'])<.001 and abs(before[seat+'Pitch']-after[seat+'Pitch'])<.001
        sample['stationary']=all(stable(seat) for seat in ('host','guest'))
        (out/(scenario+f'-{repeat}.json')).write_text(json.dumps(sample,indent=2),encoding='utf-8')
        assert sample['allFramesFocused'], 'Game lost foreground focus during '+scenario
        assert sample['stationary'], 'Player/camera moved during '+scenario
        assert sample['width']==1920 and sample['height']==1080
        assert sample['renderDistance']==8 and sample['simulationDistance']==8 and not sample['vsync']
        results.append({k:v for k,v in sample.items() if k!='frameMs'})
        print(json.dumps(results[-1]),flush=True)
all_results=[{k:v for k,v in json.loads(p.read_text(encoding='utf-8')).items() if k!='frameMs'} for p in sorted(out.glob('*clear-*.json'))+sorted(out.glob('*rain-*.json'))]
assert len(all_results)==8 and all(v['allFramesFocused'] and v['stationary'] for v in all_results)
(out/'frame-benchmark-summary.json').write_text(json.dumps(all_results,indent=2),encoding='utf-8')
print('PASS: all eight benchmark samples stayed focused, stationary, at 1920x1080 and 8/8.',flush=True)
