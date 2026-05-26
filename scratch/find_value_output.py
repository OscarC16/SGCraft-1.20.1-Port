import zipfile
import re

jar_path = r"D:\Antigravity\Mod\sgcraft_1_21_11\build\moddev\artifacts\neoforge-21.11.42.jar"
try:
    with zipfile.ZipFile(jar_path, 'r') as z:
        for info in z.infolist():
            if info.filename.endswith('.class'):
                content = z.read(info.filename)
                if b'ValueOutput' in content or b'ValueInput' in content:
                    # Look for class name
                    class_name = info.filename.replace('.class', '').replace('/', '.')
                    if 'Tag' in class_name or 'Nbt' in class_name or 'Output' in class_name or 'Input' in class_name:
                        print(f"Found: {class_name}")
except Exception as e:
    print(f"Error: {e}")
