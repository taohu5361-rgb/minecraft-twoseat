"""Black-box personal progress/save regression, using the real integrated server.

Set TWOSEAT_TEST_GAME and TWOSEAT_TEST_OUTPUT. Run with 'seed', save/quit the
whole JVM, relaunch the SAME world, join P2, then run with 'verify'. Never use
this destructive fixture in a user's world. The probe only accepts TwoSeat* worlds.
"""
import json, os, pathlib, sys, time, uuid

game = pathlib.Path(os.environ['TWOSEAT_TEST_GAME']).resolve()
out = pathlib.Path(os.environ['TWOSEAT_TEST_OUTPUT']).resolve()
out.mkdir(parents=True, exist_ok=True)

def command(op, delay=.25, **kwargs):
    ident = 'progress-' + uuid.uuid4().hex[:8]
    target = game/'twoseat-command.json'
    temp = target.with_suffix('.next')
    temp.write_text(json.dumps(dict(id=ident, op=op, **kwargs)), encoding='utf-8')
    temp.replace(target)
    time.sleep(delay)
    return ident

def inspect(label):
    ident = command('inspect')
    for _ in range(100):
        source = game/'twoseat-evidence'/(ident+'.json')
        if source.is_file():
            data = json.loads(source.read_text())
            (out/(label+'.json')).write_text(json.dumps(data, indent=2), encoding='utf-8')
            return data
        time.sleep(.1)
    raise RuntimeError('Inspection timed out')

def check_isolation(data):
    assert data['hostUUID'] != data['guestUUID']
    for field in ('Toasts', 'Recipes', 'Stats', 'Advancements'):
        assert data['host'+field] != data['guest'+field], 'Shared client '+field
    players = {p['name']:p for p in data['serverPlayers']}
    for name, level, item, count, ender, n, own, other in (
        ('TwoSeatHost', 7, 'minecraft:diamond', 3, 'minecraft:gold_ingot', 11,
         'minecraft:story/mine_stone', 'minecraft:story/upgrade_tools'),
        ('TwoSeatGuest', 19, 'minecraft:emerald', 5, 'minecraft:iron_ingot', 13,
         'minecraft:story/upgrade_tools', 'minecraft:story/mine_stone')):
        p = players[name]
        nbt = p['playerData']
        assert nbt['XpLevel'] == level, (name, 'XP', nbt['XpLevel'])
        assert any(x['id']==item and x['Count']==count for x in nbt['Inventory']), (name, 'inventory')
        assert any(x['id']==ender and x['Count']==n for x in nbt['EnderItems']), (name, 'ender chest')
        assert p['advancements'][own] and not p['advancements'][other], (name, 'advancements')
    assert data['hostXP'] == 7 and data['guestXP'] == 19, 'Client XP'
    assert 'minecraft:beacon' in data['hostKnownRecipes'] and 'minecraft:conduit' not in data['hostKnownRecipes']
    assert 'minecraft:conduit' in data['guestKnownRecipes'] and 'minecraft:beacon' not in data['guestKnownRecipes']
    return players

def persisted_fields(p):
    nbt = p['playerData']
    return dict(uuid=p['uuid'], jumpStat=p['jumpStat'], advancements=p['advancements'],
                data={k:nbt.get(k) for k in ('Inventory', 'EnderItems', 'XpLevel', 'XpTotal',
                    'XpP', 'Pos', 'Rotation', 'Dimension', 'SpawnX', 'SpawnY', 'SpawnZ', 'SpawnDimension', 'recipeBook')})

mode = sys.argv[1]
if mode == 'seed':
    for cmd in ('gamemode survival @a', 'time set day', 'weather clear',
                'advancement revoke @a everything', 'clear @a',
                'tp TwoSeatHost 106.5 150 100.5 0 0', 'tp TwoSeatGuest 100.5 150 100.5 90 0',
                'spawnpoint TwoSeatHost 106 150 100', 'spawnpoint TwoSeatGuest 100 150 100',
                'experience set TwoSeatHost 7 levels', 'experience set TwoSeatGuest 19 levels',
                'experience set @a 0 points',
                'give TwoSeatHost minecraft:diamond 3', 'give TwoSeatGuest minecraft:emerald 5',
                'item replace entity TwoSeatHost armor.head with minecraft:iron_helmet',
                'item replace entity TwoSeatGuest armor.head with minecraft:diamond_helmet',
                'item replace entity TwoSeatHost enderchest.0 with minecraft:gold_ingot 11',
                'item replace entity TwoSeatGuest enderchest.0 with minecraft:iron_ingot 13',
                'recipe take @a *', 'recipe give TwoSeatHost minecraft:beacon',
                'recipe give TwoSeatGuest minecraft:conduit'):
        command('server', command=cmd)
    command('clearToasts')
    command('server', command='advancement grant TwoSeatHost only minecraft:story/mine_stone')
    host = inspect('host-only-advancement')
    assert host['hostAdvancementToast'] and not host['guestAdvancementToast'], 'P1 advancement toast leaked to P2'
    command('screenshot')
    command('clearToasts')
    command('server', command='advancement grant TwoSeatGuest only minecraft:story/upgrade_tools')
    guest = inspect('guest-only-advancement')
    assert guest['guestAdvancementToast'] and not guest['hostAdvancementToast'], 'P2 advancement toast leaked to P1'
    command('clearToasts')
    players = check_isolation(inspect('before-save'))
    baseline = {name:persisted_fields(p) for name,p in players.items()}
    (out/'expected-personal-progress.json').write_text(json.dumps(baseline, indent=2), encoding='utf-8')
    print('PASS: independent client managers, bidirectional advancement toasts, XP, items, ender chests, recipes and advancement ownership.', flush=True)
elif mode == 'verify':
    players = check_isolation(inspect('after-full-jvm-restart'))
    expected = json.loads((out/'expected-personal-progress.json').read_text())
    actual = {name:persisted_fields(p) for name,p in players.items()}
    assert actual == expected, 'Personal progress changed after full JVM restart; compare saved snapshots'
    world = game/'saves/TwoSeat-Probe'
    for p in players.values():
        for folder, suffix in (('playerdata','.dat'), ('advancements','.json'), ('stats','.json')):
            assert (world/folder/(p['uuid']+suffix)).is_file(), (folder,p['uuid'])
    print('PASS: both UUIDs independently retain inventory, armor, ender chest, XP, position, rotation, spawn point, recipes, advancements and jump statistics across full JVM restart.', flush=True)
else:
    raise ValueError(mode)
