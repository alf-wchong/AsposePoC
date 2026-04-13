package com.example.pdfform;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CliOptionsTest {

    @Test
    void shouldRequireOutputInFillMode() {
        final int exitCode = new CommandLine(new CliOptions()).execute(
                "--input", "form.pdf",
                "--property-name", "Sunset Villas",
                "--property-address", "123 Main Street",
                "--company-name", "Acme Management LLC"
        );

        assertEquals(ExitCode.INVALID_ARGUMENTS.code(), exitCode);
    }

    @Test
    void shouldAllowListModeWithoutOutputOrFieldValues() {
        final int exitCode = new CommandLine(new CliOptions()).execute(
                "--input", "missing.pdf",
                "--list-fields"
        );

        assertEquals(ExitCode.FILE_IO.code(), exitCode);
    }
}
