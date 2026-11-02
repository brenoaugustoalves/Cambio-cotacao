"""Package local 1.7.9 release artifacts without signing material or build output."""
from pathlib import Path
import hashlib
import shutil
import zipfile

root=Path(__file__).resolve().parent
out=root.parent.parent/'outputs'
out.mkdir(exist_ok=True)
files={
    root/'build/CambioClaro-1.7.9.apk':out/'CambioClaro-1.7.9.apk',
    root/'RELEASE-1.7.9.md':out/'CambioClaro-1.7.9-notas.md',
    root/'TESTE-MERCADO-1.7.9.md':out/'CambioClaro-1.7.9-testes.md',
}
for source,target in files.items():
    if not source.is_file():raise FileNotFoundError(source)
    shutil.copyfile(source,target)

archive=out/'CambioClaro-1.7.9-codigo-fonte.zip'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as zipped:
    for path in root.rglob('*'):
        relative=path.relative_to(root)
        if not path.is_file() or 'build' in relative.parts or '.signing' in relative.parts or '__pycache__' in relative.parts or path.suffix=='.pyc':continue
        if path.name.startswith('.') and path.name!='.gitignore':continue
        zipped.write(path,relative)
with zipfile.ZipFile(archive) as zipped:
    assert not any('signing' in name.lower() or name.startswith('build/') for name in zipped.namelist())

checksums=out/'CambioClaro-1.7.9-SHA256.txt'
with checksums.open('w',encoding='utf-8') as stream:
    for target in list(files.values())+[archive]:stream.write(hashlib.sha256(target.read_bytes()).hexdigest()+'  '+target.name+'\n')
print('RELEASE_PACKAGED',archive)
