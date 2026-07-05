# LiSuan Cashier System

[简体中文](./README.md) | English | [繁體中文](./README_zh_TW.md)

LiSuan Cashier System is a desktop POS (Point of Sale) cashier system built with JavaFX 17. It is designed for daily retail operations, covering checkout, products, members, purchasing, inventory, returns, reports, user permissions, data backup, and hardware integration.

**Current Version**: v2.5.9 | **Latest Update**: 2026-07-05

![Java](https://img.shields.io/badge/Java-17-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-17.0.12-blue)
![Maven](https://img.shields.io/badge/Maven-3.8+-red)
![MySQL](https://img.shields.io/badge/MySQL-8.4-blue)
![License](https://img.shields.io/badge/License-MulanPSL2-blue)

---

## Features Overview

### Checkout Management
- Checkout & cash register, shopping cart CRUD, product search, member phone number recognition.
- Supports multiple payment methods including cash, WeChat Pay, Alipay, and bank cards.
- Member discounts, points accumulation, and balance consumption linked with transaction history.
- Receipt printing, print preview, cash drawer trigger, and shortcut key help.
- Shift management to record shift sales and cashier performance.

### Product & Inventory
- Product CRUD supporting product code, barcode, category, unit, specification, selling price, cost price, and stock levels.
- Unique product name constraint, duplicate barcodes allowed.
- Stock level alerts, low-stock queries, and quick inbound restocking.
- Inventory check, check details, discrepancy processing, and inventory check status tracking.
- Product CSV import and Excel/PDF export.

### Members & Customers
- Member registration, editing, queries, recharge, and balance management.
- Points accumulation from purchases, and automatic member tier upgrades.
- Tiered discount rules: Regular (no discount, 10.0), Silver (5% off, 9.5), Gold (10% off, 9.0), Diamond (15% off, 8.5).
- Recharge history queries, and linkage between member spending and return workflows.

### Purchasing Management
- Supplier profile maintenance.
- Purchase order creation, editing, querying, and status workflows.
- Purchase approval process.
- Purchase inbound stock and inbound history.
- Purchase report statistics.

### Returns Management
- Create return orders based on original transactions.
- Record returned items, return reasons, item conditions, and refund methods.
- Return approval process.
- Restores inventory upon approval and refunds via cash, balance, or points.
- Return orders and reports support querying, filtering, exporting, and printing.

### Reports & Statistics
- Transaction log querying and today's sales statistics.
- Data visualization charts.
- Purchase reports, inventory reports, profit analysis, and return reports.
- Supports Excel and PDF export.
- Profit analysis includes sales, cost, gross profit, margin, category, and daily trend views.

### Users, Permissions & Auditing
- Default roles: Administrator, Cashier, Accountant.
- BCrypt password encryption, supporting password resets and forced change on first login.
- User activation, deactivation, deletion, and role management.
- Admin auditing logs covering authentication, transactions, returns, inventory, purchases, members, users, and system settings.

### Data Backup & Recovery
- In-menu data backup and restore.
- SQL backups default to `backup/sql`.
- Backup service packages database, config, logs, invoices, and business data.
- Supports local backup, automated backup configuration, retention policies, and auto-cleanup.
- API retains cloud backup config fields for object storage integration.

### REST API & Synchronization
- Built-in Javalin API service, running on port `8080` by default.
- Currently registers 92 HTTP/WebSocket routes.
- Covers auth, products, members, transactions, inventory, reports, settings, invoices, users, printing, payments, backup, i18n, and sync status.
- Token-based authentication, role-based authorization, rate limiting, and security headers.
- WebSocket sync endpoint: `/ws/sync`.

### Hardware Support
- ESC/POS thermal printing, network printing, print queues, and printing history.
- Printer discovery, connection, status checks, and default printer configuration.
- USB HID barcode scanner integration and focus management.
- Electronic payment QR code generation and payment status queries.

### Internationalization & Themes
- Supports Simplified Chinese, English, and Traditional Chinese.
- Runtime text, dialogs, form validation, status bar, and report pages fully localized.
- Currency display internationalization.
- Themes: LiSuan, Light, Dark, compatible with legacy IntelliJ theme preferences.
- Built-in Noto Sans CJK SC fonts for cross-platform Chinese rendering.
- Left navigation icons streamlined semantically to avoid redundancy.

---

## Quick Start

### Prerequisites
- JDK 17 or higher
- Maven 3.8 or higher
- MySQL 8.4 or compatible MySQL 8.0/8.3
- Docker Compose (optional, for quick MySQL startup)

### Default Accounts
- Username: `admin`
- Initial Password: `admin123`
- *It is highly recommended to change the password immediately upon first login.*

### Starting Database

```bash
docker compose up -d mysql
```

The database initialization script is located at `docker/mysql-init/00-init-complete.sql`. The default configuration file is `config/database.properties`. For production environments, it is recommended to pass the password via environment variables:

```bash
export CASHER_DB_PASSWORD=your_password
```

### Development Run

```bash
mvn clean compile
mvn javafx:run
```

### Package and Run

```bash
mvn clean package
java -jar target/lisuan-fx-2.5.9-jar-with-dependencies.jar
```

Alternatively, launch using wildcards:

```bash
java -jar target/lisuan-fx-*-jar-with-dependencies.jar
```

### Windows

For the first-time installation, it is recommended to run:

```bat
install.bat
```

For daily launch:

```bat
start.bat
start.bat --gui
```

Database Configuration Tool:

```bat
DataConfig.bat
```

### Linux / macOS

```bash
chmod +x install.sh start.sh
./install.sh
./start.sh
```

`install.sh` checks for Java/Maven, builds the executable JAR, and guides you through configuring Docker or local/remote MySQL. `start.sh` prioritizes running the executable JAR with dependencies.

---

## Common Commands

```bash
# Compile
mvn clean compile

# Run tests
mvn test

# Package while skipping tests
mvn clean package -DskipTests

# Run specific test class
mvn test -Dtest=ProductDAOTest

# Run specific test method
mvn test -Dtest=PasswordUtilTest#testHashPassword

# Static quality checks (SpotBugs)
mvn -q -DskipTests spotbugs:check
```

---

## Shortcuts

| Shortcut Key | Function |
|--------------|----------|
| `F1` | Add item to cart |
| `Delete` | Remove selected item |
| `Ctrl+L` | Clear cart |
| `F8` | Pay with Cash |
| `Ctrl+1` | Pay with WeChat Pay |
| `Ctrl+2` | Pay with Alipay |
| `Ctrl+3` | Pay with Bank Card |
| `Ctrl+F` | Focus search bar |
| `Ctrl+M` | Focus member phone number field |
| `Ctrl+/` | Show shortcuts help |

---

## Member Tiers

| Tier | Points Range | Discount |
|------|--------------|----------|
| Regular Member | 0-1999 | No discount, calculated as 10.0 |
| Silver Member | 2000-4999 | 5% off (9.5) |
| Gold Member | 5000-9999 | 10% off (9.0) |
| Diamond Member | 10000+ | 15% off (8.5) |

Discount Calculation: `Total Amount * (Member Discount / 10.0)`.

---

## REST API

The API service runs on port `8080` by default. Before launching the API in production, security parameters must be configured:

```bash
export TOKEN_SECRET=at_least_32_characters_secret
export CORS_ALLOWED_ORIGINS=https://your-domain.example
```

Key API endpoints grouping:

| Group | Path | Description |
|-------|------|-------------|
| Health Check | `/api/health` | Server and database status |
| Authentication | `/api/auth/*` | Login, refresh token, logout, current user |
| Products | `/api/products/*` | Product CRUD, low-stock warnings |
| Members | `/api/members/*` | Member CRUD, phone query, recharge |
| Transactions | `/api/transactions/*` | Transaction creation, querying, refunding, today's stats |
| Inventory | `/api/inventory/*` | Inventory list, alerts, checking, updates |
| Reports | `/api/reports/*` | Daily, monthly reports, hot products, payment method stats |
| Settings | `/api/settings/*` | Read/write system settings |
| Invoices | `/api/invoices/*` | Invoice creation, querying, voiding, print history |
| Users | `/api/users/*` | User management |
| Printing | `/api/printers/*` | Printer discovery, connection, status, and printing |
| Payment | `/api/payment/*` | Payment creation, status querying, refunding, config |
| Backup | `/api/backup/*` | Backup execution, restoring, downloading, configuring |
| Internationalization | `/api/i18n/*` | Query language, messages, and language packs |
| Synchronization | `/ws/sync`, `/api/sync/status` | Multi-terminal sync and online status |

All API endpoints except `/api/health` and `/api/auth/login` require authentication by default.

---

## Database

The project uses MySQL, with HikariCP as the connection pool. Key database tables include:

| Table | Description |
|-------|-------------|
| `products` | Product information, product name is unique |
| `members` | Member information |
| `transactions`, `transaction_items` | Transactions and transaction details |
| `return_orders`, `return_order_items` | Return orders and return details |
| `purchase_orders`, `purchase_order_items` | Purchase orders and details |
| `purchase_approvals` | Purchase approval records |
| `purchase_inbound`, `purchase_inbound_items` | Purchase inbound stock and details |
| `suppliers` | Supplier profiles |
| `inventory_check`, `inventory_check_items` | Inventory checks and details |
| `users` | Users and roles |
| `shifts` | Shift logs |
| `invoices` | Invoices |
| `payment_orders`, `refund_records` | Payment orders and refund records |
| `promotions` | Promotion rules |
| `backup_records`, `backup_config` | Backup logs and configuration |
| `operation_logs` | Audit logs |
| `settings` | System settings |

For database schema version details, see `docker/mysql-init/DATABASE_VERSIONS.md`.

---

## Project Structure

```text
src/main/java/com/cashier/
├── api/           # Javalin REST API, Auth, Sync
├── component/     # Reusable JavaFX components
├── constant/      # App, DB, and system property constants
├── controller/    # JavaFX Controllers
├── dao/           # Data Access Objects (DAOs)
├── i18n/          # I18n management and key constants
├── model/         # Business entities
├── notification/  # Notification system
├── printer/       # Printing devices and templates
├── scanner/       # Barcode scanner integration
├── service/       # Business services
└── util/          # Utilities for DB, import/export, logs, themes
```

```text
src/main/resources/
├── com/cashier/view/      # FXML Views
├── com/cashier/i18n/      # Language bundles (.properties)
├── css/                   # LiSuan, Light, and Dark themes
├── fonts/                 # Built-in fonts
└── images/                # App icons and image assets
```

---

## Development Conventions

- Logs must uniformly use `LoggerFactoryUtil.getLogger()`.
- DAOs must use `PreparedStatement` and extend `BaseDAO`.
- Business transactions spanning multiple DAOs must be handled within transaction blocks via entry points like `DatabaseManager.executeBooleanTransaction()`.
- New UI views should prefer FXML + Controller and provide fully localized bundles for all three supported languages.
- New localized keys should be declared in `I18nKeys` to prevent scattered literal strings in controllers.
- New non-localized constants should be centralized in their respective constant classes to avoid duplicate literals.
- After fixing code quality warnings, run compiling checks, and ensure related tests are executed if public logic is modified.

---

## Recent Updates

### v2.5.9-maintenance (2026-07-05)
- Fixed API Token security test failure.
- Optimized Mockito configuration for test environments.
- Strengthened security prompts in production configuration templates.
- Unified database initialization scripts, enabling out-of-the-box support for more modules.
- Optimized README structure for better readability.

### v2.5.9-maintenance (2026-07-04)
- Organized `I18nKeys`, centralizing commonly used internationalization keys.
- Centralized non-localized constants such as system properties, database configs, resource bundles, and date-time formats.
- Fixed several code quality warnings including complexity, duplicate strings, switch/default statements, resource closures, and static analysis issues.
- Updated left navigation icons to ensure uniqueness and logical grouping.
- Re-organized README to reflect current features, API, database schema, and maintenance procedures.

### v2.5.9 (2026-06-20)
- Enhanced localization: unified Simplified Chinese, English, and Traditional Chinese resources, filling gaps in dialogs, statuses, dates, approvals, and form validation hints.
- Optimized return workflow: added quick date filters, fixed multilingual displays for viewing original transactions, printing receipts, completing returns, and exporting notifications.
- Fixed purchasing and inventory check: fixed invisible columns in inbound stock history, text truncation in inventory check type labels, and corrected empty table warnings to respect current language.
- Fixed form layouts: removed vertical red lines caused by empty error labels on product add/edit views and localized validation errors.
- Theme and font optimizations: defaulted to the LiSuan theme, prioritized Noto Sans CJK SC for Simplified Chinese, and improved cross-platform fallbacks.
- Simplified language support: removed Japanese and Korean, maintaining only Simplified Chinese, English, and Traditional Chinese.

### v2.5.7-l10n (2026-06-20)
- Streamlined language packs down to Simplified Chinese, English, and Traditional Chinese.
- Filled missing localization keys to minimize fallback occurrences.
- Optimized UI copy for dialogs, sub-pages, status bars, and tooltip notifications.
- Adjusted widths for top headers and input fields across various pages.

### v2.5.7 (2026-06-12)
- Added support for separating development and production environments.
- Uses `root` for development, and the dedicated `lisuan` user for production.
- Refactored `FormValidator` parsing methods to prevent `NumberFormatException`.
- Installation scripts now support `.env` files and automatic environment detection.
- Windows configuration tool updated to support environment variables.
- Branding unified across scripts as "LiSuan System".

### v2.5.6 (2026-06-10)
- Unified branding to "狸算(LiSuan)收银系统" (LiSuan Cashier System).
- Updated user interfaces, shell scripts, receipt prints, application icons, and Docker container branding.

### v2.5.5 (2026-06-09)
- Optimized tab width and visual style of close buttons.
- Added font-size adjustment capability.
- Fixed compatibility issues with the `fcitx5` input method.
- Improved Traditional Chinese text rendering and cross-platform font fallback chains.

### v2.5.4 (2026-05-21)
- Added GUI database configuration utility tool.
- Optimized Windows distribution package and setup script.

### v2.5.3 (2026-05-15)
- Windows-specific performance optimizations.
- Enhanced application startup user experience.
- Strengthened data sync robustness.
- Completed DAO layer refactoring.
- Fixed resource disposal issues and memory leaks.

---

## Troubleshooting

**Application Fails to Start**
- Verify JDK is version 17 or higher.
- Ensure MySQL is running.
- Check database settings in `config/database.properties`.
- Check log files inside the `logs/` directory.

**Database Connection Error**
- Double-check host, port, database name, username, and password.
- If using environment variables, ensure `CASHER_DB_PASSWORD` is properly exported.
- For Docker, verify container state with `docker compose up -d mysql`.

**API Service Fails to Run**
- Verify that a `TOKEN_SECRET` of at least 32 characters has been defined.
- In production, avoid using wildcard `CORS_ALLOWED_ORIGINS=*`.
- Check logs for port conflicts or config errors.

**Scanner Unresponsive**
- Verify the barcode scanner is connected and operating in USB HID keyboard emulation mode.
- Ensure focus is not locked inside other inputs or text boxes on the current screen.

**Printer Unresponsive**
- Check printer power, cable connection, driver status, and paper.
- Review printer state detection and printing logs.
- For network printing, verify IP address, port, and network firewall rules.

**Incorrect Chinese Characters Rendering**
- Ensure built-in Noto Sans CJK SC fonts are packed inside resources.
- On Linux, consider installing a local CJK font package.

---

## License

Mulan Permissive Software License v2 (MulanPSL2)

---

**Repository**: https://gitee.com/nevell/lisuan.git

**Issues**: https://gitee.com/nevell/lisuan/issues
