"""Run against an isolated, already running TwoSeat test world with -Dtwoseat.probe=true."""
import json, pathlib, time, uuid, math, os

root = pathlib.Path(__file__).resolve().parent
game = pathlib.Path(os.environ['TWOSEAT_TEST_GAME']).resolve()
results = []

def command(op, delay=.25, **kwargs):
    identifier = 'ctl-' + op + '-' + uuid.uuid4().hex[:8]
    target = game / 'twoseat-command.json'
    temp = target.with_suffix('.next')
    temp.write_text(json.dumps(dict(id=identifier, op=op, **kwargs)), encoding='utf-8')
    for attempt in range(40):
        try:
            temp.replace(target);break
        except PermissionError:
            if attempt==39:raise
            time.sleep(.05)
    time.sleep(delay)
    return identifier

def inspect(label):
    identifier = command('inspect')
    path = game / 'twoseat-evidence' / (identifier + '.json')
    for _ in range(100):
        if path.is_file():
            value = json.loads(path.read_text(encoding='utf-8'))
            (root / ('controller-' + label + '.json')).write_text(json.dumps(value, indent=2), encoding='utf-8')
            return value
        time.sleep(.1)
    raise RuntimeError('inspect timeout: ' + identifier)

def state(axes=None, buttons=None, delay=.3):
    command('controllerState', delay=delay, state=dict(axes=axes or {}, buttons=buttons or []))

def point(value, seat):
    return tuple(float(v) for v in value[seat + 'Position'].strip('()').split(','))

def distance(a, b, seat):
    return math.dist(point(a, seat), point(b, seat))

def check(label, condition, details=''):
    result=dict(test=label, passed=bool(condition), details=details)
    results.append(result)
    print(json.dumps(result), flush=True)

try:
    # Keep players separated; the previous combat test intentionally leaves them adjacent.
    # Entity collision can otherwise move the idle host and masquerade as input leakage.
    command('server', command='fill 98 149 98 108 149 108 minecraft:stone')
    command('server', command='tp TwoSeatHost 106.5 150 100.5 0 0')
    command('server', command='tp TwoSeatGuest 100.5 150 100.5 0 0')
    state()
    before = inspect('baseline')
    state({'left_stick_up': .85}, delay=1)
    state()
    after = inspect('guest-movement')
    check('controller moves only guest', distance(before, after, 'guest') > .4 and distance(before, after, 'host') < .02,
          dict(guest=distance(before,after,'guest'),host=distance(before,after,'host')))
    before = after
    command('hostForward', down=True, delay=.7)
    command('hostForward', down=False, delay=.7)
    after = inspect('host-movement')
    check('keyboard moves only host', distance(before, after, 'host') > .4 and distance(before, after, 'guest') < .02,
          dict(host=distance(before,after,'host'),guest=distance(before,after,'guest')))
    state({'right_stick_right': .75}, delay=.8)
    state()
    look = inspect('guest-look')
    check('controller turns only guest', abs(look['guestYaw']-after['guestYaw']) > 2 and look['hostYaw']==after['hostYaw'])
    state({'right_trigger': 1}, delay=.1)
    attack = inspect('attack-key-isolation')
    state()
    check('controller attack key isolated', attack['guestAttackKey'] and not attack['hostAttackKey'])
    state({'left_trigger': 1}, delay=.1)
    use = inspect('use-key-isolation')
    state()
    check('controller use key isolated', use['guestUseKey'] and not use['hostUseKey'])
    state(buttons=['north'])
    state()
    inventory = inspect('guest-inventory')
    check('controller opens only guest inventory', inventory['guestScreen']=='InventoryScreen' and inventory['hostScreen']=='none')
    check('independent mouse handlers', inventory['guestMouse'] != inventory['hostMouse'])
    state({'left_stick_right': .8}, delay=.6)
    state()
    cursor = inspect('guest-cursor')
    check('controller cursor does not move host cursor', cursor['guestMouseX'] != inventory['guestMouseX'] and cursor['hostMouseX']==inventory['hostMouseX'])
    command('hostForward', down=True, delay=.7)
    command('hostForward', down=False, delay=.7)
    while_menu = inspect('host-moves-during-guest-menu')
    check('host moves while guest inventory stays open', distance(cursor,while_menu,'host')>.4 and while_menu['guestScreen']=='InventoryScreen')
    state(buttons=['east'])
    state()
    closed = inspect('guest-inventory-closed')
    check('controller closes guest inventory', closed['guestScreen']=='none' and closed['hostScreen']=='none')
    command('screenshot')
finally:
    state()
    command('hostForward', down=False)
    (root / 'controller-regression-results.json').write_text(json.dumps(results, indent=2), encoding='utf-8')
if any(not r['passed'] for r in results):
    raise SystemExit(1)
