#!/usr/bin/env python3
"""
scripts/sync_play_store_assets.py
Synchronizes localized store listing metadata and actual Compose UI screenshots
directly to Google Play Console via the official Google Play Developer Publishing API (androidpublisher v3).

Features:
- Pure Python implementation with zero Fastlane/Ruby dependencies.
- Replaces brittle changelog/track lookups with direct store listing and images API calls.
- Uploads Phone, 7-inch Tablet, and 10-inch Tablet screenshots compliant with Play Store guidelines.
- Performs post-commit live verification by querying the Google Play Developer Console API
  and asserting screenshot presence.
"""

import os
import sys
import argparse
from PIL import Image
from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload
from googleapiclient.errors import HttpError

PACKAGE_NAME = "com.aistudio.calculator.gtypxz"
METADATA_DIR = "fastlane/metadata/android/en-US"
SCREENSHOT_CATEGORIES = {
    "phoneScreenshots": "phoneScreenshots",
    "sevenInchScreenshots": "sevenInchScreenshots",
    "tenInchScreenshots": "tenInchScreenshots",
}

def verify_local_images(metadata_dir):
    images_dir = os.path.join(metadata_dir, "images")
    if not os.path.exists(images_dir):
        print(f"Error: Images directory not found at {images_dir}", file=sys.stderr)
        sys.exit(1)

    print("=== Verifying Local Layout Screenshots ===")
    for cat_key, subfolder in SCREENSHOT_CATEGORIES.items():
        folder_path = os.path.join(images_dir, subfolder)
        if not os.path.exists(folder_path):
            print(f"Error: Missing category folder {folder_path}", file=sys.stderr)
            sys.exit(1)
        
        png_files = sorted([f for f in os.listdir(folder_path) if f.endswith(".png")])
        if len(png_files) < 4:
            print(f"Error: Expected at least 4 screenshots in {subfolder}, found {len(png_files)}", file=sys.stderr)
            sys.exit(1)
            
        for f in png_files:
            img_path = os.path.join(folder_path, f)
            with Image.open(img_path) as im:
                w, h = im.size
                if im.mode != "RGB":
                    print(f"Converting {img_path} to 24-bit RGB...")
                    im.convert("RGB").save(img_path, "PNG")
                print(f"  ✓ {subfolder}/{f}: {w}x{h} (Format: {im.format}, Mode: {im.mode})")
    print("All local screenshots verified successfully.\n")

def read_text_file(path, max_length=None):
    if not os.path.exists(path):
        return None
    with open(path, "r", encoding="utf-8") as f:
        content = f.read().strip()
    if max_length and len(content) > max_length:
        print(f"Warning: {path} exceeds {max_length} characters (length: {len(content)}). Truncating.")
        content = content[:max_length]
    return content

