Set-StrictMode -Off
$ErrorActionPreference = "Continue"

$javac  = 'C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot\bin\javac.exe'
$jar    = 'C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot\bin\jar.exe'
$mc       = 'C:\Users\julip\AppData\Roaming\ModrinthApp\profiles\Versuch 1\.fabric\remappedJars\minecraft-1.21.11-0.18.6\client-intermediary.jar'
$loader   = 'C:\Users\julip\AppData\Roaming\ModrinthApp\meta\libraries\net\fabricmc\fabric-loader\0.18.6\fabric-loader-0.18.6.jar'
$cmdApi   = 'C:\Users\julip\AppData\Roaming\ModrinthApp\profiles\Versuch 1\.fabric\processedMods\fabric-command-api-v2-2.4.7+6b42a6003e-a6d7072552922485.jar'
$apiBase  = 'C:\Users\julip\AppData\Roaming\ModrinthApp\profiles\Versuch 1\.fabric\processedMods\fabric-api-base-1.0.5+4ebb5c083e-ccbe8773b96707a4.jar'
$gson     = 'C:\Users\julip\AppData\Roaming\ModrinthApp\meta\libraries\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar'
$brig     = 'C:\Users\julip\AppData\Roaming\ModrinthApp\meta\libraries\com\mojang\brigadier\1.3.10\brigadier-1.3.10.jar'
$lwjglGlfw = 'C:\Users\julip\AppData\Roaming\ModrinthApp\meta\libraries\org\lwjgl\lwjgl-glfw\3.3.3\lwjgl-glfw-3.3.3.jar'

$base   = 'C:\Users\julip\Projekte Code\uptdate mc\zen_gui_mod'
$src    = "$base\src\main\java"
$res    = "$base\src\main\resources"
$out    = "$base\classes"
$jarOut = "$base\zen-gui-1.0.jar"
$deploy = 'C:\Users\julip\AppData\Roaming\ModrinthApp\profiles\Versuch 1\mods\zen-gui-1.0.jar'

# Clean output
Remove-Item -Recurse -Force $out -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $out | Out-Null

# Find java sources
$sources = Get-ChildItem -Path $src -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName
Write-Host "Sources: $($sources -join ', ')"

# Compile
$cp = "$mc;$loader;$cmdApi;$apiBase;$gson;$brig;$lwjglGlfw"
Write-Host "Compiling..."
$result = & $javac -cp $cp -d $out @sources 2>&1
if ($result) { Write-Host "Compile output: $result" }

# Check compiled classes
$classes = Get-ChildItem $out -Recurse -Filter '*.class'
Write-Host "Compiled $($classes.Count) classes"
if ($classes.Count -eq 0) {
    Write-Host "COMPILE FAILED - no classes produced"
    exit 1
}

# Copy resources
Copy-Item "$res\fabric.mod.json" "$out\fabric.mod.json"

# Create jar
if (Test-Path $jarOut) { Remove-Item $jarOut }
Push-Location $out
& $jar cf $jarOut -C . .
Pop-Location

Write-Host "Jar created: $(Test-Path $jarOut)"

# Deploy
Copy-Item $jarOut $deploy -Force
Write-Host "Deployed to mods folder: $deploy"
Write-Host "DONE"
