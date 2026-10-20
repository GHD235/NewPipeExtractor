package org.schabi.newpipe.extractor.services.youtube;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeFeedInfoItemExtractor;

/**
 * VideoPrime: Shorts in the channel feed (RSS) keep the watch URL that the channel tabs use, and
 * are marked as Shorts.
 */
class YoutubeFeedInfoItemExtractorTest {
    private static Element entry(final String id, final String link) {
        // parsed as YoutubeFeedExtractor does
        return Jsoup.parse("<feed><entry><id>yt:video:" + id + "</id>"
                + "<yt:videoId>" + id + "</yt:videoId>"
                + "<title>Title</title>"
                + "<link rel=\"alternate\" href=\"" + link + "\"/>"
                + "<author><name>Channel</name><uri>https://www.youtube.com/channel/UCx</uri></author>"
                + "<published>2026-10-20T10:00:00+00:00</published>"
                + "</entry></feed>").getElementsByTag("entry").first();
    }

    @Test
    void shortKeepsTheWatchUrlAndIsMarked() {
        final YoutubeFeedInfoItemExtractor item = new YoutubeFeedInfoItemExtractor(
                entry("abcDEF12345", "https://www.youtube.com/shorts/abcDEF12345"));
        assertEquals("https://www.youtube.com/watch?v=abcDEF12345", item.getUrl());
        assertTrue(item.isShortFormContent());
    }

    @Test
    void videoIsNotAShort() {
        final YoutubeFeedInfoItemExtractor item = new YoutubeFeedInfoItemExtractor(
                entry("xyz98765432", "https://www.youtube.com/watch?v=xyz98765432"));
        assertEquals("https://www.youtube.com/watch?v=xyz98765432", item.getUrl());
        assertFalse(item.isShortFormContent());
    }
}
