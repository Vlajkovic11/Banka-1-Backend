package com.banka1.stock_service.service;

import com.banka1.stock_service.config.FuturesContractSeedProperties;
import com.banka1.stock_service.domain.FuturesContract;
import com.banka1.stock_service.dto.FuturesContractImportResponse;
import com.banka1.stock_service.repository.FuturesContractRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.DefaultResourceLoader;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FuturesContractCsvImportService}.
 */
@ExtendWith(MockitoExtension.class)
class FuturesContractCsvImportServiceTest {

    @Mock
    private FuturesContractRepository futuresContractRepository;

    @Test
    void importFromResourceCreatesNewContractsFromCsv() {
        FuturesContractCsvImportService service = createService("classpath:seed/futures_seed.csv");
        when(futuresContractRepository.findAllByTickerIn(any())).thenReturn(List.of());
        when(futuresContractRepository.saveAll(any())).thenAnswer(invocation -> List.of());

        FuturesContractImportResponse response = service.importFromResource(
                csvResource("""
                        Ticker,Name,Contract Size,Contract Unit,Settlement Date
                        CORNSEP26,Corn Futures September 2026,5000,Kilogram,2026-09-15
                        BRENTNOV26,Brent Oil Futures November 2026,1000,Barrel,2026-11-20
                        """),
                "test-futures.csv"
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FuturesContract>> captor = ArgumentCaptor.forClass(List.class);
        verify(futuresContractRepository).saveAll(captor.capture());
        List<FuturesContract> savedEntities = captor.getValue();

        assertThat(response.processedRows()).isEqualTo(2);
        assertThat(response.createdCount()).isEqualTo(2);
        assertThat(response.updatedCount()).isZero();
        assertThat(response.unchangedCount()).isZero();
        assertThat(savedEntities).hasSize(2);
        assertThat(savedEntities.getFirst().getTicker()).isEqualTo("CORNSEP26");
        assertThat(savedEntities.getFirst().getContractSize()).isEqualTo(5_000);
        assertThat(savedEntities.getFirst().getContractUnit()).isEqualTo("Kilogram");
        assertThat(savedEntities.getFirst().getSettlementDate()).isEqualTo(LocalDate.of(2026, 9, 15));
    }

    @Test
    void importFromConfiguredCsvLoadsDummySeedFileFromResources() {
        FuturesContractCsvImportService service = createService("classpath:seed/futures_seed.csv");
        when(futuresContractRepository.findAllByTickerIn(any())).thenReturn(List.of());
        when(futuresContractRepository.saveAll(any())).thenAnswer(invocation -> List.of());

        FuturesContractImportResponse response = service.importFromConfiguredCsv();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FuturesContract>> captor = ArgumentCaptor.forClass(List.class);
        verify(futuresContractRepository).saveAll(captor.capture());
        List<FuturesContract> savedEntities = captor.getValue();

        assertThat(response.source()).isEqualTo("classpath:seed/futures_seed.csv");
        assertThat(response.processedRows()).isEqualTo(3);
        assertThat(savedEntities).extracting(FuturesContract::getTicker)
                .containsExactly("CORNSEP26", "BRENTNOV26", "MILKAUG26");
    }

    @Test
    void importFromResourceSkipsUnchangedContractOnRepeatedImport() {
        FuturesContract existingContract = new FuturesContract();
        existingContract.setTicker("BRENTNOV26");
        existingContract.setName("Brent Oil Futures November 2026");
        existingContract.setContractSize(1_000);
        existingContract.setContractUnit("Barrel");
        existingContract.setSettlementDate(LocalDate.of(2026, 11, 20));

        FuturesContractCsvImportService service = createService("classpath:seed/futures_seed.csv");
        when(futuresContractRepository.findAllByTickerIn(any())).thenReturn(List.of(existingContract));

        FuturesContractImportResponse response = service.importFromResource(
                csvResource("""
                        Ticker,Name,Contract Size,Contract Unit,Settlement Date
                        BRENTNOV26,Brent Oil Futures November 2026,1000,Barrel,2026-11-20
                        """),
                "test-futures.csv"
        );

        assertThat(response.processedRows()).isEqualTo(1);
        assertThat(response.createdCount()).isZero();
        assertThat(response.updatedCount()).isZero();
        assertThat(response.unchangedCount()).isEqualTo(1);
        verify(futuresContractRepository, never()).saveAll(any());
    }

    @Test
    void importFromResourceRejectsDuplicateTickersInsideCsv() {
        FuturesContractCsvImportService service = createService("classpath:seed/futures_seed.csv");

        assertThatThrownBy(() -> service.importFromResource(
                csvResource("""
                        Ticker,Name,Contract Size,Contract Unit,Settlement Date
                        CORNSEP26,Corn Futures September 2026,5000,Kilogram,2026-09-15
                        CORNSEP26,Duplicate Corn Futures,6000,Kilogram,2026-10-01
                        """),
                "test-futures.csv"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate futures ticker 'CORNSEP26'");
    }

    private FuturesContractCsvImportService createService(String csvLocation) {
        return new FuturesContractCsvImportService(
                futuresContractRepository,
                new FuturesContractSeedProperties(true, csvLocation),
                new DefaultResourceLoader()
        );
    }

    private ByteArrayResource csvResource(String content) {
        return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
    }
}
