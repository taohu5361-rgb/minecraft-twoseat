"""Independent menu regression through Minecraft's keyboard entry and Controlify bindings.
Use only an isolated TwoSeat world, with TWOSEAT_TEST_GAME / TWOSEAT_TEST_OUTPUT.
"""
import json, math, os, pathlib, time, uuid
game=pathlib.Path(os.environ['TWOSEAT_TEST_GAME']).resolve()
out=pathlib.Path(os.environ['TWOSEAT_TEST_OUTPUT']).resolve();out.mkdir(parents=True,exist_ok=True)
shots={}
def command(op, delay=.3, **kwargs):
    ident='menu-'+uuid.uuid4().hex[:8];target=game/'twoseat-command.json';temp=target.with_suffix('.next')
    temp.write_text(json.dumps(dict(id=ident,op=op,**kwargs)),encoding='utf-8')
    for i in range(40):
        try:temp.replace(target);break
        except PermissionError:
            if i==39:raise
            time.sleep(.05)
    time.sleep(delay);return ident
def inspect(label):
    ident=command('inspect')
    for _ in range(100):
        p=game/'twoseat-evidence'/(ident+'.json')
        if p.exists():
            v=json.loads(p.read_text(encoding='utf-8'));(out/(label+'.json')).write_text(json.dumps(v,indent=2),encoding='utf-8');return v
        time.sleep(.1)
    raise TimeoutError(ident)
def state(axes=None,buttons=None,delay=.3):command('controllerState',state={'axes':axes or {},'buttons':buttons or []},delay=delay)
def distance(a,b,seat):
    point=lambda r:tuple(float(v) for v in r[seat+'Position'].strip('()').split(','))
    return math.dist(point(a),point(b))

command('testOptions')
command('hostCloseMenu');command('guestCloseMenu');command('controllerConnect')
for c in ('gamemode survival @a','fill 88 149 88 120 149 120 stone','fill 88 150 88 120 154 120 air',
          'tp TwoSeatHost 106.5 150 100.5 0 0','tp TwoSeatGuest 100.5 150 100.5 0 0'):
    command('server',command=c)
state();command('pressKey',key=256,delay=.6)
before=inspect('host-menu-before')
assert before['hostScreen']!='none' and before['guestScreen']=='none', 'Esc must open only P1 menu'
command('hostForward',down=True)
state({'left_stick_up':.8,'right_stick_right':.5},delay=.8);state()
command('hostForward',down=False)
after=inspect('host-menu-after')
assert not after['paused'], 'P1 menu globally paused both clients'
assert after['worldTime']-before['worldTime']>=10, 'P1 menu froze the integrated server'
assert distance(before,after,'guest')>.4, 'P2 cannot move while P1 menu is open'
assert distance(before,after,'host')<.03, 'Menu owner moved unexpectedly'
assert abs(after['guestYaw']-before['guestYaw'])>.1, 'P2 cannot turn while P1 menu is open'
shots['hostOnly']=command('screenshot')
print('PASS: P1 Esc menu leaves P2 and server running.',flush=True)

command('clickMenuWidget',guest=False,index=3)
child=inspect('host-options-before')
assert child['hostScreen']=='OptionsScreen'
state({'left_stick_up':.8},delay=.8);state()
after_child=inspect('host-options-after')
assert not after_child['paused'] and after_child['worldTime']-child['worldTime']>=10
assert distance(child,after_child,'guest')>.4
command('hostCloseMenu');command('hostCloseMenu');state(buttons=['start']);state()
before=inspect('guest-menu-before')
assert before['guestScreen']!='none' and before['hostScreen']=='none', 'P2 Start must open only P2 menu'
state({'left_stick_up':1,'right_stick_right':1,'right_trigger':1})
command('hostForward',down=True,delay=.8);command('hostForward',down=False,delay=.4);state()
after=inspect('guest-menu-after')
assert distance(before,after,'host')>.4 and distance(before,after,'guest')<.03
assert abs(after['guestYaw']-before['guestYaw'])<.001, 'P2 menu rotated its player'
assert after['worldTime']-before['worldTime']>=10 and not after['paused']
print('PASS: P2 menu leaves P1 and server running.',flush=True)

command('pressKey',key=256,delay=.5)
both=inspect('both-menus')
assert both['hostScreen']!='none' and both['guestScreen']!='none', 'Both menus must coexist'
assert not both['paused']
shots['both']=command('screenshot')
command('pressKey',key=256)
one=inspect('host-resumed')
assert one['hostScreen']=='none' and one['guestScreen']!='none'
state(buttons=['east']);state()
none=inspect('both-resumed')
assert none['hostScreen']=='none' and none['guestScreen']=='none'
print('PASS: independent coexistence and resume.',flush=True)

command('controllerDisconnect');command('pressKey',key=299)
keyboard=inspect('keyboard-p2-menu')
assert keyboard['guestScreen']!='none' and keyboard['hostScreen']=='none', 'F10 must belong to P2'
command('pressKey',key=256)
both=inspect('keyboard-both-menus')
assert both['hostScreen']!='none' and both['guestScreen']!='none', 'P2 keyboard menu swallowed P1 Escape'
command('pressKey',key=256);command('pressKey',key=299)
print('PASS: P2 keyboard menu does not steal P1 Escape.',flush=True)

command('pressKey',key=256);command('pressKey',key=299)
command('clickMenuWidget',guest=False,index=0)
half=inspect('mouse-host-resumed')
assert half['hostScreen']=='none' and half['guestScreen']!='none', 'Right menu stole a left-half click'
command('pressKey',key=256)
command('clickMenuWidget',guest=True,index=0)
half=inspect('mouse-guest-resumed')
assert half['hostScreen']!='none' and half['guestScreen']=='none', 'Right-half click changed P1 menu'
command('pressKey',key=299)
command('clickMenuWidget',guest=True,index=4,delay=.8)
left=inspect('guest-left-only')
assert len(left['serverPlayers'])==1 and left['hostUUID']!='none'
assert left['hostScreen']!='none', 'P2 leave closed P1 menu'
command('hostCloseMenu');command('pressKey',key=256,delay=.6)
solo=inspect('single-player-pause')
assert solo['paused'] and solo['hostScreen']=='PauseScreen', 'Single-player vanilla pause was not restored'
command('hostCloseMenu');command('join',delay=2)
command('pressKey',key=256)
command('clickMenuWidget',guest=False,index=4,delay=2)
ended=inspect('session-ended')
assert ended['hostUUID']=='none' and ended['hostScreen']=='TitleScreen', 'Host end-session did not close world normally'
print('PASS: half-screen click ownership, P2-only leave, vanilla solo pause, and normal host end-session.',flush=True)

(out/'summary.json').write_text(json.dumps({'passed':True,'cases':['P1 menu / P2 movement and look','P1 options / P2 movement','P2 menu / P1 movement and menu owner stopped','two menus and independent resume','Esc and F10 keyboard ownership','half-screen mouse ownership','P2 leave preserves P1','vanilla single-player pause restored','host end saves and closes world']},indent=2),encoding='utf-8')

(out/'screenshots.json').write_text(json.dumps({k:'twoseat-'+v+'.png' for k,v in shots.items()},indent=2),encoding='utf-8')
