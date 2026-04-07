package com.banka1.credit_service.repository;

import com.banka1.credit_service.domain.LoanRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanRequestRepository extends JpaRepository<LoanRequest,Long> {
}
