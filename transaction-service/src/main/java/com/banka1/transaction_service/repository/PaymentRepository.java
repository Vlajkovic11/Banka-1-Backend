package com.banka1.transaction_service.repository;

import com.banka1.transaction_service.domain.Payment;
import com.banka1.transaction_service.domain.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PaymentRepository extends JpaRepository<Payment,Long>, JpaSpecificationExecutor<Payment> {

    @Modifying
    @Query("""
    UPDATE Payment p
    SET p.status = :newStatus
    WHERE p.status = :oldStatus
    AND p.createdAt < :threshold
""")
    int markStuckPayments(
            TransactionStatus oldStatus,
            TransactionStatus newStatus,
            LocalDateTime threshold
    );


    @Query("""
    SELECT p
    FROM Payment p
    WHERE p.fromAccountNumber = :accountNumber
       OR p.toAccountNumber = :accountNumber
    ORDER BY p.createdAt DESC
""")
    Page<Payment> findByAccountNumber(
            @Param("accountNumber") String accountNumber,
            Pageable pageable
    );
}
