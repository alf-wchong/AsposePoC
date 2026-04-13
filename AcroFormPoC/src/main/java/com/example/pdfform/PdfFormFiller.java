package com.example.pdfform;

import com.aspose.pdf.Document;
import com.aspose.pdf.Field;
import com.aspose.pdf.Form;
import com.aspose.pdf.TextBoxField;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PdfFormFiller {

    public List<FieldInfo> listFields(final Path inputPdf) {
        Objects.requireNonNull(inputPdf, "inputPdf must not be null");
        validateReadableInput(inputPdf);

        Document document = null;
        try {
            document = new Document(inputPdf.toString());
            final List<FieldInfo> fieldInfos = new ArrayList<>();
            for (final Field field : getLowestLevelFields(document.getForm())) {
                fieldInfos.add(new FieldInfo(field.getFullName(), field.getClass().getSimpleName()));
            }
            return fieldInfos;
        } catch (final PdfFormCliException exception) {
            throw exception;
        } catch (final Exception exception) {
            throw new PdfFormCliException(
                    ExitCode.FILE_IO,
                    "Failed to inspect PDF form: " + inputPdf.toAbsolutePath(),
                    exception);
        } finally {
            closeQuietly(document);
        }
    }

    public void fill(final Path inputPdf,
                     final Path outputPdf,
                     final String propertyName,
                     final String propertyAddress,
                     final String companyName) {
        Objects.requireNonNull(inputPdf, "inputPdf must not be null");
        Objects.requireNonNull(outputPdf, "outputPdf must not be null");
        requireTextValue("property-name", propertyName);
        requireTextValue("property-address", propertyAddress);
        requireTextValue("company-name", companyName);

        validateReadableInput(inputPdf);
        prepareOutputPath(outputPdf);

        Document document = null;
        try {
            document = new Document(inputPdf.toString());
            final Map<String, Field> fieldsByName = buildFieldMap(document.getForm());

            setTextField(fieldsByName, FieldNames.PROPERTY_NAME, propertyName);
            setTextField(fieldsByName, FieldNames.PROPERTY_ADDRESS, propertyAddress);
            setTextField(fieldsByName, FieldNames.COMPANY_NAME, companyName);

            document.save(outputPdf.toString());
        } catch (final PdfFormCliException exception) {
            throw exception;
        } catch (final Exception exception) {
            throw new PdfFormCliException(
                    ExitCode.SAVE_FAILURE,
                    "Failed to fill and save PDF: " + outputPdf.toAbsolutePath(),
                    exception);
        } finally {
            closeQuietly(document);
        }
    }

    static void requireTextValue(final String argumentName, final String value) {
        if (value == null || value.isBlank()) {
            throw new PdfFormCliException(
                    ExitCode.INVALID_ARGUMENTS,
                    "Missing required value for --" + argumentName);
        }
    }

    static Map<String, Field> buildFieldMap(final Form form) {
        final Map<String, Field> fieldsByName = new LinkedHashMap<>();
        for (final Field field : getLowestLevelFields(form)) {
            fieldsByName.putIfAbsent(field.getFullName(), field);
        }
        return fieldsByName;
    }

    private static Field[] getLowestLevelFields(final Form form) {
        final Field[] fields = form.getFields();
        return fields == null ? new Field[0] : fields;
    }

    private static void setTextField(final Map<String, Field> fieldsByName,
                                     final String fieldName,
                                     final String value) {
        final Field field = fieldsByName.get(fieldName);
        if (field == null) {
            throw new PdfFormCliException(
                    ExitCode.MISSING_FIELD,
                    "Missing required field in PDF: " + fieldName);
        }
        if (!(field instanceof TextBoxField)) {
            throw new PdfFormCliException(
                    ExitCode.FIELD_TYPE_MISMATCH,
                    "Field is not a text field: " + fieldName + " (" + field.getClass().getName() + ")");
        }
        ((TextBoxField) field).setValue(value);
    }

    private static void validateReadableInput(final Path inputPdf) {
        if (!Files.isRegularFile(inputPdf) || !Files.isReadable(inputPdf)) {
            throw new PdfFormCliException(
                    ExitCode.FILE_IO,
                    "Input PDF does not exist or is not readable: " + inputPdf.toAbsolutePath());
        }
    }

    private static void prepareOutputPath(final Path outputPdf) {
        final Path parent = outputPdf.toAbsolutePath().getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (final IOException exception) {
            throw new PdfFormCliException(
                    ExitCode.FILE_IO,
                    "Failed to create output directory: " + parent,
                    exception);
        }
    }

    private static void closeQuietly(final Document document) {
        if (document == null) {
            return;
        }
        try {
            document.close();
        } catch (final Exception ignored) {
            // Best-effort cleanup.
        }
    }
}
