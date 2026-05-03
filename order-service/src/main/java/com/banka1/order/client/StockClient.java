package com.banka1.order.client;

import com.banka1.order.dto.ExchangeStatusDto;
import com.banka1.order.dto.StockExchangeDto;
import com.banka1.order.dto.StockListingDto;

/**
 * Client interface for communicating with the stock-service.
 * Used to retrieve listing prices and verify whether a stock exchange is currently open.
 */
public interface StockClient {

    /**
     * Fetches a security listing by its identifier.
     *
     * @param id the listing's unique identifier
     * @return listing details including ticker, price, and contract size
     */
    StockListingDto getListing(Long id);

    /**
     * Fetches a stock exchange by its identifier.
     *
     * @param id the exchange's unique identifier
     * @return exchange details including name, currency, and open status
     */
    StockExchangeDto getStockExchange(Long id);

    /**
     * Checks whether a stock exchange is currently open for trading.
     *
     * @param id the exchange's unique identifier
     * @return {@code true} if the exchange is open, {@code false} otherwise
     */
    Boolean isStockExchangeOpen(Long id);

    /**
     * Gets the status of a stock exchange.
     *
     * @param id the exchange's unique identifier
     * @return exchange status
     */
    ExchangeStatusDto getExchangeStatus(Long id);

    /**
     * Triggers an on-demand refresh of one listing's market-data snapshot in stock-service.
     * Used by the order-service approval flow so a confirmed BUY can fill against current
     * quote data even on weekends when the scheduled refresh is gated off by closed exchanges.
     * Implementations should treat upstream failures as non-fatal — the order should still
     * progress on stale data and rely on the next scheduled refresh.
     *
     * @param listingId the listing's unique identifier
     */
    void refreshListing(Long listingId);
}
