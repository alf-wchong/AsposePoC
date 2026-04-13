package com.example.asposecells.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldMapping {
    private String key;
    private String value;
    private List<String> aliases = new ArrayList<>();

    @JsonProperty("ReplaceAll")
    private Boolean replaceAll;

    @JsonProperty("CaseSensitivity")
    private Boolean caseSensitivity;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases == null ? new ArrayList<>() : aliases;
    }

    public Boolean getReplaceAll() {
        return replaceAll;
    }

    public void setReplaceAll(Boolean replaceAll) {
        this.replaceAll = replaceAll;
    }

    public Boolean getCaseSensitivity() {
        return caseSensitivity;
    }

    public void setCaseSensitivity(Boolean caseSensitivity) {
        this.caseSensitivity = caseSensitivity;
    }

    public boolean isReplaceAll() {
        return replaceAll == null || replaceAll;
    }

    public boolean isCaseSensitive() {
        return caseSensitivity != null && caseSensitivity;
    }

    public List<String> effectiveNames() {
        List<String> names = new ArrayList<>();
        if (key != null && !key.isBlank()) {
            names.add(key.trim());
        }
        if (aliases != null) {
            aliases.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .forEach(names::add);
        }
        return names;
    }

    @Override
    public String toString() {
        return "FieldMapping{" +
            "key='" + key + '\'' +
            ", value='" + value + '\'' +
            ", aliases=" + aliases +
            ", ReplaceAll=" + isReplaceAll() +
            ", CaseSensitivity=" + isCaseSensitive() +
            '}';
    }
}
