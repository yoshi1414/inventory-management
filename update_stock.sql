-- 30アイテムの在庫を0に更新
-- そのうち10アイテムを販売中止に設定

-- 30アイテムすべての在庫を0に更新
UPDATE products SET stock = 0, updated_at = NOW()
WHERE product_code IN (
    '93TGNAY7', 'TYYZ5AV7', '5C94FGTQ', 'XBHKYPQB', '728GCZFU',
    '27R9M103', 'JDOVOMY2', '0KHFMXFN', 'T4F2EW7G', 'GL199BEL',
    'SX7HALUU', '1AOXE5QW', 'KOXR70B4', 'X2D1GWO5', 'ZKI2USNA',
    'EPKS7ECS', 'UB8UO1PA', '6ID782KK', 'YWJW03Z5', '44WFTM8R',
    'CVYLYMJ3', '9UQRU4ZU', '4RBD28EN', 'XNTCQGCN', 'T27XMDD2',
    'OR97MBZ1', 'HGFIOVL2', '758FMQ42', '8XJVYL79', 'YWS54ZGS'
);

-- 最初の10アイテムを販売中止に変更
UPDATE products SET status = 'inactive', updated_at = NOW()
WHERE product_code IN (
    '93TGNAY7', 'TYYZ5AV7', '5C94FGTQ', 'XBHKYPQB', '728GCZFU',
    '27R9M103', 'JDOVOMY2', '0KHFMXFN', 'T4F2EW7G', 'GL199BEL'
);

-- 確認用クエリ
SELECT product_code, product_name, stock, status 
FROM products 
WHERE product_code IN (
    '93TGNAY7', 'TYYZ5AV7', '5C94FGTQ', 'XBHKYPQB', '728GCZFU',
    '27R9M103', 'JDOVOMY2', '0KHFMXFN', 'T4F2EW7G', 'GL199BEL',
    'SX7HALUU', '1AOXE5QW', 'KOXR70B4', 'X2D1GWO5', 'ZKI2USNA',
    'EPKS7ECS', 'UB8UO1PA', '6ID782KK', 'YWJW03Z5', '44WFTM8R',
    'CVYLYMJ3', '9UQRU4ZU', '4RBD28EN', 'XNTCQGCN', 'T27XMDD2',
    'OR97MBZ1', 'HGFIOVL2', '758FMQ42', '8XJVYL79', 'YWS54ZGS'
)
ORDER BY product_code;
