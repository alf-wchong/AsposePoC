package com.example.pdfform;

public final class PdfFormCliException extends RuntimeException {
    private final ExitCode exitCode;

    public PdfFormCliException(final ExitCode exitCode, final String message) {
        super(message);
        this.exitCode = exitCode;
    }

    public PdfFormCliException(final ExitCode exitCode, final String message, final Throwable cause) {
        super(message, cause);
        this.exitCode = exitCode;
    }

    public ExitCode getExitCode() {
        return exitCode;
    }
}
