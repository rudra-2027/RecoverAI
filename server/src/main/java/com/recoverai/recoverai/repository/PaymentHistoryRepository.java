package com.recoverai.recoverai.repository;

import com.recoverai.recoverai.entity.PaymentHistory;
import com.recoverai.recoverai.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {
    List<PaymentHistory> findByCustomerId(String customerId);

    long countByCustomerIdAndStatus(String customerId, PaymentStatus status);

    boolean existsByMandateIdAndStatusAndTransactionTimeAfter(
            String mandateId,
            PaymentStatus status,
            LocalDateTime transactionTime);

    @Query("select distinct p.merchantId from PaymentHistory p where p.merchantId is not null and p.merchantId <> ''")
    List<String> findDistinctMerchantIds();
}
