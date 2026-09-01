package com.empik.coupons.domain;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Locale;

import static lombok.AccessLevel.PROTECTED;

@Getter
@NoArgsConstructor(access = PROTECTED)
@EqualsAndHashCode(of = {"code", "countryCode"}, callSuper = false)
@Entity
@Table(
        name = "coupons",
        schema = "coupon",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_coupon_code_country",
                columnNames = {"code", "country_code"}
        )
)
public class Coupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "max_usages", nullable = false)
    private int maxUsages;

    @Column(name = "current_usages", nullable = false)
    private int currentUsages = 0;

    public Coupon(String code, String countryCode, int maxUsages) {
        this.code = code.toUpperCase(Locale.ROOT);
        this.countryCode = countryCode.toUpperCase(Locale.ROOT);
        this.maxUsages = maxUsages;
    }

    public void updateMaxUsages(int newMaxUsages) {
        this.maxUsages = newMaxUsages;
    }
}
