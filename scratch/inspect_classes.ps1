$jar = "D:\Antigravity\Mod\sgcraft_1_21_11\build\moddev\artifacts\neoforge-21.11.42.jar"
$outDir = "d:\Antigravity\Mod\sgcraft_1_21_11\scratch"
if (-not (Test-Path $outDir)) {
    New-Item -ItemType Directory -Path $outDir -Force | Out-Null
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($jar)

$entries = @(
    "net/neoforged/neoforge/common/extensions/IBlockExtension.class"
)

foreach ($entry in $zip.Entries) {
    if ($entries -contains $entry.FullName) {
        $target = Join-Path $outDir ($entry.Name)
        [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $target, $true)
        Write-Host "--- Methods in $($entry.FullName) ---"
        & "D:\Aplicaciones\JDK 21\bin\javap.exe" -p $target > scratch/iblockextension_methods.txt
        Write-Host "Written to scratch/iblockextension_methods.txt"
        Remove-Item $target
    }
}
$zip.Dispose()
