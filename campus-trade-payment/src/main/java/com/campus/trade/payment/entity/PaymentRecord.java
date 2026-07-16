package com.campus.trade.payment.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_records")
@Data
public class PaymentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String orderNo;

    /** 支付宝交易流水号 */
    private String alipayTradeNo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /** WAIT_PAY / SUCCESS / FAILED */
    @Column(nullable = false, length = 20)
    private String status = "WAIT_PAY";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime paidAt;
}
