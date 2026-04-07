package com.banka1.stock_service.service;

import com.banka1.stock_service.config.FuturesContractSeedProperties;
import com.banka1.stock_service.domain.FuturesContract;
import com.banka1.stock_service.dto.FuturesContractImportResponse;
import com.banka1.stock_service.repository.FuturesContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Imports futures contract reference data from a CSV file and upserts it into the database.
 *
 * <p>The import is idempotent and keyed by the contract ticker.
 * That means:
 *
 * <ul>
 *     <li>if a ticker does not exist yet, a new futures contract is created</li>
 *     <li>if a ticker already exists and at least one imported value changed, the contract is updated</li>
 *     <li>if a ticker already exists and all imported values are the same, the row is counted as unchanged</li>
 * </ul>
 *
 * <p>The importer intentionally accepts only the current production-oriented
 * {@code futures_seed.csv} format so startup seeding and tests stay deterministic.
 *
 * <p>Example:
 * if the CSV contains {@code BRENTNOV26} and the database does not, a new contract is inserted.
 * If {@code BRENTNOV26} already exists with the same values, the row is skipped as unchanged.
 * If only the settlement date changed, the existing row is updated.
 */
@Service
@RequiredArgsConstructor
public class FuturesContractCsvImportService {

    private static final List<String> REQUIRED_HEADERS = List.of(
            "Ticker",
            "Name",
            "Contract Size",
            "Contract Unit",
            "Settlement Date"
    );
    private static final Set<String> ALLOWED_CONTRACT_UNITS = Set.of("Kilogram", "Liter", "Barrel");

    private final FuturesContractRepository futuresContractRepository;
    private final FuturesContractSeedProperties futuresContractSeedProperties;
    private final ResourceLoader resourceLoader;

    /**
     * Imports the configured CSV source from application properties.
     *
     * <p>This is the entry point used by the startup seed flow.
     *
     * @return import summary with created, updated, and unchanged counters
     */
    @Transactional
    public FuturesContractImportResponse importFromConfiguredCsv() {
        return importFromLocation(futuresContractSeedProperties.csvLocation());
    }

    /**
     * Imports futures contracts from the provided Spring resource location.
     *
     * <p>Example locations:
     *
     * <ul>
     *     <li>{@code classpath:seed/futures_seed.csv}</li>
     *     <li>{@code file:./custom/futures_seed.csv}</li>
     * </ul>
     *
     * @param csvLocation Spring resource location, for example {@code classpath:seed/futures_seed.csv}
     * @return import summary
     */
    @Transactional
    public FuturesContractImportResponse importFromLocation(String csvLocation) {
        Resource resource = resourceLoader.getResource(csvLocation);
        return importFromResource(resource, csvLocation);
    }

    /**
     * Imports futures contracts from the provided resource.
     *
     * <p>This method intentionally splits the process into two steps:
     *
     * <ol>
     *     <li>parse and validate CSV rows into an intermediate row model</li>
     *     <li>persist those rows as create/update/unchanged database operations</li>
     * </ol>
     *
     * @param resource CSV resource
     * @param source source label used in the response
     * @return import summary
     */
    @Transactional
    public FuturesContractImportResponse importFromResource(Resource resource, String source) {
        List<FuturesContractCsvRow> rows = parseCsv(resource, source);
        return persistRows(rows, source);
    }

