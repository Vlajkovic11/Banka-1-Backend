package com.banka1.order.client.impl;

import com.banka1.order.client.StockClient;
import com.banka1.order.dto.StockExchangeDto;
import com.banka1.order.dto.StockListingDto;
import com.banka1.order.dto.ExchangeStatusDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * RestClient-based implementation of {@link StockClient}.
 * Active in all profiles except "local".
 */
@Component
@Profile("!local")
@RequiredArgsConstructor
@Slf4j
public class StockClientImpl implements StockClient {

    private final RestClient stockRestClient;

    /**
     * {@inheritDoc}
     */
    @Override
    public StockListingDto getListing(Long id) {
        return stockRestClient.get()
                .uri("/api/listings/{id}?period=DAY", id)
                .retrieve()
                .body(StockListingDto.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StockExchangeDto getStockExchange(Long id) {
        return stockRestClient.get()
                .uri("/api/stock-exchanges/{id}", id)
                .retrieve()
                .body(StockExchangeDto.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean isStockExchangeOpen(Long id) {
        return stockRestClient.get()
                .uri("/api/stock-exchanges/{id}/is-open", id)
                .retrieve()
                .body(Boolean.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExchangeStatusDto getExchangeStatus(Long id) {
        // Assume the endpoint returns ExchangeStatusDto
        return stockRestClient.get()
                .uri("/api/stock-exchanges/{id}/status", id)
                .retrieve()
                .body(ExchangeStatusDto.class);
    }

    @Override
    public void refreshListing(Long id) {
        try {
            stockRestClient.post()
                    .uri("/api/internal/listings/{id}/refresh", id)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            // Tolerate provider rate-limits and stock-service errors: the order should still
            // progress on whatever quote data is already persisted, and the next scheduled
            // refresh will recover. Failing the order confirm here would punish the user for
            // an upstream issue they cannot resolve.
            log.warn("Listing refresh failed for id={}, falling back to stale data: {}", id, e.getMessage());
        }
    }
}
