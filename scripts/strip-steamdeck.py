"""Remove the optional steamdeck4j archive from an upstream Controlify JAR before distribution.

Usage: python scripts/strip-steamdeck.py original-controlify.jar output-base.jar
The normal Gradle build then compiles vendor/controlify-patches and installs those overrides.
"""
import argparse, json, pathlib, zipfile
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('input',type=pathlib.Path)
p.add_argument('output',type=pathlib.Path)
a=p.parse_args()
if a.input.resolve()==a.output.resolve(): p.error('Use a different output path.')
with zipfile.ZipFile(a.input) as original, zipfile.ZipFile(a.output,'w',zipfile.ZIP_DEFLATED) as result:
    for item in original.infolist():
        if item.filename=='META-INF/jarjar/steamdeck4j-1.1.7.jar': continue
        data=original.read(item.filename)
        if item.filename=='META-INF/jarjar/metadata.json':
            metadata=json.loads(data)
            metadata['jars']=[m for m in metadata['jars'] if m['identifier']['artifact']!='steamdeck4j']
            data=json.dumps(metadata,indent=2).encode()
        result.writestr(item,data)
print(a.output)
