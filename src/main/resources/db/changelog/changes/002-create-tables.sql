CREATE TABLE IF NOT EXISTS coupon.coupons
(
    id             BIGSERIAL PRIMARY KEY,
    code           VARCHAR(100) NOT NULL,
    country_code   VARCHAR(2)   NOT NULL,
    max_usages     INT          NOT NULL CHECK (max_usages > 0),
    current_usages INT          NOT NULL DEFAULT 0 CHECK (current_usages >= 0),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_coupon_code_country UNIQUE (code, country_code)
);

CREATE INDEX IF NOT EXISTS idx_coupons_country_code ON coupon.coupons (country_code);

CREATE TABLE IF NOT EXISTS coupon.coupon_usages
(
    id         BIGSERIAL PRIMARY KEY,
    coupon_id  BIGINT       NOT NULL REFERENCES coupon.coupons (id) ON DELETE CASCADE,
    user_id    VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_coupon_usage_coupon_user UNIQUE (coupon_id, user_id)
);
