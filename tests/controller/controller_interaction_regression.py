"""Extends the basic live-client regression with real server interactions and inventory transfers."""
from controller_regression import *

def server(value, delay=.3): command('server', command=value, delay=delay)
def player(report, name): return next(p for p in report['serverPlayers'] if p['name']==name)

try:
    server('fill 98 149 98 108 149 108 minecraft:stone')
    server('fill 98 150 98 108 154 108 minecraft:air')
    server('tp TwoSeatHost 106.5 150 100.5 0 0')
    server('tp TwoSeatGuest 100.5 150 100.5 0 45')
    server('item replace entity TwoSeatGuest hotbar.0 with minecraft:dirt 64')
    before = inspect('before-controller-place')
    state({'left_trigger':1}, delay=.12)
    state(delay=.5)
    placed = inspect('after-controller-place')
    count = player(placed,'TwoSeatGuest')['selectedItem']
    check('controller places a server-confirmed block', count != '64 dirt' and 'dirt' in placed.get('guestHitState',''),
          dict(selectedItem=count,hit=placed.get('guestHitState'),block=placed.get('guestHitBlock')))
    state({'right_trigger':1},delay=1.4)
    state(delay=.5)
    mined = inspect('after-controller-mine')
    check('controller mines the placed block', 'dirt' not in mined.get('guestHitState',''),mined.get('guestHitState'))
    server('tp TwoSeatHost 100.5 150 102.3 180 0')
    server('tp TwoSeatGuest 100.5 150 100.5 0 0')
    before = inspect('before-controller-combat')
    state({'right_trigger':1},delay=.15)
    state(delay=.5)
    combat = inspect('after-controller-combat')
    check('controller attacks the other real player', player(combat,'TwoSeatHost')['health'] < player(before,'TwoSeatHost')['health']
          and player(combat,'TwoSeatGuest')['health']==player(before,'TwoSeatGuest')['health'],
          dict(hostBefore=player(before,'TwoSeatHost')['health'],hostAfter=player(combat,'TwoSeatHost')['health']))
    state(buttons=['north'])
    state()
    inventory = inspect('inventory-slots')
    slot = next(s for s in inventory['guestSlots'] if s['id']==36)

    # Move using real Controlify analogue virtual-mouse bindings. No direct slot/mouse injection.
    # GUI pixels to window pixels: current test window has GUI scale 2.
    for _ in range(30):
        report = inspect('inventory-cursor-progress')
        dx,dy=slot['x']*2-report['guestMouseX'],slot['y']*2-report['guestMouseY']
        if abs(dx)<10 and abs(dy)<10: break
        axes={}
        if abs(dx)>8: axes['left_stick_right' if dx>0 else 'left_stick_left']=min(.65,max(.28,abs(dx)/130))
        if abs(dy)>8: axes['left_stick_down' if dy>0 else 'left_stick_up']=min(.65,max(.28,abs(dy)/130))
        state(axes,delay=.1)
        state(delay=.2)
    report = inspect('inventory-cursor-on-slot')
    state(buttons=['south'])
    state(delay=.5)
    picked = inspect('inventory-picked')
    check('controller picks up guest inventory stack', 'dirt' in picked['guestCarried'],dict(carried=picked['guestCarried'],cursor=(report['guestMouseX'],report['guestMouseY']),slot=slot))
    state(buttons=['south'])
    state(delay=.5)
    returned=inspect('inventory-returned')
    check('controller places stack back in guest inventory', returned['guestCarried']=='0 air' and any('dirt' in s['item'] for s in returned['guestSlots']))
    command('hostInventory')
    both=inspect('simultaneous-inventories')
    check('both players can have separate inventory screens', both['hostScreen']=='InventoryScreen' and both['guestScreen']=='InventoryScreen')
    command('hostInventory')
    state(buttons=['east'])
    state()
    command('controllerDisconnect')
    before=inspect('disconnect-baseline')
    command('keys', keys=[73],delay=.7)
    command('keys', keys=[],delay=.5)
    fallback=inspect('keyboard-fallback')
    check('disconnect restores P2 keyboard movement', not fallback['controllerActive'] and distance(before,fallback,'guest')>.4 and distance(before,fallback,'host')<.02)
finally:
    if ControllerProbeConnected := inspect('final').get('controllerProbe'): state()
    command('keys',keys=[])
    command('hostForward',down=False)
    (root/'controller-all-regression-results.json').write_text(json.dumps(results,indent=2),encoding='utf-8')
if any(not r['passed'] for r in results): raise SystemExit(1)
