package org.schabi.newpipe.extractor.services.twitch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TwitchParsingHelperTest {
    @Test
    void imageSizePlaceholders() {
        assertEquals("https://static-cdn.jtvnw.net/a/thumb0-640x360.jpg",
                TwitchParsingHelper.getImages(
                        "https://static-cdn.jtvnw.net/a/thumb0-%{width}x%{height}.jpg", 640, 360)
                        .get(0).getUrl());
        assertEquals("https://static-cdn.jtvnw.net/previews-ttv/live_user_x-640x360.jpg",
                TwitchParsingHelper.getImages(
                        "https://static-cdn.jtvnw.net/previews-ttv/live_user_x-{width}x{height}.jpg",
                        640, 360).get(0).getUrl());
    }

    @Test
    void imageOfBroadcastStillProcessing() {
        assertTrue(TwitchParsingHelper.getImages(
                "https://vod-secure.twitch.tv/_404/404_processing_640x360.png", 640, 360).isEmpty());
    }
}
