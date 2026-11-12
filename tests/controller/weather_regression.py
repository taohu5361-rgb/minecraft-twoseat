"""Real rain animation regression: both clients must advance their renderer clocks.

Use an isolated running TwoSeat test world with -Dtwoseat.probe=true.
Set TWOSEAT_TEST_GAME and TWOSEAT_TEST_OUTPUT. Expected to fail on 0.2.1.
"""
import json, os, pathlib, time, uuid

game=pathlib.Path(os.environ['TWOSEAT_TEST_GAME']).resolve()
out=pathlib.Path(os.environ['TWOSEAT_TEST_OUTPUT']).resolve()
out.mkdir(parents=True,exist_ok=True)

def replace(temp,target):
    # Windows may briefly deny replacement while the game/indexer reads the command.
    for attempt in range(40):
        try:
            temp.replace(target);return
        except PermissionError:
            if attempt==39:raise
            time.sleep(.05)
def command(op, delay=.25, **kwargs):
    ident='weather-'+uuid.uuid4().hex[:8]
    target=game/'twoseat-command.json'; temp=target.with_suffix('.next')
    temp.write_text(json.dumps(dict(id=ident,op=op,**kwargs)),encoding='utf-8')
    replace(temp,target)
    time.sleep(delay)
    return ident

def report(ident):
    for _ in range(600):
        p=game/'twoseat-evidence'/(ident+'.json')
        if p.exists():return json.loads(p.read_text(encoding='utf-8'))
        time.sleep(.1)
    raise TimeoutError(ident)

command('testOptions')
for c in ('tp TwoSeatHost 100.5 180 100.5 0 0','tp TwoSeatGuest 900.5 180 100.5 0 0'):
    command('server',command=c)
time.sleep(3)
command('server',command='kill @e[tag=TwoSeatSimMob]')
time.sleep(2)  # Living entities retain their UUID during the death animation.
for c in ('gamemode creative @a','gamerule doDaylightCycle false','time set noon',
          'fill 84 149 84 116 149 116 stone','fill 884 149 84 916 149 116 stone',
          'fill 84 150 84 116 155 116 air','fill 884 150 84 916 155 116 air',
          'fillbiome 88 144 88 116 172 116 minecraft:plains',
          'fillbiome 888 144 88 916 172 116 minecraft:plains',
          'tp TwoSeatHost 100.5 150 100.5 0 0','tp TwoSeatGuest 900.5 150 100.5 0 0',
          'summon minecraft:cow 100.5 150 104.5 {UUID:[I;0,0,0,273],PersistenceRequired:1b,Invulnerable:1b,Silent:1b,Tags:["TwoSeatSimMob","TwoSeatSimHost"]}',
          'summon minecraft:cow 900.5 150 104.5 {UUID:[I;0,0,0,546],PersistenceRequired:1b,Invulnerable:1b,Silent:1b,Tags:["TwoSeatSimMob","TwoSeatSimGuest"]}',
          'weather rain 1000000'):
    command('server',command=c)
time.sleep(8)
ident=command('benchmark',seconds=10)
data=report(ident)
(out/'rain-far.json').write_text(json.dumps(data,indent=2),encoding='utf-8')
print(json.dumps({k:v for k,v in data.items() if k!='frameMs'}),flush=True)
assert data['guestRendererTicksAdvanced']>=150, 'P2 renderer animation clock did not advance'
assert data['guestWeatherPhaseRewinds']==0, 'P2 weather animation rewound across game ticks'
assert data['renderDistance']==8 and data['simulationDistance']==8
for seat in ('hostMob','guestMob'):
    assert data[seat]['samples']>100 and data[seat]['ticksAdvanced']>=150, (seat, 'mob simulation stopped')
    assert data[seat]['animationClockRewinds']==0, (seat, 'mob animation clock rewound')
    assert data[seat]['maxInterpolatedFrameStepBlocks']<.5, (seat, 'mob motion jumped between frames')
inspect=report(command('inspect'))
assert inspect['hostParticleState']!=inspect['guestParticleState'], 'Particle pools are shared'
assert inspect['hostParticleWorldMismatches']==0 and inspect['guestParticleWorldMismatches']==0
(out/'weather-inspect.json').write_text(json.dumps(inspect,indent=2),encoding='utf-8')
command('server',command='particle minecraft:campfire_signal_smoke 900.5 151 100.5 0 0 0 0 10 force TwoSeatGuest')
smoke_before=report(command('inspect'))
time.sleep(.5)
smoke_after=report(command('inspect'))
for data in (smoke_before,smoke_after):
    assert data['guestParticleTypes']['CampfireSmokeParticle']['count']>0
    assert 'CampfireSmokeParticle' not in data['hostParticleTypes'], 'P2 particle leaked into P1'
    assert data['hostParticleWorldMismatches']==0 and data['guestParticleWorldMismatches']==0
assert smoke_after['guestParticleTypes']['CampfireSmokeParticle']['maxAge']>smoke_before['guestParticleTypes']['CampfireSmokeParticle']['maxAge']
(out/'particle-aging-before.json').write_text(json.dumps(smoke_before,indent=2),encoding='utf-8')
(out/'particle-aging-after.json').write_text(json.dumps(smoke_after,indent=2),encoding='utf-8')
print('PASS: P2 rain clock advances without reversals at 800-block separation, with 8/8 distances.',flush=True)