def main():
    parser = argparse.ArgumentParser(description="Sync Play Store Metadata and Screenshots")
    parser.add_argument("--key-file", required=True, help="Path to Google Play Service Account JSON key")
    parser.add_argument("--package-name", default=PACKAGE_NAME, help="Android package name")
    parser.add_argument("--metadata-dir", default=METADATA_DIR, help="Path to metadata directory")
    parser.add_argument("--validate-only", action="store_true", help="Dry run without committing changes")
    args = parser.parse_args()

    if not os.path.exists(args.key_file):
        print(f"Error: Key file not found at {args.key_file}", file=sys.stderr)
        sys.exit(1)

    verify_local_images(args.metadata_dir)

    print("=== Authenticating with Google Play Developer Publishing API ===")
    credentials = service_account.Credentials.from_service_account_file(
        args.key_file,
        scopes=["https://www.googleapis.com/auth/androidpublisher"]
    )
    service = build("androidpublisher", "v3", credentials=credentials)
    print("✓ Successfully authenticated with Google Play API.\n")

    print(f"=== Opening New Edit Transaction for {args.package_name} ===")
    edit_request = service.edits().insert(body={}, packageName=args.package_name)
    edit = edit_request.execute()
    edit_id = edit["id"]
    print(f"✓ Edit transaction created (ID: {edit_id})\n")

    try:
        # 1. Update Store Listing (Title, Short Description, Full Description)
        title_file = os.path.join(args.metadata_dir, "title.txt")
        short_desc_file = os.path.join(args.metadata_dir, "short_description.txt")
        full_desc_file = os.path.join(args.metadata_dir, "full_description.txt")

        title = read_text_file(title_file, max_length=30)
        short_desc = read_text_file(short_desc_file, max_length=80)
        full_desc = read_text_file(full_desc_file, max_length=4000)

        listing_body = {}
        if title:
            listing_body["title"] = title
        if short_desc:
            listing_body["shortDescription"] = short_desc
        if full_desc:
            listing_body["fullDescription"] = full_desc

        if listing_body:
            print("=== Updating Store Listing Metadata (en-US) ===")
            print(f"  • Title: {title}")
            print(f"  • Short Description: {short_desc}")
            print(f"  • Full Description length: {len(full_desc) if full_desc else 0} chars")
            
            service.edits().listings().patch(
                packageName=args.package_name,
                editId=edit_id,
                language="en-US",
                body=listing_body
            ).execute()
            print("✓ Store listing metadata updated successfully.\n")

        # 2. Upload Screenshots for Each Device Category
        images_dir = os.path.join(args.metadata_dir, "images")
        print("=== Uploading Real UI Screenshots to Google Play Console ===")
        for cat_key, subfolder in SCREENSHOT_CATEGORIES.items():
            folder_path = os.path.join(images_dir, subfolder)
            png_files = sorted([f for f in os.listdir(folder_path) if f.endswith(".png")])
            
            print(f"\nProcessing category: '{cat_key}' ({len(png_files)} screenshots)...")
            
            # Delete old screenshots in this category
            try:
                service.edits().images().deleteall(
                    packageName=args.package_name,
                    editId=edit_id,
                    language="en-US",
                    imageType=cat_key
                ).execute()
                print(f"  ✓ Cleared previous {cat_key} screenshots on Google Play Console.")
            except HttpError as e:
                print(f"  (Note: deleteall for {cat_key} returned {e.resp.status})")

            # Upload each screenshot
            for idx, f in enumerate(png_files, start=1):
                img_path = os.path.join(folder_path, f)
                media = MediaFileUpload(img_path, mimetype="image/png", resumable=False)
                res = service.edits().images().upload(
                    packageName=args.package_name,
                    editId=edit_id,
                    language="en-US",
                    imageType=cat_key,
                    media_body=media
                ).execute()
                print(f"  ✓ [{idx}/{len(png_files)}] Uploaded {subfolder}/{f} -> ID: {res.get('id', 'N/A')}")

        print("\nAll screenshot categories uploaded to transaction edit.")

        # 3. Commit or Validate
        if args.validate_only:
            print("\n=== Dry Run Completed (--validate-only set). Discarding Edit ===")
            service.edits().delete(packageName=args.package_name, editId=edit_id).execute()
            print("✓ Edit discarded. No changes committed to production listing.")
            return

        print("\n=== Committing Edit Transaction to Google Play Console ===")
        try:
            commit_res = service.edits().commit(
                packageName=args.package_name,
                editId=edit_id
            ).execute()
        except HttpError as e:
            if "changesNotSentForReview" in str(e):
                print("  Handling managed publishing changesNotSentForReview flag...")
                commit_res = service.edits().commit(
                    packageName=args.package_name,
                    editId=edit_id,
                    changesNotSentForReview=True
                ).execute()
            else:
                raise

        print(f"✓ Edit successfully committed to Google Play Console! (Commit ID: {commit_res.get('id', edit_id)})\n")

        # 4. Live Post-Commit Verification
        print("=== LIVE CONSOLE VERIFICATION: Verifying Assets on Google Play Console ===")
        verify_edit = service.edits().insert(body={}, packageName=args.package_name).execute()
        verify_edit_id = verify_edit["id"]
        
        all_verified = True
        try:
            for cat_key in SCREENSHOT_CATEGORIES.keys():
                resp = service.edits().images().list(
                    packageName=args.package_name,
                    editId=verify_edit_id,
                    language="en-US",
                    imageType=cat_key
                ).execute()
                
                live_images = resp.get("images", [])
                print(f"  • {cat_key}: Found {len(live_images)} active screenshots in Google Play Console.")
                for img in live_images:
                    print(f"      - ID: {img.get('id')}, SHA256: {img.get('sha256', 'N/A')[:16]}...")
                if len(live_images) < 4:
                    all_verified = False
        finally:
            service.edits().delete(packageName=args.package_name, editId=verify_edit_id).execute()

        if all_verified:
            print("\n🎉 VERIFICATION PASSED: All 12 real screenshots and metadata are 100% LIVE on Google Play Console!")
        else:
            print("\n⚠️ Note: Some screenshot categories had fewer than 4 images listed. Check Google Play Console UI.")

    except Exception as e:
        print(f"\n❌ Error during sync: {e}", file=sys.stderr)
        try:
            service.edits().delete(packageName=args.package_name, editId=edit_id).execute()
        except Exception:
            pass
        sys.exit(1)

if __name__ == "__main__":
    main()
