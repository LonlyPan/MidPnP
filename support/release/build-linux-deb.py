"""Build a Debian package without requiring a Linux host or changing user configs."""
from pathlib import Path
import io
import tarfile
import time
import shutil
import gzip

root = Path(__file__).resolve().parents[2]
out = root / 'dist' / 'last-board'
app = out / 'OpenPnP-Photon-Linux'
app.mkdir(parents=True, exist_ok=True)
shutil.copy2(root / 'target/openpnp-gui-0.0.1-alpha-SNAPSHOT.jar', app)
shutil.copytree(root / 'target/lib', app / 'lib', dirs_exist_ok=True)
shutil.copy2(root / 'support/release/openpnp.sh', app)
shutil.copy2(root / 'LICENSE.txt', app)
readme = "Photon last-board confirmation build, based on upstream 5bd404c.\nEach new job run using Photon Feed After Pick asks whether this is the last board.\nYes: suppress advance after each part's last required successful pick.\nNo: advance even after the last pick, preparing the next board.\nJob starts never issue a prime feed. The operator must prepare valid first parts for a new job or after a last-board run.\nPending advance from the previous run is checked before picking; no extra feed is issued.\nRepeating the same job asks only the last-board question again. Pause/resume and stepping keep the current answer.\nCancel Placement or closing the dialog stops before head preparation.\nManual part-library/feeder Feed-and-Pick retains the existing Yes/No/Cancel preparation prompt. Manual picks do not auto-advance.\nManual recycle correction and feeder-state UI refresh are retained.\nExisting machine.xml and settings are not overwritten.\nRequires desktop Java 11+ and Linux x86_64. Launch: sh ./openpnp.sh\nInstall: sudo apt install ./openpnp-photon_2.6.0+photon.20261106.5_amd64.deb\nNot tested on physical hardware or Linux GUI.\n"
(app / 'README.txt').write_text(readme, encoding='utf-8')
now = int(time.time())

def put_bytes(tar, name, data, mode=0o644):
    info = tarfile.TarInfo(name)
    info.size = len(data)
    info.mode = mode
    info.mtime = now
    info.uid = info.gid = 0
    info.uname = info.gname = 'root'
    tar.addfile(info, io.BytesIO(data))

def put_file(tar, source, name, mode=0o644):
    info = tar.gettarinfo(str(source), name)
    info.mode = mode
    info.uid = info.gid = 0
    info.uname = info.gname = 'root'
    with source.open('rb') as stream:
        tar.addfile(info, stream)

data_file = out / 'data.tar.gz'
with tarfile.open(data_file, 'w:gz', compresslevel=3) as tar:
    for file in sorted(app.rglob('*')):
        if file.is_file():
            put_file(tar, file, 'opt/openpnp-photon/' + file.relative_to(app).as_posix(),
                     0o755 if file.suffix == '.sh' else 0o644)
    put_bytes(tar, 'usr/bin/openpnp-photon', b'#!/bin/sh\nexec /opt/openpnp-photon/openpnp.sh "$@"\n', 0o755)
    put_bytes(tar, 'usr/share/applications/openpnp-photon.desktop', b'''[Desktop Entry]
Type=Application
Name=OpenPnP Photon
Comment=OpenPnP with post-pick Photon advance feeding
Exec=/usr/bin/openpnp-photon
Icon=openpnp-photon
Terminal=false
Categories=Development;Engineering;
''')
    put_file(tar, root / 'src/main/resources/icons/OpenPNP-logo.iconset/icon_256x256.png',
             'usr/share/icons/hicolor/256x256/apps/openpnp-photon.png')
    put_file(tar, app / 'LICENSE.txt', 'usr/share/doc/openpnp-photon/copyright')
control = f'''Package: openpnp-photon
Version: 2.6.0+photon.20261106.5
Architecture: amd64
Maintainer: Local OpenPnP Build <local@localhost>
Section: electronics
Priority: optional
Installed-Size: {sum(p.stat().st_size for p in app.rglob('*') if p.is_file()) // 1024}
Depends: default-jre | java11-runtime | java17-runtime | java21-runtime
Recommends: libusb-1.0-0, libv4l-0
Homepage: https://github.com/openpnp/openpnp
Description: OpenPnP with post-pick Photon advance feeding
 Starts feeder advance after pick and checks completion before the next pick.
 Preserves existing user configuration.
'''
control_buffer = io.BytesIO()
with tarfile.open(fileobj=control_buffer, mode='w:gz') as tar:
    put_bytes(tar, 'control', control.encode())

deb = out / 'openpnp-photon_2.6.0+photon.20261106.5_amd64.deb'
with deb.open('wb') as stream:
    stream.write(b'!<arch>\n')
    for name, content in [('debian-binary', b'2.0\n'), ('control.tar.gz', control_buffer.getvalue()),
                          ('data.tar.gz', data_file.read_bytes())]:
        header = f'{name + "/":<16}{now:<12}{0:<6}{0:<6}{"100644":<8}{len(content):<10}`\n'
        assert len(header) == 60
        stream.write(header.encode('ascii'))
        stream.write(content)
        if len(content) % 2:
            stream.write(b'\n')
with tarfile.open(out / 'OpenPnP-Photon-Linux-last-board-x64.tar.gz', 'w:gz', compresslevel=3) as tar:
    for file in sorted(app.rglob('*')):
        if file.is_file():
            put_file(tar, file, 'OpenPnP-Photon-Linux/' + file.relative_to(app).as_posix(),
                     0o755 if file.suffix == '.sh' else 0o644)
print(deb)
print('Debian archive generated; checking members and executable mode.')
with tarfile.open(fileobj=io.BytesIO(control_buffer.getvalue()), mode='r:gz') as tar:
    assert b'Package: openpnp-photon' in tar.extractfile('control').read()
with tarfile.open(data_file, 'r:gz') as tar:
    assert tar.getmember('usr/bin/openpnp-photon').mode == 0o755
    assert tar.getmember('opt/openpnp-photon/openpnp.sh').mode == 0o755
    assert not any('.openpnp' in m.name for m in tar.getmembers())
print('Archive structure and permissions verified.')
