package com.example.pdfform;

import picocli.CommandLine;

public final class Main {
    private Main() {
    }

    public static void main(final String[] args) {
        final int exitCode = new CommandLine(new CliOptions()).execute(args);
        System.exit(exitCode);
    }
}
