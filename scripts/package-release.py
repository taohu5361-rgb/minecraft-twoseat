"""Package the current TwoSeat release and verify source, embedded modules and licenses."""
import hashlib, io, json, pathlib, shutil, zipfile

root = pathlib.Path(__file__).resolve().parent.parent
properties = dict(line.split('=', 1) for line in (root/'mod/gradle.properties').read_text().splitlines() if '=' in line and not line.startswith('#'))
version = properties['mod_version']
stem = f'twoseat-1.20.1-forge-{version}'
dist = root/'dist'/version
dist.mkdir(parents=True, exist_ok=True)
jar = dist/(stem+'.jar')
shutil.copy2(root/'mod/build/libs'/jar.name, jar)
shutil.copy2(root/'README.md', dist/'README-zh-CN.md')
shutil.copy2(root/'docs/CONTROLIFY.md', dist/'VERIFICATION-zh-CN.md')
shutil.copy2(root/'THIRD-PARTY.md', dist/'THIRD-PARTY.md')
benchmark = root/'docs'/('BENCHMARK-' + version.removesuffix('-alpha') + '.md')
if benchmark.is_file(): shutil.copy2(benchmark, dist/'BENCHMARK-zh-CN.md')

def digest(data): return hashlib.sha256(data).hexdigest()

with zipfile.ZipFile(jar) as archive:
    assert archive.testzip() is None
    for name in ['META-INF/mods.toml','META-INF/MANIFEST.MF','twoseat.mixins.json','twoseat.refmap.json',
                 'META-INF/licenses/LGPL-3.0.txt','META-INF/licenses/GPL-3.0.txt','META-INF/THIRD-PARTY.md']:
        assert name in archive.namelist(), name
    assert version in archive.read('META-INF/mods.toml').decode()
    metadata = json.loads(archive.read('META-INF/jarjar/metadata.json'))
    assert len(metadata['jars'])==2
    embedded = []
    for module in metadata['jars']:
        data = archive.read(module['path'])
        name = pathlib.PurePosixPath(module['path']).name
        with zipfile.ZipFile(io.BytesIO(data)) as modified, zipfile.ZipFile(root/'mod/libs'/name) as original:
            assert modified.testzip() is None
            assert set(modified.namelist()) == set(original.namelist())
            changed = [n for n in original.namelist() if original.read(n)!=modified.read(n)]
            expected = 'controlify.mixins.json' if name.startswith('controlify') else 'yacl.mixins.json'
            allowed = {expected}
            if name.startswith('controlify'):
                allowed.update('dev/isxander/controlify/'+n for n in [
                    'driver/steamdeck/SteamDeckUtil.class', 'driver/steamdeck/SteamDeckDriver.class',
                    'ingame/InGameInputHandler.class', 'ingame/InGameInputHandler$1.class'])
            assert expected in changed and set(changed).issubset(allowed), (name, changed)
            if name.startswith('controlify'):
                assert not any('steamdeck4j' in n.lower() or n.startswith('dev/isxander/deckapi/') for n in modified.namelist())
                deps = json.loads(modified.read('META-INF/jarjar/metadata.json'))
                assert not any(m['identifier']['artifact']=='steamdeck4j' for m in deps['jars'])
            assert not any(n.startswith(('net/minecraft/', 'net/minecraftforge/')) for n in modified.namelist())
        embedded.append(dict(path=module['path'],sha256=digest(data),resourceChanges=changed))
    for name in archive.namelist():
        assert not name.startswith(('net/minecraft/','net/minecraftforge/','com/example/')),name
        if name.endswith('.class'):
            assert int.from_bytes(archive.read(name)[6:8],'big') == 61, name

files = [root/n for n in ['.gitattributes','.gitignore','LICENSE','THIRD-PARTY.md','README.md','build.ps1','run-dev.ps1']]
for folder in ['docs','licenses','scripts','tests','vendor']:
    files.extend(p for p in (root/folder).rglob('*') if p.is_file() and '__pycache__' not in p.parts)
for p in (root/'mod').rglob('*'):
    if not p.is_file(): continue
    rel = p.relative_to(root/'mod')
    if rel.parts[0] in {'.gradle','build','run','run-data','.mixin.out','.idea'}: continue
    if rel.as_posix().startswith(('src/generated/','src/main/java/com/example/')): continue
    if p.suffix in {'.log','.class'}: continue
    files.append(p)
files = sorted(set(files), key=lambda p:p.relative_to(root).as_posix())
source = dist/(stem+'-source.zip')
manifest = []
with zipfile.ZipFile(source,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as archive:
    for p in files:
        name = p.relative_to(root).as_posix()
        data = p.read_bytes()
        info = zipfile.ZipInfo('TwoSeat/'+name)
        info.compress_type = zipfile.ZIP_DEFLATED
        info.external_attr = (0o100755 if name == 'mod/gradlew' else 0o100644) << 16
        archive.writestr(info, data)
        manifest.append(digest(data)+'  '+name)
    archive.writestr('TwoSeat/SOURCE-MANIFEST.sha256','\n'.join(manifest)+'\n')
with zipfile.ZipFile(source) as archive:
    assert archive.testzip() is None
    for p in files:
        assert archive.read('TwoSeat/'+p.relative_to(root).as_posix())==p.read_bytes()
    assert not any('/tools/' in n or '/references/' in n or '/.git/' in n for n in archive.namelist())
for p in (root/'vendor').rglob('*'):
    if p.suffix in {'.zip','.jar'}:
        with zipfile.ZipFile(p) as archive: assert archive.testzip() is None,p
report = dict(version=version,jarBytes=jar.stat().st_size,jarSHA256=digest(jar.read_bytes()),
              sourceFiles=len(files),sourceBytes=source.stat().st_size,sourceSHA256=digest(source.read_bytes()),embedded=embedded)
(dist/'PACKAGE-VERIFICATION.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
checks = [digest(p.read_bytes())+'  '+p.name for p in sorted(dist.iterdir()) if p.is_file() and p.name!='SHA256SUMS.txt']
(dist/'SHA256SUMS.txt').write_text('\n'.join(checks)+'\n',encoding='utf-8')
print(json.dumps(report,indent=2))
