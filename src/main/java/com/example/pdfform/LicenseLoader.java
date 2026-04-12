package com.example.pdfform;

import com.aspose.pdf.License;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public final class LicenseLoader {
    public static final String ASPOSE_LICENSE_ENV = "ASPOSE_PDF_LICENSE";

    public void loadIfConfigured(final String explicitLicensePath) {
        final Optional<Path> licensePath = resolveLicensePath(explicitLicensePath);
        if (!licensePath.isPresent()) {
            return;
        }

        try {
            final Path path = licensePath.get();
            if (!Files.isReadable(path)) {
                throw new PdfFormCliException(
                        ExitCode.LICENSE_FAILURE,
                        "Aspose license file is not readable: " + path.toAbsolutePath());
            }
            final License license = new License();
            license.setLicense(path.toString());
        } catch (final PdfFormCliException exception) {
            throw exception;
        } catch (final Exception exception) {
            throw new PdfFormCliException(
                    ExitCode.LICENSE_FAILURE,
                    "Failed to load Aspose license: " + exception.getMessage(),
                    exception);
        }
    }

    Optional<Path> resolveLicensePath(final String explicitLicensePath) {
        if (explicitLicensePath != null && !explicitLicensePath.isBlank()) {
            return Optional.of(Paths.get(explicitLicensePath));
        }

        final String envPath = System.getenv(ASPOSE_LICENSE_ENV);
        if (envPath != null && !envPath.isBlank()) {
            return Optional.of(Paths.get(envPath));
        }

        return Optional.empty();
    }
}