    /**
     * Persists parsed CSV rows into the database using ticker as the stable business key.
     *
     * <p>The method first loads all existing contracts for the imported tickers in one repository call.
     * It then decides row by row whether to:
     *
     * <ul>
     *     <li>create a new entity</li>
     *     <li>update an existing entity</li>
     *     <li>skip persistence because nothing changed</li>
     * </ul>
     *
     * <p>Example:
     * if the CSV contains {@code CORNSEP26} and the database already contains {@code CORNSEP26}
     * with the same name, size, unit, and settlement date, the row is counted as unchanged.
     * If the stored settlement date differs, the row is counted as updated.
     *
     * @param rows validated parsed rows
     * @param source human-readable source label
     * @return import summary
     */
    private FuturesContractImportResponse persistRows(List<FuturesContractCsvRow> rows, String source) {
        Collection<String> tickers = rows.stream()
                .map(FuturesContractCsvRow::ticker)
                .toList();

        Map<String, FuturesContract> existingByTicker = futuresContractRepository.findAllByTickerIn(tickers)
                .stream()
                .collect(Collectors.toMap(FuturesContract::getTicker, Function.identity()));

        List<FuturesContract> entitiesToPersist = new ArrayList<>();
        int createdCount = 0;
        int updatedCount = 0;
        int unchangedCount = 0;

        for (FuturesContractCsvRow row : rows) {
            FuturesContract existingEntity = existingByTicker.get(row.ticker());
            if (existingEntity == null) {
                FuturesContract newEntity = new FuturesContract();
                applyRow(newEntity, row);
                entitiesToPersist.add(newEntity);
                createdCount++;
                continue;
            }

            if (applyRowIfChanged(existingEntity, row)) {
                entitiesToPersist.add(existingEntity);
                updatedCount++;
                continue;
            }

            unchangedCount++;
        }

        if (!entitiesToPersist.isEmpty()) {
            futuresContractRepository.saveAll(entitiesToPersist);
        }

        return new FuturesContractImportResponse(
                source,
                rows.size(),
                createdCount,
                updatedCount,
                unchangedCount
        );
    }

