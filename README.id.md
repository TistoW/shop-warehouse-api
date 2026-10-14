# Shop Warehouse API

[English](README.md) | Bahasa Indonesia

REST API sederhana untuk gudang toko: barang, variant-nya (ukuran, warna, dan sebagainya), harga, dan jumlah stok, ditambah order yang menolak menjual barang yang tidak ada di rak. Dibangun dengan Java 17 dan Spring Boot 3.5.

Demo online: https://geli-api.tisto.my.id membuka Swagger UI. Data di-reset setiap malam dan H2 console dinonaktifkan di sana.

## Latar belakang

Saya pernah menyelesaikan masalah ini di production, hanya saja dengan stack yang berbeda. Zenenta (https://zenenta.com) adalah sistem POS dan back-office untuk toko retail, dan ZenentaShop (https://zenentashop.com) adalah toko online-nya. Keduanya memakai data produk, variant, dan stok yang sama, sehingga penjualan di toko dan penjualan online mengambil dari satu jumlah stok. ChatSaya (https://chatsaya.com), asisten AI di WhatsApp yang saya bangun, membaca harga dan stok langsung dari Zenenta dan bisa mengubah pesanan yang sudah dikonfirmasi lewat chat menjadi order berstatus pending di sana.

Sistem-sistem tersebut berjalan di atas NestJS, Prisma, dan PostgreSQL. Repository ini adalah versi Java/Spring Boot dari ide-ide inti yang sama:

- harga dan stok disimpan di variant, bukan di item
- setiap perubahan stok meninggalkan satu baris di ledger pergerakan stok
- penjualan ditolak di level database ketika stok tidak cukup

Ini adalah implementasi baru yang saya tulis khusus untuk assessment ini. Bukan kode yang berjalan di production.

## Cara menjalankan

Anda membutuhkan Java 17. Maven tidak perlu dipasang, wrapper-nya sudah disertakan.

```
./mvnw spring-boot:run
```

Di Windows gunakan `mvnw.cmd spring-boot:run`. Jika port 8080 sedang terpakai, tambahkan `--server.port=9090`. Aplikasi berjalan di http://localhost:8080 dengan database H2 embedded yang disimpan di `./data`, jadi tidak ada yang perlu disiapkan dan data tetap ada setelah restart. Data contoh dimuat saat pertama kali dijalankan.

- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 console: http://localhost:8080/h2-console, JDBC URL `jdbc:h2:file:./data/warehouse`, user `sa`, password kosong (hentikan aplikasi terlebih dahulu jika Anda membuka file yang sama dari tool lain)

Untuk menjalankan dengan PostgreSQL menggunakan Docker:

```
docker compose up --build
```

Perintah ini menjalankan Postgres 16 dan aplikasinya (dengan profile `postgres`) di port 8080. Untuk mengarahkan aplikasi yang dijalankan secara lokal ke Postgres Anda sendiri, jalankan dengan `--spring.profiles.active=postgres` dan atur `DB_URL`, `DB_USER`, dan `DB_PASSWORD`.

Tes:

```
./mvnw test
```

Test suite berjalan di atas H2 in-memory. Ada juga varian Postgres dari concurrency test yang memakai Testcontainers. Varian ini membutuhkan Docker dan otomatis dilewati jika Docker tidak tersedia.

## Keputusan desain dan alasannya

**Variant membawa harga dan stok.** Item hanyalah entri katalog (nama, deskripsi). Apa pun yang benar-benar bisa dijual dan dihitung adalah variant dengan SKU, harga, dan stoknya sendiri. Item tanpa variant sungguhan mendapat satu variant bernama "Default" (buat cukup dengan `sku` dan `price`). Dengan begitu hanya ada satu jalur kode untuk penjualan, baik produk punya ukuran maupun tidak, dan tidak ada percabangan "apakah item ini punya variant?" di mana pun.

**Stok tidak pernah berubah lewat update biasa.** `PUT /variants/{id}` hanya mengubah nama, SKU, dan harga. Stok hanya berubah lewat stock adjustment atau order, dan keduanya menulis satu baris ke `stock_movements` (positif untuk restock, negatif untuk penjualan atau koreksi). Angka saat ini di variant adalah yang dibaca dan dijadikan dasar penjualan; ledger adalah cara untuk menjelaskannya. Variant baru dimulai dari nol dan diisi stoknya lewat adjustment RESTOCK, jadi hanya ada satu cara stok masuk ke sistem.

**Pencegahan stok minus memakai conditional atomic update.** Inti dari alur order adalah satu statement per baris:

```sql
UPDATE variants SET stock_quantity = stock_quantity - :qty
WHERE id = :id AND active = true AND stock_quantity >= :qty
```

Jika tidak ada baris yang ter-update, penjualan ditolak. Saya mempertimbangkan tiga opsi:

- Baca, cek, tulis di Java: sederhana, tetapi dua request bisa sama-sama membaca "sisa 1" dan sama-sama menjualnya.
- Pessimistic lock (`SELECT ... FOR UPDATE`): benar, tetapi menambah satu round trip dan lock tertahan selama kode Java berjalan.
- Optimistic locking dengan retry: benar, tetapi saat terjadi contention (barang populer, flash sale) sebagian besar percobaan gagal dan harus diulang, sehingga banyak kerja yang sia-sia.

Conditional update melakukan pengecekan dan penulisan dalam satu statement, sehingga database yang menyerialisasi pembeli yang berebut pada baris itu dan tidak ada yang oversell. Tidak perlu retry loop. Kolom `@Version` pada variant menangkap dua penulisan ke variant yang sama yang berebut di saat bersamaan. Version tidak diekspos ke client (tidak ada ETag atau If-Match), jadi ini tidak mencegah seseorang menimpa perubahan yang tidak pernah ia lihat. Update stok juga menaikkan version, sehingga perubahan harga yang berbenturan dengan penjualan akan mendapat 409 dan cukup dicoba ulang.

Satu order adalah satu transaction. Variant yang duplikat di dalam request digabung lebih dulu, dan baris diproses berurutan berdasarkan id variant, sehingga order yang berjalan bersamaan selalu mengambil row lock dengan urutan yang sama dan tidak bisa deadlock. Jika satu baris saja gagal, exception akan me-rollback semuanya: tidak ada order setengah jadi, tidak ada stok yang terpotong sebagian, tidak ada baris ledger yatim. Constraint `CHECK (stock_quantity >= 0)` di schema menjadi benteng terakhir kalau suatu saat ada yang melewati service.

**Snapshot harga pada baris order.** `order_items` menyimpan `unit_price` dan `line_total` saat penjualan terjadi. Mengubah harga variant di kemudian hari tidak menulis ulang order yang sudah lewat.

**Arsipkan, bukan hapus.** `DELETE` mengatur `active = false`. Order lama merujuk ke variant, jadi barisnya tidak boleh begitu saja hilang. Item dan variant yang diarsipkan disembunyikan dari daftar (gunakan `includeArchived=true` untuk melihatnya), tidak bisa dijual, dan tidak bisa di-restock. Mengarsipkan item otomatis mengarsipkan semua variant-nya.

**Flyway yang memegang schema, Hibernate hanya memvalidasi** (`ddl-auto=validate`). Migration-nya berupa SQL biasa yang jalan di H2 (mode PostgreSQL) maupun PostgreSQL asli, jadi file yang sama dipakai di semua tempat. Data contoh adalah migration kedua.

**H2 secara default, Postgres lewat profile.** Tanpa setup bagi siapa pun yang menjalankannya, sementara `docker compose` menunjukkan kode yang sama berjalan di Postgres. Concurrency test tersedia untuk keduanya.

**Package by feature** (`catalog`, `inventory`, `order`, `common`), dengan controller, service, dan repository di masing-masing. Tidak ada interface dengan satu implementasi, tidak ada library mapping: DTO berupa record dan mapping-nya hanya beberapa baris yang ditulis manual. Error dikembalikan sebagai RFC 7807 problem details.

## Asumsi

- Satu gudang dan satu mata uang (harga berupa desimal biasa, data contoh memakai IDR).
- Order adalah penjualan yang sudah selesai. Tidak ada alur pembayaran, reservasi, atau pembatalan.
- Tidak ada authentication maupun authorization.
- SKU unik di seluruh katalog.
- Request untuk menjual variant yang tidak dikenal dijawab dengan 404, sedangkan variant yang ada tetapi sudah diarsipkan dijawab dengan 409.
- Nomor order berasal dari database sequence (`ORD-20261014-000001`). Nomornya unik, tetapi bisa ada celah ketika sebuah order di-rollback.

## Contoh API

Data contoh berisi empat item. Item 1 adalah kaos dengan lima variant (id 1 sampai 5), variant 6 adalah mug, variant 7 adalah tote bag dengan stok nol, variant 8 adalah buku catatan. Daftar menggunakan paging (`page`, `size`, `sort`).

Item:

```
curl "localhost:8080/api/items?q=shirt&page=0&size=10"
curl localhost:8080/api/items/1

curl -i -X POST localhost:8080/api/items -H "Content-Type: application/json" -d '{
  "name": "Hoodie",
  "description": "Fleece, regular fit",
  "variants": [
    {"sku": "HOOD-GRY-M", "name": "Grey / M", "price": 349000},
    {"sku": "HOOD-GRY-L", "name": "Grey / L", "price": 349000}
  ]
}'

curl -i -X POST localhost:8080/api/items -H "Content-Type: application/json" \
  -d '{"name": "Sticker pack", "sku": "STICKER-1", "price": 15000}'

curl -X PUT localhost:8080/api/items/1 -H "Content-Type: application/json" \
  -d '{"name": "Classic Cotton T-Shirt", "description": "Heavyweight 180gsm cotton tee"}'

curl -i -X DELETE localhost:8080/api/items/4
```

Pembuatan data mengembalikan `201` dengan header `Location`. `DELETE` mengembalikan `204`.

Variant:

```
curl localhost:8080/api/items/1/variants
curl -i -X POST localhost:8080/api/items/1/variants -H "Content-Type: application/json" \
  -d '{"sku": "TEE-BLK-XL", "name": "Black / XL", "price": 155000}'
curl localhost:8080/api/variants/1
curl -X PUT localhost:8080/api/variants/1 -H "Content-Type: application/json" \
  -d '{"sku": "TEE-BLK-M", "name": "Black / M", "price": 155000}'
curl -X DELETE localhost:8080/api/variants/5
```

Stok:

```
curl -i -X POST localhost:8080/api/variants/4/stock-adjustments -H "Content-Type: application/json" \
  -d '{"quantity": 20, "reason": "RESTOCK", "note": "supplier delivery"}'

curl -X POST localhost:8080/api/variants/1/stock-adjustments -H "Content-Type: application/json" \
  -d '{"quantity": -2, "reason": "ADJUSTMENT", "note": "damaged in storage"}'

curl localhost:8080/api/variants/1/stock-movements
```

Order:

```
curl -i -X POST localhost:8080/api/orders -H "Content-Type: application/json" -d '{
  "items": [
    {"variantId": 1, "quantity": 2},
    {"variantId": 6, "quantity": 1}
  ]
}'

curl "localhost:8080/api/orders?size=5"
curl localhost:8080/api/orders/1
```

Jika berhasil, response-nya `201` berisi order lengkap dengan snapshot harga per baris:

```json
{
  "id": 1,
  "orderNumber": "ORD-20261014-000001",
  "totalAmount": 363000.00,
  "items": [
    {"variantId": 1, "sku": "TEE-BLK-M", "name": "Black / M", "quantity": 2, "unitPrice": 149000.00, "lineTotal": 298000.00},
    {"variantId": 6, "sku": "MUG-350", "name": "Default", "quantity": 1, "unitPrice": 65000.00, "lineTotal": 65000.00}
  ]
}
```

Meminta lebih banyak daripada stok yang ada (variant 5 sisa 3) mengembalikan `409`, dan tidak ada yang berubah, termasuk baris lain dalam order yang sama:

```
curl -i -X POST localhost:8080/api/orders -H "Content-Type: application/json" \
  -d '{"items": [{"variantId": 5, "quantity": 10}]}'
```

```json
{
  "type": "about:blank",
  "title": "Insufficient stock",
  "status": 409,
  "detail": "Not enough stock for TEE-RED-M: requested 10, available 3",
  "instance": "/api/orders",
  "sku": "TEE-RED-M",
  "requested": 10,
  "available": 3
}
```

Error validasi mengembalikan `400` beserta field yang bermasalah:

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/api/items",
  "errors": [{"field": "name", "message": "must not be blank"}]
}
```

Kasus lain: `404` untuk id yang tidak dikenal, `409` untuk SKU duplikat, untuk adjustment negatif yang membuat stok di bawah nol, untuk operasi pada data yang sudah diarsipkan, dan untuk edit yang kalah berebut dengan edit lain.

## Rencana berikutnya

- Authentication dan role (staf yang boleh mengatur stok versus staf yang hanya boleh menjual).
- Banyak gudang atau lokasi: stok akan dipindah ke tabel `(variant, location)`.
- Pembatalan order yang mengembalikan stok, dicatat sebagai movement bergaya RESTOCK yang merujuk ke order tersebut.
- Idempotency key pada `POST /orders`, agar client yang mengulang request setelah timeout tidak membuat order dua kali.
- Jika ini dipecah menjadi beberapa service, order service akan mempublikasikan event `OrderPlaced` ke Kafka lewat transactional outbox (event ditulis dalam transaction yang sama dengan order lalu diteruskan setelahnya), sehingga service lain seperti notifikasi atau reporting tetap sinkron tanpa dual write.
- Batas stok minimum per variant beserta laporannya.
