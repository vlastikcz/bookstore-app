package com.example.bookstore.catalog.book.domain;

public enum BookGenre {
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

    BookGenre(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
