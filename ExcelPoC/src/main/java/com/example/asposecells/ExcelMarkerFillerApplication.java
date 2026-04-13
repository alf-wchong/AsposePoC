package com.example.asposecells;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import com.aspose.cells.Cell;
import com.aspose.cells.Cells;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.example.asposecells.model.FieldMapping;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ExcelMarkerFillerApplication {
    private static final Pattern MARKER_PATTERN = Pattern.compile("^\\[\\{!(.+?)!}]$");
    private static final String DEFAULT_LOG_FILE = "target/excel-marker-filler.log";
    private static Logger log = LoggerFactory.getLogger(ExcelMarkerFillerApplication.class);

    public static void main(String[] args) {
        int exitCode = 1;
        Instant startedAt = Instant.now();
        try {
            CommandLine commandLine = parseArgs(args);
            configureLogging(commandLine.getOptionValue("log-file", DEFAULT_LOG_FILE));

            Path inputPath = Path.of(commandLine.getOptionValue("input")).toAbsolutePath().normalize();
            Path configPath = Path.of(commandLine.getOptionValue("config")).toAbsolutePath().normalize();
            Path outputPath = Path.of(commandLine.getOptionValue("output")).toAbsolutePath().normalize();
            boolean scanAllSheets = commandLine.hasOption("scan-all-sheets");

            log.info("event=run_start input=\"{}\" config=\"{}\" output=\"{}\" scanAllSheets={}",
                inputPath, configPath, outputPath, scanAllSheets);

            validateReadableFile(inputPath, "Excel input workbook");
            validateReadableFile(configPath, "JSON config file");
            createParentDirectory(outputPath);

            List<FieldMapping> mappings = loadMappings(configPath);
            validateMappings(mappings);

            Workbook workbook = new Workbook(inputPath.toString());
            List<Worksheet> worksheets = resolveWorksheets(workbook, scanAllSheets);
            DiscoveryResult discoveryResult = findMarkerCells(worksheets);

            if (discoveryResult.markerCells.isEmpty()) {
                throw new IllegalStateException("No marker cells matching the pattern [{!...!}] were found in the targeted workbook scope.");
            }

            checkForAmbiguousMatches(discoveryResult.markerCells, mappings);
            FillResult fillResult = fillMarkers(discoveryResult.markerCells, mappings);

            workbook.save(outputPath.toString());
            log.info("event=workbook_saved output=\"{}\"", outputPath);
            log.info("event=run_complete status=success elapsedMs={} sheetsScanned={} markersDiscovered={} mappingsLoaded={} mappingsMatched={} mappingsUnmatched={} cellsReplaced={}",
                Duration.between(startedAt, Instant.now()).toMillis(),
                discoveryResult.sheetsScanned,
                discoveryResult.markersDiscovered,
                mappings.size(),
                fillResult.mappingsMatched,
                fillResult.mappingsUnmatched,
                fillResult.cellsReplaced);
            exitCode = 0;
        } catch (Exception exception) {
            log.error("event=run_failed status=failed errorType=unhandled_exception message=\"{}\"", exception.getMessage(), exception);
            System.err.println("ERROR: " + exception.getMessage());
            printUsage();
        }

        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    private static CommandLine parseArgs(String[] args) throws ParseException {
        Options options = buildOptions();
        try {
            return new DefaultParser().parse(options, args);
        } catch (ParseException ex) {
            throw new IllegalArgumentException("Invalid command-line arguments. Use --input --config --output [--log-file] [--scan-all-sheets]", ex);
        }
    }

    private static void configureLogging(String logFilePath) throws JoranException, IOException {
        System.setProperty("APP_LOG_FILE", Path.of(logFilePath).toAbsolutePath().normalize().toString());
        Path logParent = Path.of(logFilePath).toAbsolutePath().normalize().getParent();
        if (logParent != null) {
            Files.createDirectories(logParent);
        }
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        configurator.doConfigure(ExcelMarkerFillerApplication.class.getClassLoader().getResource("logback.xml"));
        log = LoggerFactory.getLogger(ExcelMarkerFillerApplication.class);
    }

    private static Options buildOptions() {
        Options options = new Options();
        options.addOption(Option.builder().longOpt("input").hasArg().required().desc("Path to input Excel workbook").build());
        options.addOption(Option.builder().longOpt("config").hasArg().required().desc("Path to input JSON mapping file").build());
        options.addOption(Option.builder().longOpt("output").hasArg().required().desc("Path to output Excel workbook").build());
        options.addOption(Option.builder().longOpt("log-file").hasArg().desc("Path to rolling application log file").build());
        options.addOption(Option.builder().longOpt("scan-all-sheets").desc("Scan all worksheets instead of only the first worksheet").build());
        return options;
    }

    private static void printUsage() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar aspose-cells-marker-filler.jar", buildOptions());
    }

    private static void validateReadableFile(Path path, String label) {
        if (!Files.exists(path) || !Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IllegalArgumentException(label + " is not readable: " + path);
        }
    }

    private static void createParentDirectory(Path outputPath) throws IOException {
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static List<FieldMapping> loadMappings(Path configPath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        List<FieldMapping> mappings = objectMapper.readValue(configPath.toFile(), new TypeReference<>() {});
        log.info("event=mappings_loaded mappingsLoaded={} config=\"{}\"", mappings.size(), configPath);
        for (int i = 0; i < mappings.size(); i++) {
            FieldMapping mapping = mappings.get(i);
            log.debug("event=mapping_loaded mappingIndex={} key=\"{}\" aliases={} replaceAll={} caseSensitive={}",
                i,
                mapping.getKey(),
                mapping.getAliases(),
                mapping.isReplaceAll(),
                mapping.isCaseSensitive());
        }
        return mappings;
    }

    private static void validateMappings(List<FieldMapping> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            throw new IllegalArgumentException("JSON config must contain at least one mapping object.");
        }

        Set<String> normalizedNames = new LinkedHashSet<>();
        for (int index = 0; index < mappings.size(); index++) {
            FieldMapping mapping = mappings.get(index);
            if (mapping.getKey() == null || mapping.getKey().isBlank()) {
                throw new IllegalArgumentException("Mapping at index " + index + " is missing a non-blank key.");
            }
            if (mapping.getValue() == null) {
                throw new IllegalArgumentException("Mapping at index " + index + " is missing value.");
            }
            List<String> names = mapping.effectiveNames();
            if (names.isEmpty()) {
                throw new IllegalArgumentException("Mapping at index " + index + " must contain a key and/or aliases.");
            }
            for (String name : names) {
                String normalized = mapping.isCaseSensitive() ? "CS::" + name : "CI::" + name.toLowerCase(Locale.ROOT);
                if (!normalizedNames.add(normalized)) {
                    log.debug("event=duplicate_searchable_name mappingIndex={} searchableName=\"{}\"", index, normalized);
                }
            }
        }
    }

    private static List<Worksheet> resolveWorksheets(Workbook workbook, boolean scanAllSheets) {
        List<Worksheet> worksheets = new ArrayList<>();
        if (scanAllSheets) {
            for (int i = 0; i < workbook.getWorksheets().getCount(); i++) {
                worksheets.add(workbook.getWorksheets().get(i));
            }
        } else {
            worksheets.add(workbook.getWorksheets().get(0));
        }
        return worksheets;
    }

    private static DiscoveryResult findMarkerCells(List<Worksheet> worksheets) {
        List<MarkerCell> markerCells = new ArrayList<>();
        for (Worksheet worksheet : worksheets) {
            Cells cells = worksheet.getCells();
            int maxDataRow = cells.getMaxDataRow();
            int maxDataColumn = cells.getMaxDataColumn();
            log.debug("event=worksheet_scan_start sheet=\"{}\" maxDataRow={} maxDataColumn={}",
                worksheet.getName(), maxDataRow, maxDataColumn);
            for (int row = 0; row <= maxDataRow; row++) {
                for (int col = 0; col <= maxDataColumn; col++) {
                    Cell cell = cells.get(row, col);
                    Object value = cell.getValue();
                    if (!(value instanceof String text)) {
                        continue;
                    }
                    Matcher matcher = MARKER_PATTERN.matcher(text.trim());
                    if (matcher.matches()) {
                        String markerName = matcher.group(1).trim();
                        MarkerCell markerCell = new MarkerCell(worksheet.getName(), cell.getName(), markerName, text, cell);
                        markerCells.add(markerCell);
                        log.debug("event=marker_discovered sheet=\"{}\" cell=\"{}\" markerRaw=\"{}\" markerName=\"{}\"",
                            markerCell.sheetName, markerCell.cellName, markerCell.rawCellValue, markerCell.markerName);
                    }
                }
            }
        }

        DiscoveryResult discoveryResult = new DiscoveryResult();
        discoveryResult.markerCells = markerCells;
        discoveryResult.sheetsConsidered = worksheets.size();
        discoveryResult.sheetsScanned = worksheets.size();
        discoveryResult.markersDiscovered = markerCells.size();
        log.info("event=marker_discovery_complete sheetsConsidered={} sheetsScanned={} markersDiscovered={}",
            discoveryResult.sheetsConsidered, discoveryResult.sheetsScanned, discoveryResult.markersDiscovered);
        return discoveryResult;
    }

    private static void checkForAmbiguousMatches(List<MarkerCell> markerCells, List<FieldMapping> mappings) {
        for (MarkerCell markerCell : markerCells) {
            List<MatchedMapping> matchedMappings = new ArrayList<>();
            for (int i = 0; i < mappings.size(); i++) {
                FieldMapping mapping = mappings.get(i);
                if (matches(markerCell.markerName, mapping)) {
                    matchedMappings.add(new MatchedMapping(i, mapping));
                }
            }
            if (matchedMappings.size() > 1) {
                String matchingMappings = matchedMappings.stream()
                    .map(match -> String.format("{mappingIndex=%d,key=\"%s\",aliases=%s}",
                        match.mappingIndex,
                        safe(match.mapping.getKey()),
                        match.mapping.getAliases()))
                    .collect(Collectors.joining(", "));
                String message = String.format(
                    "event=validation_failed errorType=ambiguous_marker_match status=failed sheet=\"%s\" cell=\"%s\" markerRaw=\"%s\" markerName=\"%s\" matchingMappings=[%s] message=\"Marker matched more than one JSON entry\"",
                    markerCell.sheetName,
                    markerCell.cellName,
                    markerCell.rawCellValue,
                    markerCell.markerName,
                    matchingMappings);
                log.error(message);
                throw new IllegalStateException("Marker matched more than one JSON entry. See error log for sheet, cell, marker, and competing mappings.");
            }
        }
        log.info("event=validation_complete ambiguousMarkers=0 ambiguousMappings=0 validationStatus=passed");
    }

    private static FillResult fillMarkers(List<MarkerCell> markerCells, List<FieldMapping> mappings) {
        FillResult result = new FillResult();
        result.mappingsEvaluated = mappings.size();

        for (int i = 0; i < mappings.size(); i++) {
            FieldMapping mapping = mappings.get(i);
            int matchedMarkersForMapping = 0;
            int replacedCellsForMapping = 0;
            List<String> matchedCells = new ArrayList<>();

            log.debug("event=mapping_evaluation_start mappingIndex={} key=\"{}\" aliases={} replaceAll={} caseSensitive={}",
                i, mapping.getKey(), mapping.getAliases(), mapping.isReplaceAll(), mapping.isCaseSensitive());

            for (MarkerCell markerCell : markerCells) {
                if (markerCell.replaced || !matches(markerCell.markerName, mapping)) {
                    continue;
                }

                matchedMarkersForMapping++;
                matchedCells.add(markerCell.sheetName + "!" + markerCell.cellName);
                markerCell.cell.putValue(mapping.getValue());
                markerCell.replaced = true;
                replacedCellsForMapping++;
                result.cellsReplaced++;

                log.debug("event=cell_replaced mappingIndex={} key=\"{}\" sheet=\"{}\" cell=\"{}\" markerName=\"{}\" value=\"{}\"",
                    i, mapping.getKey(), markerCell.sheetName, markerCell.cellName, markerCell.markerName, mapping.getValue());

                if (!mapping.isReplaceAll()) {
                    break;
                }
            }

            if (!matchedCells.isEmpty()) {
                result.mappingsMatched++;
            } else {
                result.mappingsUnmatched++;
            }
            if (!mapping.isReplaceAll()) {
                result.replaceAllFalseMappings++;
            }
            result.markersMatched += matchedMarkersForMapping;

            log.debug("event=mapping_result mappingIndex={} key=\"{}\" matchedMarkers={} replacedCells={} matchedCells={}",
                i, mapping.getKey(), matchedMarkersForMapping, replacedCellsForMapping, matchedCells);
        }

        result.untouchedMarkers = (int) markerCells.stream().filter(markerCell -> !markerCell.replaced).count();
        log.info("event=replacement_complete mappingsEvaluated={} mappingsMatched={} mappingsUnmatched={} replaceAllFalseMappings={} markersMatched={} cellsReplaced={}",
            result.mappingsEvaluated,
            result.mappingsMatched,
            result.mappingsUnmatched,
            result.replaceAllFalseMappings,
            result.markersMatched,
            result.cellsReplaced);
        return result;
    }

    private static boolean matches(String markerName, FieldMapping mapping) {
        for (String candidate : mapping.effectiveNames()) {
            if (mapping.isCaseSensitive()) {
                if (markerName.equals(candidate)) {
                    return true;
                }
            } else if (markerName.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace("\"", "\\\"");
    }

    private static final class MarkerCell {
        private final String sheetName;
        private final String cellName;
        private final String markerName;
        private final String rawCellValue;
        private final Cell cell;
        private boolean replaced;

        private MarkerCell(String sheetName, String cellName, String markerName, String rawCellValue, Cell cell) {
            this.sheetName = sheetName;
            this.cellName = cellName;
            this.markerName = markerName;
            this.rawCellValue = rawCellValue;
            this.cell = cell;
            this.replaced = false;
        }
    }

    private static final class MatchedMapping {
        private final int mappingIndex;
        private final FieldMapping mapping;

        private MatchedMapping(int mappingIndex, FieldMapping mapping) {
            this.mappingIndex = mappingIndex;
            this.mapping = mapping;
        }
    }

    private static final class DiscoveryResult {
        private List<MarkerCell> markerCells = new ArrayList<>();
        private int sheetsConsidered;
        private int sheetsScanned;
        private int markersDiscovered;
    }

    private static final class FillResult {
        private int mappingsEvaluated;
        private int mappingsMatched;
        private int mappingsUnmatched;
        private int replaceAllFalseMappings;
        private int markersMatched;
        private int cellsReplaced;
        private int untouchedMarkers;
    }
}
