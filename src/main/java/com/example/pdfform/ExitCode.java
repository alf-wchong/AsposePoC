package com.example.pdfform;

public enum ExitCode {
    SUCCESS(0),
    INVALID_ARGUMENTS(2),
    FILE_IO(3),
    MISSING_FIELD(4),
    FIELD_TYPE_MISMATCH(5),
    SAVE_FAILURE(6),
    LICENSE_FAILURE(7),
    UNEXPECTED_ERROR(10);

    private final int code;

    ExitCode(final int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
