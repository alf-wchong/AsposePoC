package com.example.pdfform;

import java.util.Objects;

public final class FieldInfo {
    private final String fullName;
    private final String javaType;

    public FieldInfo(final String fullName, final String javaType) {
        this.fullName = Objects.requireNonNull(fullName, "fullName must not be null");
        this.javaType = Objects.requireNonNull(javaType, "javaType must not be null");
    }

    public String getFullName() {
        return fullName;
    }

    public String getJavaType() {
        return javaType;
    }

    @Override
    public String toString() {
        return fullName + " :: " + javaType;
    }
}
