"""Compare real Voxy native mip outputs from two workers against serial references."""
import json,pathlib,runpy
s=runpy.run_path(str(pathlib.Path(__file__).with_name('staged_distance.py')))
result=s['report']('lodMipRegression')
assert result['passed'] and result['outputMatchesSerialBytes'],result
(s['out']/'mip-concurrency.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
print('PASS: two workers, 1000 calls each, eight transparent patterns match serial bytes.',flush=True)
