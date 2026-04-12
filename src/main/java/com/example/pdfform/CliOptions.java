package com.example.pdfform;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "pdf-form-cli",
        mixinStandardHelpOptions = true,
        version = "pdf-form-cli 1.0.0",
        description = "Fill selected AcroForm fields in a PDF using Aspose.PDF for Java.")
public final class CliOptions implements Callable<Integer> {

    @Spec
    private CommandSpec spec;

    @Option(names = "--input", required = true, description = "Source PDF form")
    private Path input;

    @Option(names = "--output", description = "Output PDF path")
    private Path output;

    @Option(names = "--property-name", description = "Value for the Property name field")
    private String propertyName;

    @Option(names = "--property-address", description = "Value for the Property address field")
    private String propertyAddress;

    @Option(names = "--company-name", description = "Value for the Company name field")
    private String companyName;

    @Option(names = "--list-fields", description = "List all detected AcroForm fields and exit")
    private boolean listFields;

    @Option(names = "--license", description = "Optional Aspose license file")
    private String licensePath;

    @Option(names = "--verbose", description = "Print stack traces on failure")
    private boolean verbose;

    @Override
    public Integer call() {
        try {
            new LicenseLoader().loadIfConfigured(licensePath);
            final PdfFormFiller filler = new PdfFormFiller();

            if (listFields) {
                final List<FieldInfo> fields = filler.listFields(input);
                fields.stream()
                        .sorted(Comparator.comparing(FieldInfo::getFullName, String.CASE_INSENSITIVE_ORDER))
                        .forEach(field -> spec.commandLine().getOut().println(field));
                return ExitCode.SUCCESS.code();
            }

            validateFillMode();
            filler.fill(input, output, propertyName, propertyAddress, companyName);
            spec.commandLine().getOut().println("Filled PDF written to: " + output.toAbsolutePath());
            return ExitCode.SUCCESS.code();
        } catch (final PdfFormCliException exception) {
            printFailure(exception);
            return exception.getExitCode().code();
        } catch (final Exception exception) {
            printFailure(new PdfFormCliException(
                    ExitCode.UNEXPECTED_ERROR,
                    "Unexpected error: " + exception.getMessage(),
                    exception));
            return ExitCode.UNEXPECTED_ERROR.code();
        }
    }

    private void validateFillMode() {
        if (output == null) {
            throw new PdfFormCliException(ExitCode.INVALID_ARGUMENTS, "Missing required option: --output");
        }
        PdfFormFiller.requireTextValue("property-name", propertyName);
        PdfFormFiller.requireTextValue("property-address", propertyAddress);
        PdfFormFiller.requireTextValue("company-name", companyName);
    }

    private void printFailure(final PdfFormCliException exception) {
        final PrintWriter err = spec.commandLine().getErr();
        err.println("ERROR: " + exception.getMessage());
        if (verbose && exception.getCause() != null) {
            exception.printStackTrace(err);
        }
        err.flush();
    }
}
