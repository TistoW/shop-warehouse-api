insert into items (name, description, active, created_at, updated_at) values
    ('Classic Cotton T-Shirt', 'Heavyweight 180gsm cotton tee, unisex fit', true, current_timestamp, current_timestamp),
    ('Ceramic Mug 350ml', 'Dishwasher safe, matte white glaze', true, current_timestamp, current_timestamp),
    ('Canvas Tote Bag', 'Natural canvas, long handles. Sold out until the next production run.', true, current_timestamp, current_timestamp),
    ('Spiral Notebook A5', 'Dotted pages, 120 sheets', true, current_timestamp, current_timestamp);

insert into variants (item_id, sku, name, price, stock_quantity, active, version, created_at, updated_at)
select i.id, v.sku, v.name, v.price, v.stock, true, 0, current_timestamp, current_timestamp
from (values
    ('Classic Cotton T-Shirt', 'TEE-BLK-M', 'Black / M', 149000.00, 25),
    ('Classic Cotton T-Shirt', 'TEE-BLK-L', 'Black / L', 149000.00, 12),
    ('Classic Cotton T-Shirt', 'TEE-WHT-M', 'White / M', 139000.00, 8),
    ('Classic Cotton T-Shirt', 'TEE-WHT-L', 'White / L', 139000.00, 0),
    ('Classic Cotton T-Shirt', 'TEE-RED-M', 'Red / M', 159000.00, 3),
    ('Ceramic Mug 350ml', 'MUG-350', 'Default', 65000.00, 40),
    ('Canvas Tote Bag', 'TOTE-NAT', 'Default', 89000.00, 0),
    ('Spiral Notebook A5', 'NOTE-A5', 'Default', 35000.00, 100)
) as v (item_name, sku, name, price, stock)
join items i on i.name = v.item_name;

insert into stock_movements (variant_id, quantity_change, reason, reference, created_at)
select id, stock_quantity, 'RESTOCK', 'initial stock', current_timestamp
from variants
where stock_quantity > 0;
