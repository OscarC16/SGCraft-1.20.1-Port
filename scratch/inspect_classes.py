import zipfile
import subprocess
import os

jar_path = r"D:\Antigravity\Mod\sgcraft_1_21_11\build\moddev\artifacts\neoforge-21.11.42.jar"
if not os.path.exists(jar_path):
    print("Jar not found!")
    exit(1)

target_classes = [
    "net/neoforged/neoforge/capabilities/Capabilities",
    "net/neoforged/neoforge/capabilities/Capabilities$Energy",
    "net/neoforged/neoforge/capabilities/BlockCapability",
    "net/neoforged/neoforge/transfer/energy/EnergyHandler",
    "net/neoforged/neoforge/energy/IEnergyStorage"
]

with zipfile.ZipFile(jar_path, 'r') as z:
    for tc in target_classes:
        class_file = tc + ".class"
        try:
            data = z.read(class_file)
            temp_name = class_file.replace('/', '_').replace('$', '_')
            with open(temp_name, "wb") as f:
                f.write(data)
            print(f"--- Methods in {tc} ---")
            # Run javap
            res = subprocess.run(["javap", "-p", temp_name], capture_output=True, text=True)
            print(res.stdout)
            os.remove(temp_name)
        except Exception as e:
            print(f"Error reading {tc}: {e}")
