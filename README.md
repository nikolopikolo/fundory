# Fundory ₾

> **Modern Native Android Savings, Purchase Planning & Currency Tracking Application**
> Built with Kotlin, Jetpack Compose, Material 3, Room, DataStore, Retrofit, Kotlin Coroutines & Flow, Hilt, and WorkManager.

Fundory is a complete native Android application designed to help users track personal savings goals, manage wishlist items across multiple foreign currencies, calculate import shipping and Georgian customs VAT, and visualize historical and projected savings through interactive Compose Canvas charts.

Fundory adheres to a strict **privacy-first, local-storage architecture**: all savings balances, goals, wishlist items, and transactions are stored locally on-device. No cloud accounts, subscriptions, third-party analytics, or advertising.

---

## 📸 Key Features

### 1. Home Dashboard
- **Savings Overview**: Displays total saved across all goals, combined target costs, remaining funds needed, overall percentage completion, and active goal counter.
- **Visual Goal Card**: Prominent featured goal card showing current progress, target completion estimate, and smooth animated progress indicators.
- **Quick Actions**: Prominent **+ Add Money** and **− Withdraw Money** bottom sheets with instant atomic balance recalculation and note logging.
- **Multi-Goal Switching**: Quickly switch active goals or jump to individual goal views without double-counting balances.

### 2. Multi-Currency Tracking & Live Conversion
- **Native Georgian Lari (`GEL` / `₾`) Base**: All foreign purchases and targets are converted into GEL for unified budgeting.
- **Supported Currencies**: GEL (`₾`), USD (`$`), EUR (`€`), CNY (`¥`), GBP (`£`), JPY (`¥`), and TRY (`₺`).
- **Dual API Synchronization**: Live rates from `open.er-api.com` with automatic fallback to the National Bank of Georgia (`nbg.gov.ge`).
- **Offline Caching & Stale Indicators**: Retains cached rates in Room, performs periodic updates via `WorkManager` (12-hour intervals), provides manual instant refresh, and flags stale rates when disconnected.
- **Precision Decimal Math**: All financial computations utilize `BigDecimal` with 2 decimal places (`RoundingMode.HALF_UP`), eliminating binary floating-point rounding errors.
- **Manual Overrides**: Users can override automatic conversions for specific items to account for locked exchange rates or card issuer spreads.

### 3. Multiple Savings Goals & Product Management
- **Custom Goals**: Create unlimited goals with custom icons, accent colors, target completion deadlines, and monthly savings plans.
- **Calculated Targets vs. Manual Targets**: Target amount can either be dynamically calculated from the wishlist products or manually set.
- **Rich Product Wishlist**: Products support original foreign currency prices, quantities, product URLs, images, shipping costs, and tax adjustments.
- **Georgian Customs CIF & Shipping Calculator**:
  - Automatically assesses the **300 GEL CIF threshold** (Cost + Insurance + Freight).
  - Computes the 18% Georgian Import VAT and 20.00 GEL customs declaration fee for shipments exceeding 300 GEL.
  - Accounts for weight-based air freight (e.g., USA2GEORGIA, Camex rates) and bank card foreign transaction fees.

### 4. Savings Transactions & Account Ledger
- **Atomic Operations**: Atomic Room database transactions ensure balance integrity and prevent race conditions.
- **Transaction Types**: Deposits, Withdrawals, Adjustments, and Goal-to-Goal Transfers.
- **Overdraft Protection**: Prohibits withdrawals exceeding the goal's current balance unless an explicit adjustment is made.
- **Conservation of Funds**: Transfers debit the source goal and credit the destination goal atomically.
- **Monthly Ledger**: Interactive transaction history grouped chronologically by month with net savings calculation (`Deposits − Withdrawals`).

### 5. Interactive Canvas Analytics & Graphs
Built directly with custom Jetpack Compose Canvas shaders, Bezier curves, and touch scrubbers:
- **Graph A: Savings Growth**: Smooth Bezier area chart with interactive drag scrubbers, crosshair inspection, and time-range filters (7D, 30D, 3M, 6M, 12M, All Time).
- **Graph B: Monthly Net Savings**: Dual-color bar chart (emerald for positive net savings, coral for negative) with toggle between Net Savings and Gross Deposits.
- **Graph C: Savings Projection**: Historical actuals (solid green) transitioning into projected forecast lines (dashed gold) against a horizontal target line (cyan), complete with scenario chips (Conservative, Expected, Optimistic).
- **Graph D: Product Cost Breakdown**: Interactive donut chart showing percentage contribution of each wishlist product to the goal's total cost, with touch segment inspection.
- **Graph E: Goal Progress History**: Historical trajectory of completion percentages accounting for past deposits, item additions, and price fluctuations with milestone reference lines (25%, 50%, 75%, 100%).

