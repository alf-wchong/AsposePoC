package com.example.pdfform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FieldNamesTest {

    @Test
    void shouldExposeStableFieldNames() {
        assertEquals("Property name", FieldNames.PROPERTY_NAME);
        assertEquals("Property address", FieldNames.PROPERTY_ADDRESS);
        assertEquals("Company name", FieldNames.COMPANY_NAME);
    }
}
