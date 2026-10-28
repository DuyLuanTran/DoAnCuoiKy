# Marketplace — checkpoint 1

Latest progress: [checkpoint 2 — shop-owned products](MARKETPLACE_STEP2.md).

## Scope and next steps

Implemented: shop registration, Admin approval/rejection/suspension/resumption,
default/per-shop commission settings, review history, Seller dashboard and own-shop
information editing. Both management areas reuse SB Admin 2. Seller menus exclude
global administration; permissions are enforced in the backend.

Next checkpoint: shop-scoped product management. Multi-shop checkout, shop orders,
and applying commission to purchases are not implemented yet. V001 does not change
products or orders. Commission displayed here is configuration, not accounting.

## Isolation and backups

- Branch: `codex/marketplace-isolated`; changes have not been committed or pushed.
- Starting revision/local patch: `.local/backups/base-commit.txt` and
  `.local/backups/before-marketplace.patch`.
- Public application schema/data dump: `.local/backups/source-before-marketplace.dump`.
  It contains private data and is ignored by Git. Preserve it outside build directories.
- Source connection used `default_transaction_read_only=on`.
- Restore verified before migration: 6 users, 12 products, 7 categories, 4 carts,
  10 orders and 17 order details.
- Dedicated PostgreSQL: Docker container `shoptech-marketplace-db`, bound to
  `127.0.0.1:55432`, database `shoptech_marketplace`, persistent Docker volume.
- Source server: PostgreSQL 17.6; local container: PostgreSQL 18.6. Validation on
  the source server version is still required before deployment.
- Database password, JWT secret and demo password are in Git-ignored `.local/*.env`.
- Real email, Cloudinary writes and Gemini requests are disabled in the preview.
- The isolated application guard rejects unexpected database URLs/database names,
  automatic DDL, and real external-service settings.
- Existing product images are only read by URL.

Do not run `db/schema.sql` on the original database: it starts with DROP statements.
The V001 migration has a database-name guard and was applied only to the local copy.
Do not apply this sandbox-only script directly to Supabase.

## Run the preview

From `D:\DoAnCuoiKy\shoptech` in PowerShell:

```powershell
.\scripts\Start-MarketplaceSandbox.ps1
```

The script uses Java 17 and the standalone `application-isolated.properties`,
never imports `.env`, and explicitly chooses local credentials and the isolated
profile. URL: http://127.0.0.1:18081. Stop with Ctrl+C. Docker Desktop must be running.
Do not use the ordinary production start command to preview this branch.

First-time provisioning: `scripts/Initialize-MarketplaceSandbox.ps1`. It creates a
read-only source dump, restores into an empty local database, and leaves non-empty
databases intact. Migration V001 is a separate explicit step, never automatic.

## Demo accounts

All use `SANDBOX_DEMO_PASSWORD` in `.local/preview.env`. Original users' passwords
were not changed.

| Account | Purpose |
| --- | --- |
| sandbox-admin | Global Admin: `/admin/shops` |
| sandbox-user | Approved Seller of An Khang Tech: `/seller` |
| sandbox-other | User with no shop, for registration testing |

The bootstrap ShopTech owner in the clone is the first active original Admin.
Confirm the intended owner before planning production migration. The default
commission was set to 5% as demonstration data only.

## Verification

- Maven: 114 discovered, 77 executed/passed, 37 conditional PostgreSQL tests skipped
  because their dedicated test database was not configured.
- Shop tests cover page rendering/filtering, pending/approval, duplicate/invalid
  registration, rejection/resubmission/stale review, cross-shop ownership and
  mass assignment, suspension/resumption, commission validation and CSRF/login.
- Guard tests reject a non-sandbox URL and real external-service settings.
- Browser checked on the local PostgreSQL clone: registration, PENDING state,
  Admin list/detail, commission save, approval, Seller dashboard, own-address edit.
- Evidence: `.local/evidence/`; test log: `.local/test-step1-verified.log`.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
.\mvnw.cmd test
```

Review this checkpoint's live pages before starting product ownership and Seller
product CRUD. No rollback, overwrite, source deployment or push is automatic.
