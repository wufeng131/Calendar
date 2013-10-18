package com.android.calendar;

import android.content.SearchRecentSuggestionsProvider;

public class SearchSuggestionSampleProvider extends SearchRecentSuggestionsProvider{
    public final static String AUTHORITY = SearchSuggestionSampleProvider.class.getName();
    public final static int MODE = DATABASE_MODE_QUERIES;

    public SearchSuggestionSampleProvider() {
        super();
        setupSuggestions(AUTHORITY, MODE);
        }
    }