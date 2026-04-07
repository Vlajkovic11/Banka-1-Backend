package com.banka1.stock_service.service;

import com.banka1.stock_service.config.FuturesContractSeedProperties;
import com.banka1.stock_service.config.StockExchangeSeedProperties;
import com.banka1.stock_service.dto.FuturesContractImportResponse;
import com.banka1.stock_service.dto.StockExchangeImportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Startup runner that seeds reference data from the configured CSV files.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeedRunner implements ApplicationRunner {

    private final StockExchangeCsvImportService stockExchangeCsvImportService;
    private final FuturesContractCsvImportService futuresContractCsvImportService;
    private final StockExchangeSeedProperties stockExchangeSeedProperties;
    private final FuturesContractSeedProperties futuresContractSeedProperties;

    /**
     * Imports stock exchanges and futures contracts on startup when their seed features are enabled.
     *
     * @param args application startup arguments
     */
    @Override
    public void run(ApplicationArguments args) {
        if (stockExchangeSeedProperties.enabled()) {
            StockExchangeImportResponse importResponse = stockExchangeCsvImportService.importFromConfiguredCsv();
            log.info(
                    "Stock exchanges imported from {}. processedRows={}, createdCount={}, updatedCount={}, unchangedCount={}",
                    importResponse.source(),
                    importResponse.processedRows(),
                    importResponse.createdCount(),
                    importResponse.updatedCount(),
                    importResponse.unchangedCount()
            );
        } else {
            log.info("Stock exchange CSV seeding is disabled.");
        }

        if (futuresContractSeedProperties.enabled()) {
            FuturesContractImportResponse importResponse = futuresContractCsvImportService.importFromConfiguredCsv();
            log.info(
                    "Futures contracts imported from {}. processedRows={}, createdCount={}, updatedCount={}, unchangedCount={}",
                    importResponse.source(),
                    importResponse.processedRows(),
                    importResponse.createdCount(),
                    importResponse.updatedCount(),
                    importResponse.unchangedCount()
            );
        } else {
            log.info("Futures contract CSV seeding is disabled.");
        }
    }
}
