package com.banka1.credit_service.service;

import com.banka1.credit_service.dto.request.LoanRequestDto;
import com.banka1.credit_service.dto.response.LoanRequestResponseDto;
import org.springframework.security.oauth2.jwt.Jwt;

public interface LoanService {
    LoanRequestResponseDto request(Jwt jwt, LoanRequestDto loanRequestDto);
}
