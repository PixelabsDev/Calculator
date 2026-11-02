#!/usr/bin/env python3
"""
scripts/bundle_debug_symbols.py
Locates native libraries from AGP release build, generates standalone native-debug-symbols.zip,
and injects native debug symbols directly into the release AAB metadata
(BUNDLE-METADATA/com.android.tools.build.debugsymbols/<abi>/*.so).
"""

import os
import sys
import zipfile
import glob

def find_merged_native_libs(project_root):
    search_pattern = os.path.join(
        project_root,
        "app/build/intermediates/merged_native_libs/release/**/out/lib"
    )
    matches = glob.glob(search_pattern, recursive=True)
    for m in matches:
        if os.path.isdir(m) and any(os.path.isdir(os.path.join(m, d)) for d in os.listdir(m)):
            return m
            
    # Fallback: search for any directory with ABI subdirectories containing .so files
    base_merged = os.path.join(project_root, "app/build/intermediates/merged_native_libs/release")
    if os.path.exists(base_merged):
        for root, dirs, files in os.walk(base_merged):
            if any(f.endswith(".so") for f in files):
                # ABI folder is parent of .so, lib dir is parent of ABI
                return os.path.dirname(root)
    return None

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.abspath(os.path.join(script_dir, ".."))
    
    print("=== Processing Android Release Symbols & Deobfuscation Mapping ===")
    
    # 1. Check R8 / Proguard mapping.txt
    mapping_file = os.path.join(project_root, "app/build/outputs/mapping/release/mapping.txt")
    if os.path.exists(mapping_file):
        map_size = os.path.getsize(mapping_file)
        print(f"✓ Found R8 Deobfuscation Mapping File:")
        print(f"    Path: {mapping_file}")
        print(f"    Size: {map_size:,} bytes")
    else:
        print(f"⚠️ Warning: mapping.txt not found at {mapping_file}")

    # 2. Locate native libraries
    lib_dir = find_merged_native_libs(project_root)
    if not lib_dir or not os.path.exists(lib_dir):
        print(f"Notice: No merged native libraries found under {project_root}/app/build/intermediates/merged_native_libs/release")
        return 0

    print(f"✓ Found merged native libraries at: {lib_dir}")
    
    # 3. Create standalone native-debug-symbols.zip
    output_dir = os.path.join(project_root, "app/build/outputs/native-debug-symbols/release")
    os.makedirs(output_dir, exist_ok=True)
    symbols_zip = os.path.join(output_dir, "native-debug-symbols.zip")
    
    so_count = 0
    with zipfile.ZipFile(symbols_zip, "w", compression=zipfile.ZIP_DEFLATED) as zf:
        for root, dirs, files in os.walk(lib_dir):
            for f in sorted(files):
                if f.endswith(".so"):
                    full_path = os.path.join(root, f)
                    rel_path = os.path.relpath(full_path, lib_dir)
                    zf.write(full_path, rel_path)
                    so_count += 1
                    print(f"    + {rel_path} ({os.path.getsize(full_path):,} bytes)")

    zip_size = os.path.getsize(symbols_zip)
    print(f"✓ Generated native debug symbols archive:")
    print(f"    Path: {symbols_zip}")
    print(f"    Size: {zip_size:,} bytes ({so_count} native binaries)")

    # 4. Inject symbols into release AAB if available
    aab_pattern = os.path.join(project_root, "app/build/outputs/bundle/release/*.aab")
    aab_files = glob.glob(aab_pattern)
    for aab in aab_files:
        print(f"✓ Injecting native debug symbols into AAB: {aab}")
        with zipfile.ZipFile(aab, "a", compression=zipfile.ZIP_DEFLATED) as zf:
            existing_names = set(zf.namelist())
            for root, dirs, files in os.walk(lib_dir):
                for f in sorted(files):
                    if f.endswith(".so"):
                        full_path = os.path.join(root, f)
                        rel_path = os.path.relpath(full_path, lib_dir)
                        arc_path = f"BUNDLE-METADATA/com.android.tools.build.debugsymbols/{rel_path}"
                        if arc_path not in existing_names:
                            zf.write(full_path, arc_path)
                            print(f"    + Embedded: {arc_path}")
                        else:
                            print(f"    (Already embedded): {arc_path}")
                            
        print(f"✓ AAB native debug symbols bundling complete: {aab}")

    print("===================================================================")
    return 0

if __name__ == "__main__":
    sys.exit(main())