    /**
     * Reads and validates a CSV resource and converts it into intermediate row objects.
     *
     * <p>Validation performed here includes:
     *
     * <ul>
     *     <li>resource existence</li>
     *     <li>non-empty header row</li>
     *     <li>presence of all required futures headers</li>
     *     <li>consistent column count per row</li>
     *     <li>duplicate ticker detection inside the same CSV file</li>
     *     <li>positive integer parsing for {@code Contract Size}</li>
     *     <li>allowed contract-unit validation</li>
     *     <li>ISO date parsing for {@code Settlement Date}</li>
     * </ul>
     *
     * <p>The parser keeps row numbers in error messages so invalid files are easy to debug.
     *
     * @param resource CSV resource to read
     * @param source human-readable source label used in error messages
     * @return parsed CSV rows ready for persistence
     */
    private List<FuturesContractCsvRow> parseCsv(Resource resource, String source) {
        if (!resource.exists()) {
            throw new IllegalStateException("Futures contract CSV resource does not exist: " + source);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
        )) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new IllegalArgumentException("Futures contract CSV is empty: " + source);
            }

            List<String> headerValues = parseCsvLine(headerLine, 1, source);
            Map<String, Integer> headerIndexes = indexHeaders(headerValues, source);
            validateHeaders(headerIndexes, source);

            List<FuturesContractCsvRow> rows = new ArrayList<>();
            Set<String> tickers = new HashSet<>();
            int lineNumber = 1;
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }

                List<String> values = parseCsvLine(line, lineNumber, source);
                if (values.stream().allMatch(String::isBlank)) {
                    continue;
                }

                if (values.size() != headerValues.size()) {
                    throw new IllegalArgumentException(
                            "CSV row " + lineNumber + " in " + source + " has " + values.size()
                                    + " columns, expected " + headerValues.size()
                    );
                }

                FuturesContractCsvRow row = mapRow(values, headerIndexes, lineNumber, source);
                if (!tickers.add(row.ticker())) {
                    throw new IllegalArgumentException(
                            "Duplicate futures ticker '" + row.ticker() + "' found in " + source
                                    + " on row " + lineNumber
                    );
                }
                rows.add(row);
            }

            return rows;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read futures contract CSV resource: " + source, exception);
        }
    }

    /**
     * Builds a header-name to column-index map from the first CSV row.
     *
     * <p>Headers are trimmed before indexing.
     * Duplicate header names are rejected because they would make column resolution ambiguous.
     *
     * @param headers parsed header row values
     * @param source source label used in error messages
     * @return header-index map
     */
    private Map<String, Integer> indexHeaders(List<String> headers, String source) {
        Map<String, Integer> headerIndexes = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String normalizedHeader = headers.get(i).trim();
            if (headerIndexes.putIfAbsent(normalizedHeader, i) != null) {
                throw new IllegalArgumentException("Duplicate CSV header '" + normalizedHeader + "' in " + source);
            }
        }
        return headerIndexes;
    }

    /**
     * Validates that all required business columns from {@code futures_seed.csv} exist.
     *
     * @param headerIndexes indexed CSV headers
     * @param source source label used in error messages
     */
    private void validateHeaders(Map<String, Integer> headerIndexes, String source) {
        for (String requiredHeader : REQUIRED_HEADERS) {
            if (!headerIndexes.containsKey(requiredHeader)) {
                throw new IllegalArgumentException(
                        "Missing required CSV header '" + requiredHeader + "' in " + source
                );
            }
        }
    }

    /**
     * Converts one parsed CSV row into the intermediate row record used by the importer.
     *
     * <p>The resulting record is fully validated and ready to be copied into a JPA entity.
     *
     * @param values row values
     * @param headerIndexes indexed CSV headers
     * @param lineNumber current CSV row number for error reporting
     * @param source source label used in error messages
     * @return parsed row model
     */
    private FuturesContractCsvRow mapRow(
            List<String> values,
            Map<String, Integer> headerIndexes,
            int lineNumber,
            String source
    ) {
        String ticker = requiredValue(values, headerIndexes, "Ticker", lineNumber, source);
        String name = requiredValue(values, headerIndexes, "Name", lineNumber, source);
        int contractSize = parseContractSize(
                requiredValue(values, headerIndexes, "Contract Size", lineNumber, source),
                lineNumber,
                source
        );
        String contractUnit = parseContractUnit(
                requiredValue(values, headerIndexes, "Contract Unit", lineNumber, source),
                lineNumber,
                source
        );
        LocalDate settlementDate = parseSettlementDate(
                requiredValue(values, headerIndexes, "Settlement Date", lineNumber, source),
                lineNumber,
                source
        );

        return new FuturesContractCsvRow(ticker, name, contractSize, contractUnit, settlementDate);
    }

    /**
     * Resolves a required CSV value by exact header name.
     *
     * <p>If the column does not exist or the resolved value is blank, the file is rejected.
     *
     * @param values current row values
     * @param headerIndexes indexed CSV headers
     * @param header exact header name
     * @param lineNumber current row number for error reporting
     * @param source source label used in error messages
     * @return non-blank required value
     */
    private String requiredValue(
            List<String> values,
            Map<String, Integer> headerIndexes,
            String header,
            int lineNumber,
            String source
    ) {
        Integer index = headerIndexes.get(header);
        if (index == null || index >= values.size()) {
            throw new IllegalArgumentException("Missing required CSV header '" + header + "' in " + source);
        }

        String value = values.get(index).trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "Missing value for column '" + header + "' on row " + lineNumber + " in " + source
            );
        }
        return value;
    }

    /**
     * Parses a required positive contract size.
     *
     * <p>Example valid values:
     *
     * <ul>
     *     <li>{@code 1000}</li>
     *     <li>{@code 5000}</li>
     * </ul>
     *
     * <p>Zero and negative values are rejected because a futures contract must represent
     * a positive amount of the underlying unit.
     *
     * @param rawValue raw CSV value
     * @param lineNumber row number used in error messages
     * @param source source label used in error messages
     * @return parsed positive contract size
     */
    private int parseContractSize(String rawValue, int lineNumber, String source) {
        try {
            int contractSize = Integer.parseInt(rawValue);
            if (contractSize <= 0) {
                throw new IllegalArgumentException(
                        "Contract Size must be positive on row " + lineNumber + " in " + source
                );
            }
            return contractSize;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Invalid contract size '" + rawValue + "' on row " + lineNumber + " in " + source,
                    exception
            );
        }
    }

    /**
     * Validates the contract unit against the supported domain values.
     *
     * <p>The issue specification currently allows only:
     *
     * <ul>
     *     <li>{@code Kilogram}</li>
     *     <li>{@code Liter}</li>
     *     <li>{@code Barrel}</li>
     * </ul>
     *
     * @param rawValue raw CSV value
     * @param lineNumber row number used in error messages
     * @param source source label used in error messages
     * @return validated contract unit
     */
    private String parseContractUnit(String rawValue, int lineNumber, String source) {
        if (!ALLOWED_CONTRACT_UNITS.contains(rawValue)) {
            throw new IllegalArgumentException(
                    "Invalid contract unit '" + rawValue + "' on row " + lineNumber + " in " + source
                            + ". Supported values are " + ALLOWED_CONTRACT_UNITS
            );
        }
        return rawValue;
    }

    /**
     * Parses a settlement date using ISO {@code yyyy-MM-dd} format.
     *
     * <p>Example valid value: {@code 2026-11-20}.
     *
     * @param rawValue raw CSV value
     * @param lineNumber row number used in error messages
     * @param source source label used in error messages
     * @return parsed settlement date
     */
    private LocalDate parseSettlementDate(String rawValue, int lineNumber, String source) {
        try {
            return LocalDate.parse(rawValue);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "Invalid settlement date '" + rawValue + "' on row " + lineNumber + " in " + source
                            + ". Expected yyyy-MM-dd format.",
                    exception
            );
        }
    }

    /**
     * Parses one CSV line while respecting quoted values and escaped quotes.
     *
     * <p>Important behavior:
     *
     * <ul>
     *     <li>commas outside quotes split columns</li>
     *     <li>commas inside quotes are preserved as part of the value</li>
     *     <li>double quotes inside a quoted field are unescaped</li>
     *     <li>all returned values are trimmed</li>
     * </ul>
     *
     * <p>Example:
     * the line {@code "Brent, ICE",BRENTNOV26,1000,Barrel,2026-11-20}
     * is parsed so that {@code Brent, ICE} stays a single field.
     *
     * @param line raw CSV line
     * @param lineNumber current CSV row number for error reporting
     * @param source source label used in error messages
     * @return parsed row values
     */
    private List<String> parseCsvLine(String line, int lineNumber, String source) {
        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char currentCharacter = line.charAt(i);

            if (currentCharacter == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    currentValue.append('"');
                    i++;
                    continue;
                }

                inQuotes = !inQuotes;
                continue;
            }

            if (currentCharacter == ',' && !inQuotes) {
                values.add(currentValue.toString().trim());
                currentValue.setLength(0);
                continue;
            }

            currentValue.append(currentCharacter);
        }

        if (inQuotes) {
            throw new IllegalArgumentException("Unclosed quoted CSV value on row " + lineNumber + " in " + source);
        }

        values.add(currentValue.toString().trim());
        return values;
    }

    /**
     * Copies parsed row values into a persistent entity.
     *
     * <p>This method performs the raw field assignment only.
     * It is used both when creating a brand-new contract and when updating an existing one.
     *
     * @param entity target entity
     * @param row parsed CSV row
     */
    private void applyRow(FuturesContract entity, FuturesContractCsvRow row) {
        entity.setTicker(row.ticker());
        entity.setName(row.name());
        entity.setContractSize(row.contractSize());
        entity.setContractUnit(row.contractUnit());
        entity.setSettlementDate(row.settlementDate());
    }

    /**
     * Updates an existing entity only when at least one imported value changed.
     *
     * <p>This is what allows the import summary to distinguish between updated rows and unchanged rows.
     *
     * @param entity existing entity from the database
     * @param row parsed CSV row
     * @return {@code true} if the entity was changed and should be persisted
     */
    private boolean applyRowIfChanged(FuturesContract entity, FuturesContractCsvRow row) {
        if (matches(entity, row)) {
            return false;
        }

        applyRow(entity, row);
        return true;
    }

    /**
     * Compares all imported business fields between the existing entity and the parsed row.
     *
     * <p>If this method returns {@code true}, the importer treats the row as unchanged and skips persistence.
     *
     * @param entity existing entity from the database
     * @param row parsed CSV row
     * @return {@code true} when all imported fields already match
     */
    private boolean matches(FuturesContract entity, FuturesContractCsvRow row) {
        return Objects.equals(entity.getTicker(), row.ticker())
                && Objects.equals(entity.getName(), row.name())
                && Objects.equals(entity.getContractSize(), row.contractSize())
                && Objects.equals(entity.getContractUnit(), row.contractUnit())
                && Objects.equals(entity.getSettlementDate(), row.settlementDate());
    }

    /**
     * Intermediate immutable representation of one validated futures contract CSV row.
     *
     * <p>This record exists so parsing and validation stay separated from persistence.
     * The importer first converts the file into a clean row model and only then applies it to JPA entities.
     */
    private record FuturesContractCsvRow(
            String ticker,
            String name,
            Integer contractSize,
            String contractUnit,
            LocalDate settlementDate
    ) {
    }
}
