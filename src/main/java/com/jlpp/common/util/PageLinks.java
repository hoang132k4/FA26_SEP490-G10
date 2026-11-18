package com.jlpp.common.util;

import org.springframework.web.util.UriComponentsBuilder;

public final class PageLinks {

    private PageLinks() {
    }

    /** List URL keeping the non-blank filters, for fragments/components/pagination to append page=. */
    public static String of(String path, String... filterNameValuePairs) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path);
        for (int i = 0; i + 1 < filterNameValuePairs.length; i += 2) {
            String value = filterNameValuePairs[i + 1];
            if (value != null && !value.isBlank()) builder.queryParam(filterNameValuePairs[i], value);
        }
        return builder.encode().build().toUriString();
    }
}
