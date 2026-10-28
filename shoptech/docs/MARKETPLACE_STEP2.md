> Đã có checkpoint tiếp theo: [Đơn hàng và doanh thu theo shop](MARKETPLACE_STEP3.md).

# Marketplace — checkpoint 2: shop-owned products

## Completed

- Products have a required shop owner; existing catalog rows belong to ShopTech.
- SKU uniqueness is now `(shop_id, sku)`, including soft-deleted products.
- Seller can list/filter/page, create, edit, hide, soft-delete and restore products
  belonging to the authenticated Seller's shop. Request parameters cannot choose
  the owner or override platform moderation.
- Suspended shops are read-only for Sellers. Their products, blocked products and
  products owned by inactive/deleted accounts cannot be bought or viewed publicly.
- Admin's former create/edit/delete routes return 403. Admin has a monitoring
  list filtered by shop/category, detail view, block/unblock with reasons and history.
- Shared SB Admin 2 product list/form and management layout are reused.
- In the isolated profile uploaded images are stored in `.local/media/` and served
  under `/sandbox-media/`. Existing Cloudinary images remain unchanged.

## Database

V002 was applied only to `127.0.0.1:55432/shoptech_marketplace`.
It has an explicit database-name guard and requires V001.

- Migration: `src/main/resources/db/migrations/V002__shop_owned_products.sql`.
- Pre-step-2 backup: `.local/backups/step1-before-products.dump`.
- 12 original products assigned to ShopTech; all 10 original orders retained.
- Browser-created product: `AK-DEMO-01`, owned by the demo Seller's An Khang Tech.
- Demo image uploads are local files, not Cloudinary writes. Preserve `.local/media`
  together with any backup containing products that reference these local files.
- Source Supabase was not changed or used for this checkpoint's writes.

## Preview

Start with `scripts/Start-MarketplaceSandbox.ps1` (see checkpoint 1).
Do not start this development branch with the original database configuration.

| Page | Demo account |
| --- | --- |
| http://localhost:18081/seller/products | sandbox-user |
| http://127.0.0.1:18081/admin/products | sandbox-admin |

The temporary password remains in `.local/preview.env`. Original users' passwords
are unchanged. The sample product is left active and restored after testing.

## Verification

- Full standard suite: 124 discovered, 87 executed/passed, 37 PostgreSQL cases
  initially skipped because they are opt-in. Log: `.local/test-step2-full.log`.
- The 37 optional PostgreSQL cases were then explicitly enabled and all passed
  against a separate loopback database `shoptech_person3_test` in the local container.
  They use an isolated temporary schema and never use the preview or source database.
  Log: `.local/test-step2-postgres.log`.
- Ten new Seller-product cases: forged ownership/moderation fields, SKU scope,
  cross-shop IDOR, edit/image/slug retention, trash/restore, Admin read-only operations,
  platform moderation and cart/checkout rejection, suspended shops, CSRF/roles,
  validation, scoped filters and pagination.
- Browser on the actual preview PostgreSQL: create with an image, edit price/stock
  and replace image, soft-delete/restore, Admin block and confirm zero public
  search results, then unblock.
- Evidence: `.local/evidence/*step2.png`.

## Next checkpoint

Seller order processing and shop revenue are not yet implemented. The existing
Admin order/revenue modules still aggregate the original whole-store orders.
Their navigation labels now identify them as whole-platform data; operational
order permissions will be separated when shop orders are introduced.

Next work must add shop orders, immutable shop/commission snapshots on order lines,
multi-shop checkout, Seller-only transitions and per-shop revenue. The original
10 orders must be backfilled with matching totals before changing those queries.

Production remains unchanged. No automatic deployment, restore, rollback or Git
push is performed by the preview scripts.
