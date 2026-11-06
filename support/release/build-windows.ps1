$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
Push-Location $root
try {
    & mvn -s support/release/maven-settings.xml package
    if ($LASTEXITCODE) { throw 'Maven build failed' }
    $inputDir = Join-Path $root 'target/package-input'
    New-Item -ItemType Directory -Path $inputDir -Force | Out-Null
    Copy-Item target/openpnp-gui-0.0.1-alpha-SNAPSHOT.jar $inputDir -Force
    Copy-Item target/lib $inputDir -Recurse -Force
    & jpackage --type app-image --name OpenPnP-Photon --input $inputDir --main-jar openpnp-gui-0.0.1-alpha-SNAPSHOT.jar --main-class org.openpnp.Main --dest dist --java-options '--add-opens=java.base/java.lang=ALL-UNNAMED' --java-options '--add-opens=java.desktop/java.awt=ALL-UNNAMED' --java-options '--add-opens=java.desktop/java.awt.color=ALL-UNNAMED'
    if ($LASTEXITCODE) { throw 'jpackage failed' }
} finally { Pop-Location }
