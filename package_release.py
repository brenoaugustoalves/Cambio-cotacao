"""Package local 1.8.0 release artifacts without signing material or build output."""
from pathlib import Path
import hashlib
import shutil
import zipfile
import xml.etree.ElementTree as ET

root=Path(__file__).resolve().parent
version=ET.parse(root/'AndroidManifest.xml').getroot().attrib['{http://schemas.android.com/apk/res/android}versionName']
out=root.parent.parent/'outputs'
out.mkdir(exist_ok=True)
files={
    root/f'build/CambioClaro-{version}.apk':out/f'CambioClaro-{version}.apk',
    root/f'RELEASE-{version}.md':out/f'CambioClaro-{version}-notas.md',
    root/f'TESTE-ARMAZENAMENTO-{version}.md':out/f'CambioClaro-{version}-testes.md',
}
for source,target in files.items():
    if not source.is_file():raise FileNotFoundError(source)
    shutil.copyfile(source,target)

archive=out/f'CambioClaro-{version}-codigo-fonte.zip'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as zipped:
    for path in root.rglob('*'):
        relative=path.relative_to(root)
        excluded_dirs={'.git','.signing','build','__pycache__','android-matrix-logs','android-resilience-logs','stress-results'}
        if not path.is_file() or any(part in excluded_dirs for part in relative.parts) or path.suffix=='.pyc':continue
        if relative.parts[:2]==('tests','evidence') or relative.parts[:2]==('tests','evidence-1.8.0'):continue
        if path.name.startswith('.') and path.name!='.gitignore':continue
        zipped.write(path,relative)
with zipfile.ZipFile(archive) as zipped:
    assert not any(any(marker in name.lower() for marker in ('.git/','.jks','.keystore','password.txt','.env','token','android-matrix-logs/','android-resilience-logs/','stress-results/')) or name.startswith('build/') for name in zipped.namelist())

checksums=out/f'CambioClaro-{version}-SHA256.txt'
with checksums.open('w',encoding='utf-8') as stream:
    for target in list(files.values())+[archive]:stream.write(hashlib.sha256(target.read_bytes()).hexdigest()+'  '+target.name+'\n')
print('RELEASE_PACKAGED',archive)
