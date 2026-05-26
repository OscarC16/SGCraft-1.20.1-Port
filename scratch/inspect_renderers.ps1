$jar = "D:\Antigravity\Mod\sgcraft_1_21_11\build\moddev\artifacts\neoforge-21.11.42-sources.jar"
$outDir = "d:\Antigravity\Mod\sgcraft_1_21_11\scratch"
$logFile = "d:\Antigravity\Mod\sgcraft_1_21_11\scratch\source_inspect.txt"
"Starting source inspection..." | Out-File $logFile

Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($jar)

$entries = @(
    "net/minecraft/client/model/geom/builders/UVPair.java"
)

foreach ($entry in $zip.Entries) {
    if ($entries -contains $entry.FullName) {
        "Found entry: $($entry.FullName)" | Out-File $logFile -Append
        $target = Join-Path $outDir ($entry.Name)
        if (Test-Path $target) { Remove-Item $target }
        [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $target, $true)
        "Extracted to $target" | Out-File $logFile -Append
    }
}
$zip.Dispose()
"Done." | Out-File $logFile -Append
