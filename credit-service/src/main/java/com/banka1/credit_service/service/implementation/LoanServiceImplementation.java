package com.banka1.credit_service.service.implementation;

import com.banka1.credit_service.domain.LoanRequest;
import com.banka1.credit_service.domain.enums.CurrencyCode;
import com.banka1.credit_service.domain.enums.InterestType;
import com.banka1.credit_service.domain.enums.LoanType;
import com.banka1.credit_service.domain.enums.Status;
import com.banka1.credit_service.dto.request.LoanRequestDto;
import com.banka1.credit_service.dto.response.AccountDetailsResponseDto;
import com.banka1.credit_service.dto.response.ConversionResponseDto;
import com.banka1.credit_service.dto.response.LoanRequestResponseDto;
import com.banka1.credit_service.repository.LoanRequestRepository;
import com.banka1.credit_service.rest_client.AccountService;
import com.banka1.credit_service.rest_client.ExchangeService;
import com.banka1.credit_service.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor

public class LoanServiceImplementation implements LoanService {


    private final AccountService accountService;
    private final ExchangeService exchangeService;
    private final LoanRequestRepository loanRequestRepository;

    @Value("${banka.security.id}")
    private String appPropertiesId;



    BigDecimal[] iznosi={BigDecimal.valueOf(500_000), BigDecimal.valueOf(1_000_000), BigDecimal.valueOf(2_000_000), BigDecimal.valueOf(5_000_000), BigDecimal.valueOf(10_000_000), BigDecimal.valueOf(20_000_000)};

    private BigDecimal interestRate(BigDecimal amount, CurrencyCode currencyCode,LoanType loanType, InterestType interestType,BigDecimal referenceRate)
    {

        if(currencyCode!=CurrencyCode.RSD)
        {
            ConversionResponseDto conversionResponseDto=exchangeService.calculate(currencyCode,CurrencyCode.RSD,amount);
            amount=conversionResponseDto.toAmount();
        }

        int start=0;
        int end=iznosi.length-1;
        while(start<=end)
        {
            int mid=start + (end-start)/2;
            int result=amount.compareTo(iznosi[mid]);
            switch (result)
            {
                case 0 -> start=mid;
                case -1-> end=mid-1;
                case 1-> start=mid+1;
            }
            if(result==0)
                break;
        }
        BigDecimal val=BigDecimal.valueOf(6.25).subtract(BigDecimal.valueOf(0.25).multiply(BigDecimal.valueOf(start))).add(loanType.getMarza());
        if(interestType==InterestType.VARIABLE)
            val=val.add(referenceRate);
        return val;
    }

    @Transactional
    @Override
    public LoanRequestResponseDto request(Jwt jwt, LoanRequestDto loanRequestDto) {
        if(loanRequestDto.getLoanType()== LoanType.STAMBENI)
        {
                if(loanRequestDto.getRepaymentPeriod()>360 || loanRequestDto.getRepaymentPeriod() % 60 != 0)
                {
                    throw new IllegalArgumentException("Nevalidan repaymentPeriod, mora biti 60, 120, 180, 240, 300 ili 360");
                }
        }
        else
        {
            if(loanRequestDto.getRepaymentPeriod()>84 || loanRequestDto.getRepaymentPeriod() % 12 != 0)
            {
                throw new IllegalArgumentException("Nevalidan repaymentPeriod, mora biti 12, 24, 36, 48, 60, 72 ili 84");
            }
        }
        AccountDetailsResponseDto accountDetailsResponseDto=accountService.getDetails(loanRequestDto.getAccountNumber());
        if(accountDetailsResponseDto == null)
            throw new IllegalArgumentException("Ne postoji racun:"+loanRequestDto.getAccountNumber());
        if(accountDetailsResponseDto.getOwnerId()==null || !accountDetailsResponseDto.getOwnerId().equals(((Number) jwt.getClaim(appPropertiesId)).longValue()))
            throw new IllegalArgumentException("Nisi vlasnik racuna");
        if(accountDetailsResponseDto.getCurrency()!=loanRequestDto.getCurrency())
        {
            throw new IllegalArgumentException("Valuta racuna ne odgovara valuti kredita");
        }
        LoanRequest loanRequest=loanRequestRepository.save(new LoanRequest(loanRequestDto.getLoanType(),loanRequestDto.getInterestType(),loanRequestDto.getAmount(),loanRequestDto.getCurrency(),loanRequestDto.getPurpose(),loanRequestDto.getMonthlySalary(),loanRequestDto.getEmploymentStatus(),loanRequestDto.getCurrentEmploymentPeriod(),loanRequestDto.getRepaymentPeriod(),loanRequestDto.getContactPhone(),loanRequestDto.getAccountNumber(),loanRequestDto.getClientId(), Status.PENDING));
        return new LoanRequestResponseDto(loanRequest.getId(),loanRequest.getCreatedAt());
    }
}
