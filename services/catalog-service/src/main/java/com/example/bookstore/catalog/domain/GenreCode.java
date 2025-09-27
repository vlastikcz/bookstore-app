package com.example.bookstore.catalog.domain;

public enum GenreCode {
    FICTION("Fiction"),
    NON_FICTION("Non-fiction"),
    MYSTERY("Mystery"),
    SCIENCE_FICTION("Science Fiction"),
    FANTASY("Fantasy"),
    BIOGRAPHY("Biography"),
    HISTORY("History"),
    CHILDREN("Children"),
    ROMANCE("Romance"),
    SELF_HELP("Self-help");

    private final String displayName;

    GenreCode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
