package com.banka1.credit_service.controller;

import com.banka1.credit_service.domain.LoanRequest;
import com.banka1.credit_service.dto.request.LoanRequestDto;
import com.banka1.credit_service.dto.response.LoanInfoResponseDto;
import com.banka1.credit_service.dto.response.LoanRequestResponseDto;
import com.banka1.credit_service.dto.response.LoanResponseDto;
import com.banka1.credit_service.service.LoanService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/loans")
//todo pretpostavljam da je ovo za klijente, ako nije menjaj

@AllArgsConstructor
public class LoanController {
    private LoanService loanService;

    @PreAuthorize("hasRole('CLIENT_BASIC')")
    @PostMapping("/requests")
    public ResponseEntity<LoanRequestResponseDto> requests(@AuthenticationPrincipal Jwt jwt, @RequestBody @Valid LoanRequestDto loanRequestDto)
    {
        return new ResponseEntity<>(loanService.request(jwt,loanRequestDto), HttpStatus.OK);
    }

    @PreAuthorize("hasRole('CLIENT_BASIC')")
    @PutMapping("/requests/{id}/approve")
    public ResponseEntity<String> approve(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id)
    {
        return null;
    }
    @PreAuthorize("hasAnyRole('CLIENT_BASIC','BASIC')")
    @PutMapping("/requests/{id}/decline")
    public ResponseEntity<String> decline(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id)
    {
        return null;
    }

    @PreAuthorize("hasRole('CLIENT_BASIC')")
    @GetMapping("/client")
    public ResponseEntity<Page<LoanResponseDto>> find(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue = "0") @Min(value = 0) int page, @RequestParam(defaultValue = "10") @Min(value = 1) @Max(value = 100) int size)
    {
        return null;
    }

    @PreAuthorize("hasAnyRole('CLIENT_BASIC','BASIC')")
    @GetMapping("/{loanNumber}")
    public ResponseEntity<LoanInfoResponseDto> info(@AuthenticationPrincipal Jwt jwt, @PathVariable Long loanNumber)
    {
        return null;
    }

    @PreAuthorize("hasRole('BASIC')")
    @GetMapping("/requests")
    public ResponseEntity<Page<LoanRequest>> findAllLoanRequest(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "0") @Min(value = 0) int page, @RequestParam(defaultValue = "10") @Min(value = 1) @Max(value = 100) int size)
    {
        return null;
    }
    @PreAuthorize("hasRole('BASIC')")
    @GetMapping("/all")
    public ResponseEntity<Page<LoanResponseDto>> findAllLoans(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue = "0") @Min(value = 0) int page, @RequestParam(defaultValue = "10") @Min(value = 1) @Max(value = 100) int size)
    {
        return null;
    }
}