### 6. Savings Forecast Engine
- **Deterministic Month-by-Month Projection**: Computes target milestone dates using current balances, monthly contributions, and planned one-time future bonuses.
- **Scenario Multipliers**:
  - **Conservative (0.6×)**: Anticipates unforeseen expenses and lower contributions.
  - **Expected (1.0×)**: Baseline maintaining exact planned monthly deposits.
  - **Optimistic (1.4×)**: High-saving periods or overtime bonuses.
- **Deadline Calculator**: Computes the exact monthly savings required to meet a selected future target deadline.

### 7. Backup & Restore (SAF JSON)
- **Standardized JSON Schema**: Exports all goals, products, transactions, and snapshots to a formatted JSON document via Android Storage Access Framework (SAF).
- **Integrity Validation**: Rejects corrupted files, empty archives, or orphaned records.
- **Flexible Restore**: Offers **Replace Mode** (clean restore) and **Merge Mode** (non-destructive import).

---

## 🛠️ Technology Stack

| Layer | Technologies |
|---|---|
| **Language** | Kotlin 2.3.20 |
| **UI Framework** | Jetpack Compose + Material 3 |
| **Architecture** | Clean Architecture + MVVM + Repository Pattern |
| **Dependency Injection** | Hilt 2.60.1 |
| **Local Database** | Room 2.8.5 with Room KTX & TypeConverters |
| **Local Preferences** | Jetpack DataStore Preferences |
| **Networking** | Retrofit 2.11.0 + OkHttp 4.12.0 + Kotlinx Serialization |
| **Async & Reactive** | Kotlin Coroutines & Flow |
| **Navigation** | Navigation Compose 2.8.9 |
| **Background Tasks** | AndroidX WorkManager 2.10.0 |
| **Minimum SDK** | Android 9.0 (API 28) |
| **Target / Compile SDK**| Android 15 / 16 (API 35 / 37) |

---

## 📁 Architecture & Package Structure

```
com.fundory.app/
├── data/
│   ├── backup/              # JSON Backup, Restore & Validation
│   ├── local/               # Room Entities, DAOs, Database & Converters
│   ├── preferences/         # DataStore User Preferences
│   ├── remote/              # Retrofit API Service & Models
│   └── repository/          # Currency, Goal, Product & Transaction Repositories
├── di/                      # Hilt Dependency Injection Modules
├── domain/
│   ├── engine/              # SavingsForecastEngine & ImportCostCalculator
│   └── model/               # Domain Models (Goal, Product, Transaction, etc.)
├── notification/            # Milestone & Reminder Notification Channels
├── sample/                  # Default Seed & Demonstration Data
├── ui/
│   ├── analytics/           # Analytics Screen & Interactive Charts
│   ├── components/          # Reusable UI (BottomSheets, Dialogs, Charts)
│   ├── dashboard/           # Home Dashboard & Main Savings Card
│   ├── goals/               # Goals List, Detail, Add/Edit & Import Calculator
│   ├── navigation/          # Navigation Destinations & Bottom Navigation Bar
│   ├── onboarding/          # First Launch Welcome & Seed Setup
│   ├── settings/            # Settings, Currency Refresh, Backup & Theme
│   ├── theme/               # Material 3 Color Palette, Typography & Theme
│   └── transactions/        # Transaction History & Monthly Ledger
├── worker/                  # WorkManager Exchange Rate Sync Worker
├── FundoryApplication.kt    # Application Class & Channel Setup
└── MainActivity.kt          # Edge-to-Edge Navigation Host
```

---

## 🚀 Building & Running

### Prerequisites
1. **JDK 21** or higher.
2. **Android SDK** with Platform SDK 35/37 installed.

### Build APK
To compile and assemble the debug APK:
```bash
./gradlew assembleDebug
```
The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Run Unit Tests
To execute the comprehensive unit test suite:
```bash
./gradlew testDebugUnitTest
```

Test coverage includes:
- `CurrencyConversionTest`: Decimal precision, formatting with symbols/codes, live rate multiplications, manual overrides.
- `GoalTotalCalculationTest`: Dynamic target derivation from products, progress percentages, zero target safety.
- `TransactionBalanceCalculationTest`: Atomic deposits, withdrawals, overdraft protections, goal-to-goal transfers, and historical recalculations.
- `SavingsForecastEngineTest`: Month-by-month timelines, Conservative/Expected/Optimistic scenarios, future bonuses, deadline targets.
- `ImportCostCalculatorTest`: Georgian 300 GEL CIF threshold, 18% VAT, 20 GEL customs declaration, air freight weight rates.
- `BackupValidationTest`: JSON serialization round-trips, empty dataset rejection, orphan product/transaction detection, corrupt format handling.

---

## 📄 License
Fundory is open-source software built for native Android.

