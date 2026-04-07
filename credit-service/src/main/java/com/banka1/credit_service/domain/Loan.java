package com.banka1.credit_service.domain;

import com.banka1.credit_service.domain.enums.CurrencyCode;
import com.banka1.credit_service.domain.enums.InterestType;
import com.banka1.credit_service.domain.enums.LoanType;
import com.banka1.credit_service.domain.enums.Status;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loan_table")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Loan extends BaseEntity{
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanType loanType;
    @NotBlank
    @Column(nullable = false)
    private String accountNumber;
    //todo pogledati sta je ovo
    //@Column(nullable = false,unique = true)
    //private Long loanNumber;
    @Positive
    @Column(nullable = false)
    private BigDecimal amount;
    @Column(nullable = false)
    private Integer repaymentMethod;
    @Column(nullable = false)
    private BigDecimal nominalInterestRate;
    @Column(nullable = false)
    private BigDecimal effectiveInterestRate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterestType interestType;
    @Column(nullable = false)
    private LocalDate agreementDate;
    @Column(nullable = false)
    private LocalDate maturityDate;
    @Column(nullable = false)
    private BigDecimal installmentAmount;
    @Column(nullable = false)
    private LocalDate nextInstallmentDate;
    @Column(nullable = false)
    private BigDecimal remainingDebt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CurrencyCode currency;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    //todo dodati listu installmenta ako mi treba




}
