# 📱 WA Number Scanner

Automatically detects unsaved phone numbers from your WhatsApp chat list and lets you save them to contacts in bulk, or export as VCF/TXT file.

---

## ✅ Features

- 🔍 **Auto-scan** — Opens and runs in background while you scroll WhatsApp
- 📊 **Live stats** — Shows saved vs unsaved count in real time
- 💾 **Bulk save** — Enter one base name (e.g. "Old Client") → saves as Old Client 1, Old Client 2, etc.
- 📤 **Export VCF** — Import into any phone or Google Contacts
- 📄 **Export TXT** — Plain text list saved to Downloads/WA_Scanner/
- 🌍 **No country code restriction** — Works with numbers from any country
- 🌑 **Dark mode UI**
- ⚡ **No root required**

---

## 🛠️ HOW TO BUILD THE APK

### Requirements
- Android Studio (Dolphin / Electric Eel / Flamingo or newer)
- JDK 8 or higher (comes with Android Studio)
- Android SDK 29 installed

### Steps

1. **Open Android Studio**
2. Click **"Open an Existing Project"**
3. Select the `WAScannerApp` folder (this folder)
4. Wait for Gradle sync to complete (takes 1–3 minutes first time)
5. Click **Build → Build Bundle(s) / APK(s) → Build APK(s)**
6. APK will be at:
   ```
   WAScannerApp/app/build/outputs/apk/debug/app-debug.apk
   ```
7. Transfer APK to your phone via USB, WhatsApp, or Google Drive
8. On phone: Settings → Install unknown apps → allow your file manager → install APK

---

## 📲 HOW TO USE

### First-time setup (takes 1 minute):

**Step 1: Enable Accessibility Service**
1. Open the app — tap "Open Accessibility Settings"
2. Go to: Installed apps → WA Number Scanner → Toggle ON → Allow
3. Come back to the app — the "Step 1" card disappears ✅

**Step 2: Grant Contacts Permission**
1. Tap "Grant Contacts Permission"
2. Tap Allow ✅

**Step 3: Scan**
1. Open WhatsApp normally
2. Scroll through your chat list slowly
3. Come back to WA Scanner — unsaved numbers appear automatically!

### Save numbers:
1. Tap **"Save All Unsaved to Contacts"**
2. Type a base name: `Old Client`
3. Tap **"Start Saving"**
4. Contacts saved as: Old Client 1, Old Client 2, Old Client 3... ✅

### Export:
- **Export VCF** → saves to Downloads/WA_Scanner/ → share to Gmail, WhatsApp, etc.
- **Export TXT** → plain list of numbers

---

## ⚙️ How it works (Technical)

The app uses Android's **Accessibility Service API** to read text that appears on screen when you open WhatsApp. It ONLY reads chat list item names — NOT message content.

When a chat item's "name" looks like a phone number (not a real contact name), it:
1. Checks your device contacts
2. If not found → marks as **unsaved**
3. Shows in the list and updates count

---

## 🔒 Privacy

- ✅ No internet permission — everything is 100% offline
- ✅ Does NOT read message content
- ✅ Does NOT upload anything
- ✅ All data stays on your device

---

## 📁 Project Structure

```
WAScannerApp/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/wascanner/app/
│   │   ├── MainActivity.java          ← Main UI
│   │   ├── WhatsAppScannerService.java ← Accessibility service (core scanner)
│   │   ├── SaveContactsActivity.java  ← Bulk save UI
│   │   ├── ContactsHelper.java        ← Read/write contacts
│   │   ├── PhoneNumberDetector.java   ← Phone number regex engine
│   │   ├── FileExporter.java          ← VCF & TXT export
│   │   ├── NumberAdapter.java         ← ListView adapter
│   │   └── ScanResult.java            ← Shared scan state
│   └── res/
│       ├── layout/activity_main.xml
│       ├── layout/activity_save_contacts.xml
│       ├── layout/item_number.xml
│       ├── values/strings.xml
│       ├── values/styles.xml
│       ├── values/colors.xml
│       └── xml/accessibility_service_config.xml
```

---

## ❓ Troubleshooting

| Problem | Solution |
|---|---|
| Numbers not detected | Make sure Accessibility is ON and you're scrolling WhatsApp slowly |
| App shows "Step 1" still | Go to phone Settings → Accessibility → check if WA Scanner is enabled |
| Export not working | Grant Storage permission in phone Settings → Apps → WA Scanner → Permissions |
| Saved count shows 0 | Grant Contacts permission |

---

## 📦 APK Info
- Min Android: 5.0 (API 21)
- Target Android: 10 (API 29)
- Size: ~3–4 MB
- Permissions: Contacts (R/W), Accessibility, Storage (for export)
