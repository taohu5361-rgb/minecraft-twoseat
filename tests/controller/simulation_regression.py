"""Verify vanilla entity/block-entity ticking around BOTH distant real players, at 8/8.
Run after weather_regression in the same disposable TwoSeat world.
"""
import json, os, pathlib, time, uuid
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
def command(op, delay=.2, **kwargs):
    ident='sim-'+uuid.uuid4().hex[:8];target=game/'twoseat-command.json';temp=target.with_suffix('.next')
    temp.write_text(json.dumps(dict(id=ident,op=op,**kwargs)),encoding='utf-8');replace(temp,target);time.sleep(delay);return ident
def inspect(label):
    ident=command('simulation')
    for _ in range(100):
        p=game/'twoseat-evidence'/(ident+'.json')
        if p.exists():
            data=json.loads(p.read_text(encoding='utf-8'));(out/(label+'.json')).write_text(json.dumps(data,indent=2),encoding='utf-8');return data
        time.sleep(.1)
    raise TimeoutError(ident)
command('server',command='kill @e[tag=TwoSeatSimTick]')
for seat,center in (('host',104),('guest',904)):
    for offset in (0,6,7):
        x=center+offset*16
        command('server',command=f'setblock {x} 150 104 minecraft:furnace{{CookTimeTotal:200s,Items:[{{Slot:0b,id:"minecraft:iron_ore",Count:64b}},{{Slot:1b,id:"minecraft:coal",Count:64b}}]}} replace')
        command('server',command=f'summon minecraft:armor_stand {x+2} 151 104 {{Marker:1b,Invisible:1b,NoGravity:1b,Tags:["TwoSeatSimTick","TwoSeatSim-{seat}-{offset}"]}}')
before=inspect('simulation-before');time.sleep(11);after=inspect('simulation-after')
assert after['viewDistance']==8 and after['simulationDistance']==8
for p1,p2 in zip(before['players'],after['players']):
    for a,b in zip(p1['chunks'],p2['chunks']):
        if a['offsetChunks']>7:continue
        assert b['entityTicking'], (p2['name'],b)
        assert a['blockEntity']['BurnTime']-b['blockEntity']['BurnTime']>=200,(p2['name'],'furnace not ticking',a,b)
        assert any(item['Slot']==2 and item['id']=='minecraft:iron_ingot' for item in b['blockEntity']['Items']), (p2['name'],'furnace did not smelt',b)
for a in before['entities']:
    if 'TwoSeatSimTick' not in a['tags']:continue
    b=next(e for e in after['entities'] if e['uuid']==a['uuid'])
    assert b['ticks']-a['ticks']>=200,(a,b)
for seat,center in (('host',104),('guest',904)):
    for offset in (0,6,7):command('server',command=f'setblock {center+offset*16} 150 104 minecraft:air')
command('server',command='kill @e[tag=TwoSeatSimTick]')
print('PASS: both players independently tick entities and furnaces at chunk offsets 0, 6, 7; render/simulation distances 8/8.',flush=True)
