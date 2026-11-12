"""Replace an LGPL module in a TwoSeat JAR with an interface-compatible modified build.

Usage: python scripts/replace-embedded.py input.jar output.jar --controlify modified.jar
       python scripts/replace-embedded.py input.jar output.jar --yacl modified.jar
"""
import argparse
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('input', type=Path)
p.add_argument('output', type=Path)
p.add_argument('--controlify', type=Path)
p.add_argument('--yacl', type=Path)
a = p.parse_args()
if a.input.resolve() == a.output.resolve():
    p.error('Use a different output path to preserve the original artifact.')
replacements = {}
for key, path in [('controlify-forgified-2.1.9.jar', a.controlify), ('yet-another-config-lib-3.6.6.jar', a.yacl)]:
    if path:
        with ZipFile(path) as module:
            if module.testzip() is not None or 'META-INF/mods.toml' not in module.namelist():
                p.error(f'{path} is not a valid Forge module JAR')
        replacements['META-INF/jarjar/' + key] = path
if not replacements:
    p.error('Supply --controlify and/or --yacl.')
with ZipFile(a.input) as original:
    missing = replacements.keys() - set(original.namelist())
    if missing:
        p.error('Missing embedded module: ' + ', '.join(missing))
    with ZipFile(a.output, 'w', ZIP_DEFLATED) as output:
        for item in original.infolist():
            output.writestr(item, replacements[item.filename].read_bytes() if item.filename in replacements else original.read(item.filename))
print(a.output)
