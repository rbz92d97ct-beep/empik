package com.empik.coupons.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Getter
@NoArgsConstructor(access = PROTECTED)
@Entity
@Table(
        name = "coupon_usages",
        schema = "coupon",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_coupon_usage_coupon_user",
                columnNames = {"coupon_id", "user_id"}
        )
)
public class CouponUsage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Column(name = "user_id", nullable = false)
    private String userId;

    public CouponUsage(Coupon coupon, String userId) {
        this.coupon = coupon;
        this.userId = userId;
    }
}
