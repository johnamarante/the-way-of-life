# Creates the Google Play upload key for The Way of Life, and the keystore.properties file the release
# build reads. Both stay on this computer (they are kept out of git). Run it once:
#
#     powershell -ExecutionPolicy Bypass -File android\tools\create-upload-key.ps1
#
# You choose the password at the prompt; it is never shown on screen or passed on a command line.
# Keep a backup of upload-key.jks and its password somewhere safe: every future update has to be signed
# with this key. If it is ever lost, Google Play support can reset the upload key.
$ErrorActionPreference = "Stop"

$android = Split-Path $PSScriptRoot -Parent
$store = Join-Path $android "upload-key.jks"
$props = Join-Path $android "keystore.properties"
if (Test-Path $store) { throw "$store already exists. Not overwriting an existing key." }

$keytool = Join-Path $env:JAVA_HOME "bin\keytool.exe"
if (-not (Test-Path $keytool)) { $keytool = (Get-Command keytool).Source }

function Read-Secret([string]$prompt) {
    $secure = Read-Host $prompt -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try { [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr) } finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }
}

$password = Read-Secret "Choose a password for the upload key (at least 6 characters)"
if ($password.Length -lt 6) { throw "The password must be at least 6 characters." }
if ($password -ne (Read-Secret "Type the same password again")) { throw "The two passwords did not match. Nothing was created." }

# keytool reads the password from an environment variable that only this process can see.
$env:WOL_UPLOAD_KEY_PASSWORD = $password
try {
    & $keytool -genkeypair -keystore $store -alias upload -keyalg RSA -keysize 2048 -validity 10000 `
        -storepass:env WOL_UPLOAD_KEY_PASSWORD -keypass:env WOL_UPLOAD_KEY_PASSWORD -dname "CN=The Way of Life"
    if ($LASTEXITCODE -ne 0) { throw "keytool failed." }
} finally {
    Remove-Item Env:WOL_UPLOAD_KEY_PASSWORD -ErrorAction SilentlyContinue
}

# Java properties files treat a backslash as an escape character.
$escaped = $password.Replace("\", "\\")
@(
    "storeFile=upload-key.jks"
    "keyAlias=upload"
    "storePassword=$escaped"
    "keyPassword=$escaped"
) | Set-Content -Path $props -Encoding ascii

Write-Host ""
Write-Host "Created $store and $props."
Write-Host "Back up upload-key.jks and its password somewhere safe."
