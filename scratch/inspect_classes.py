import zipfile
import subprocess
import os

jar_path = r"C:\Users\oscar\.gradle\caches\neoformruntime\artifacts\minecraft_1.21.11_client.jar"
if not os.path.exists(jar_path):
    print("Jar not found!")
    exit(1)

target_classes = [
    "net/minecraft/client/renderer/OrderedSubmitNodeCollector",
    "net/minecraft/client/renderer/SubmitNodeCollector",
    "net/minecraft/client/renderer/block/BlockRenderDispatcher"
]

with zipfile.ZipFile(jar_path, 'r') as z:
    for tc in target_classes:
        class_file = tc + ".class"
        try:
            data = z.read(class_file)
            temp_name = class_file.replace('/', '_')
            with open(temp_name, "wb") as f:
                f.write(data)
            print(f"--- Methods in {tc} ---")
            # Run javap
            res = subprocess.run(["javap", "-p", temp_name], capture_output=True, text=True)
            print(res.stdout)
            os.remove(temp_name)
        except Exception as e:
            print(f"Error reading {tc}: {e}")
