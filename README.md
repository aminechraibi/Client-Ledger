# Client Ledger

A modern, offline-first personal ledger and client account manager built natively for Android using Jetpack Compose and Material Design 3. Designed for freelancers, small business owners, and clear-cut accounts management, Client Ledger allows you to visually track and organize outstanding balances with custom transaction trails, status badges, and receipt visualizers.

---

## 🎨 Design Philosophy & UX Highlights

Client Ledger leverages a clean, high-contrast, minimalist aesthetic built around intuitive Material 3 design tokens:

*   **Financial Snapshot Cards**: Visual header blocks displaying active financial health—differentiating **Amounts Owed by Me** (positive balance credit) and **Amounts Owed to Me** (negative balance debit) along with aggregate **Net Custody Balance**.
*   **Intuitive Color Accents**: Balanced color palettes following high-contrast usability guidelines:
    *   💚 **Emerald Accents** (`#065F46` / `#D1FAE5` bg) for incoming funds, credits, or positive balance items.
    *   ❤️ **Rose Accents** (`#9F1239` / `#FFE4E6` bg) for outgoing funds, debits, or negative balances.
*   **Initials Avatar Badges**: Automatic, elegantly rounded client initials badges that scale with responsive window layout formats.
*   **Attachment Integration**: Quickly view image receipts, invoices, or manual payment proof directly linked to transaction line-items.

---

## 🚀 Key Functional Modules

1.  **Financial Dashboard**
    *   Real-time global asset custody calculation.
    *   Segmented status metrics demonstrating active client balances at a glance.
    *   Unified list of recent general ledger actions.

2.  **Client Directory**
    *   Search and manage client list profiles with quick-action click handlers for Phone calls and Email.
    *   Real-time balance labels identifying those settled vs. those with active balances.

3.  **Detailed Client Profiles**
    *   Full individual ledger history of a selected client.
    *   Interactive add/delete functions for specific financial operations.
    *   Support for attaching physical receipts with live preview.

4.  **General Ledger Log**
    *   Aggregated historic view of all client transaction operations.
    *   Formatted timestamps and structured chronological sorting.

---

## 🛠️ Architecture & Stack

*   **Language**: Kotlin
*   **UI Toolkit**: Jetpack Compose (100% Declarative Layout)
*   **Design System**: Material Design 3 (M3)
*   **Local Storage**: Offline-first local data management (designed utilizing Kotlin Coroutines & Flow structures)
*   **Navigation**: Android Jetpack Navigation Compose with type-safe route serializations
*   **Build Engine**: Gradle (Kotlin DSL - `.gradle.kts`) with full compilation validation

---

## 📦 How to Get the APK

This repository is optimized for both local development builds and fully automated release builds through GitHub Actions.

### Method 1: Automatically via GitHub Actions (Recommended)

Every time you commit or push code changes to the key branches (`main` or `master`), an automated CI workflow packages the app and makes it available.

1.  Navigate to the **Actions** tab on your GitHub repository.
2.  Select the **Android APK Auto-Release** workflow in the left sidebar.
3.  Click the latest workflow run (with a green checkmark).
4.  Scroll down to the **Artifacts** section at the bottom of the page.
5.  Download the **`app-ledger-debug`** file, unzip it, and copy the `.apk` package onto your Android device to install!

*(Optional)* You can manually trigger a build by clicking the **Run workflow** dropdown on the Actions tab at any time.

---

### Method 2: Build Locally (Development Console)

If you have JDK 17 installed onto your workstation, you can manually build the APK direct from source:

1.  Open your terminal inside the project root namespace.
2.  Run the following build command:
    ```bash
    gradle assembleDebug --no-daemon
    ```
3.  Once the build task terminates successfully, find your compiled binary package under the output destination:
    `app/build/outputs/apk/debug/app-debug.apk`
