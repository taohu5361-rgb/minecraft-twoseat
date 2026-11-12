"""A fresh standard controller must reach P2 gameplay without controller-only setup screens.

Requires an isolated running TwoSeat world and -Dtwoseat.probe=true.
Unlike the original fixture, this invokes the real controller-connected setup event.
"""
import json, math, os, pathlib, time, uuid

game=pathlib.Path(os.environ['TWOSEAT_TEST_GAME']).resolve()
out=pathlib.Path(os.environ.get('TWOSEAT_TEST_OUTPUT', str(pathlib.Path(__file__).parent)))
out.mkdir(parents=True,exist_ok=True)
def command(op,delay=.3,**kwargs):
    ident='first-use-'+uuid.uuid4().hex[:8]
    target=game/'twoseat-command.json'
    temp=target.with_suffix('.next')
    temp.write_text(json.dumps(dict(id=ident,op=op,**kwargs)),encoding='utf-8')
    temp.replace(target)
    time.sleep(delay)
    return ident
def inspect(label):
    ident=command('inspect')
    for _ in range(100):
        path=game/'twoseat-evidence'/(ident+'.json')
        if path.exists():
            value=json.loads(path.read_text())
            (out/('first-use-'+label+'.json')).write_text(json.dumps(value,indent=2),encoding='utf-8')
            return value
        time.sleep(.1)
    raise RuntimeError('No first-use inspection response')

command('server',command='fill 98 149 98 108 149 108 minecraft:stone')
command('server',command='tp TwoSeatHost 106.5 150 100.5 0 0')
command('server',command='tp TwoSeatGuest 100.5 150 100.5 0 0')
command('controllerFirstConnect',delay=.8)
before=inspect('connected')
print(json.dumps(dict(test='first-use has no mandatory controller UI',screen=before['guestScreen'],passed=before['guestScreen']=='none')),flush=True)
assert before['guestScreen']=='none', 'Fresh gamepad is trapped behind '+before['guestScreen']
command('controllerState',state=dict(axes={'left_stick_up':.8},buttons=[]),delay=.8)
command('controllerState',state={})
after=inspect('movement')
def point(r,k):return tuple(float(x) for x in r[k+'Position'].strip('()').split(','))
assert math.dist(point(before,'guest'),point(after,'guest'))>.4
assert math.dist(point(before,'host'),point(after,'host'))<.02
print('Fresh gamepad reaches P2 movement without setup; P1 remains still.',flush=True)
