package com.campus.trade.payment.repository;

import com.campus.trade.payment.entity.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {
    Optional<PaymentRecord> findByOrderNo(String orderNo);
    Optional<PaymentRecord> findByAlipayTradeNo(String alipayTradeNo);
}
