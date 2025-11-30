# 🔥 STEP08: 쿼리 및 인덱스 최적화 보고서

## 🧩 1️⃣ 테스트 목적
`products` 테이블에서 **상품 ID(id)** 로 특정 상품을 조회할 때,  
인덱스가 있을 때와 없을 때의 **조회 성능 차이**를 비교한다.

> 목표: `EXPLAIN` 실행계획과 실제 쿼리 실행 시간 비교를 통해  
> 인덱스가 조회 성능에 미치는 영향을 확인한다.

---

## 🍏 2️⃣ 병목 발생 예상 지점

| 기능/쿼리      | 문제                                | 솔루션                                    | 기대 효과                 |
|------------|-----------------------------------|----------------------------------------| --------------------- |
| 상품 ID 조회   | 다수 요청 시 락 발생                      | PK 인덱스 활용 + 필요한 경우 낙관 락 적용             | 조회 속도 확보, 동시성 개선      |
| 인기 상품 조회   | `sold_count` 정렬 시 Full Table Scan | `sold_count` 컬럼에 인덱스 생성                | 조회 속도 향상              |
| 최신 상품 조회   | `created_at` 정렬 시 Full Table Scan | `created_at` 컬럼에 인덱스 생성                | 조회 속도 향상              |
| 카테고리별 상품 조회 | 필터 조건 많음                          | `(category_id, is_active)` 복합 인덱스      | 빠른 검색                 |
| 사용자 장바구니 조회 | 유저별 조회 빈번                         | `(user_id, product_id)` 복합 인덱스         | 조회 및 중복 체크 성능 향상      |
| 선착순 쿠폰 발급  | 동시 발급 요청 많음                       | `SELECT ... FOR UPDATE` 최소화 / Redis 캐싱 | DB 락 최소화, 동시 처리 성능 향상 |

---

## 🧱 3️⃣ 테스트 환경
- **DBMS**: MySQL 8.0
- **테스트 도구**: DBeaver
- **테스트 테이블**: `products`
- **데이터 개수**: 100,000 rows

### (1) 테스트용 더미 데이터 생성
```sql
-- 10만 건 더미 데이터 삽입 (Derived Table 사용)
SET @rownum := 0;

INSERT INTO products (id, category_id, name, description, price, stock, created_at, updated_at)
SELECT
    (@rownum := @rownum + 1) AS id,
    FLOOR(1 + RAND() * 10) AS category_id,   -- 1~10 랜덤
    CONCAT('상품_', @rownum) AS name,
    '테스트용 상품입니다.' AS description,
    ROUND(RAND() * 100000, 2) AS price,
    FLOOR(RAND() * 100) AS stock,
    NOW() AS created_at,
    NOW() AS updated_at
FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
) t1
CROSS JOIN (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
) t2
CROSS JOIN (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
) t3
CROSS JOIN (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
) t4
CROSS JOIN (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
) t5
LIMIT 100000;
```

---

## ⚡ 4️⃣ 인덱스 유무에 따른 조회 성능 비교

### (1) 인덱스 없음
```sql
DROP INDEX idx_products_name ON products;  -- 인덱스 제거
```

- 특정 상품 조회

```sql
SELECT * FROM products WHERE name = '상품_100';
```
- 실행 시간: 약 0.04s
- EXPLAIN 결과: 99,456행 스캔 → Full Table Scan
- 모든 행을 순차적으로 탐색하므로 이론상 시간복잡도 O(n) 수준

### (2) 인덱스 있음
```sql
CREATE INDEX idx_products_name ON products(name);  -- 인덱스 생성
```

- 특정 상품 조회

```sql
SELECT * FROM products WHERE name = '상품_100';
```
- 실행 시간: 약 0.002s
- EXPLAIN 결과: 1행 조회 → Index Scan 사용
- B-tree 인덱스를 사용하면 시간복잡도가 이론상 O(log n) 수준으로 줄어듦
  즉, 데이터가 많아도 조회 속도가 크게 향상됨

---

## 🔹 5️⃣ 인덱스 장단점
| 장점                                 | 단점                   |
| ---------------------------------- | -------------------- |
| 조회 속도 향상 (검색, 정렬)                  | 쓰기(CUD) 작업 시 오버헤드 발생 |
| 정렬된 데이터 구조 활용 가능 (ORDER BY 자동 최적화) | 인덱스 관리 필요 (추가/삭제)    |

---

## ✅ 6️⃣ 결론
- 인덱스를 사용하지 않으면 Full Table Scan으로 조회 시간이 느림
- 인덱스를 사용하면 Index Scan으로 조회 시간이 크게 감소
- 조회 성능 최적화를 위해서는 자주 검색되는 컬럼과 정렬 기준 컬럼에 인덱스를 적절히 생성해야 함

---

## 🔹 7️⃣ 추가 참고 사항
- 인덱스는 읽기 성능을 높이는 대신 쓰기(CUD) 성능 저하 가능
- 데이터 양과 조회 패턴을 고려하여 **필요한 컬럼만 인덱스 적용**
- 실 서비스 환경에서는 **실제 트래픽 기반 성능 테스트** 필요
