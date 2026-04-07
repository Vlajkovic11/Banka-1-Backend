package com.banka1.credit_service.dto.response;

import com.banka1.credit_service.domain.enums.CurrencyCode;
import com.banka1.credit_service.domain.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class InstallmentResponseDto {
    private BigDecimal installmentAmount;
    private BigDecimal interestRateAtPayment;
    private CurrencyCode currency;
    private LocalDate expectedDueDate;
    private LocalDate actualDueDate;
    private PaymentStatus paymentStatus;
}
