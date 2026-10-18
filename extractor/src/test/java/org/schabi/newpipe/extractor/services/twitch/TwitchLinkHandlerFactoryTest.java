package org.schabi.newpipe.extractor.services.twitch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.InitNewPipeTest;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.services.twitch.linkHandler.TwitchChannelLinkHandlerFactory;
import org.schabi.newpipe.extractor.services.twitch.linkHandler.TwitchStreamLinkHandlerFactory;

public class TwitchLinkHandlerFactoryTest {
    private static TwitchStreamLinkHandlerFactory streamLHF;
    private static TwitchChannelLinkHandlerFactory channelLHF;

    @BeforeAll
    public static void setUp() {
        InitNewPipeTest.initEmpty();
        streamLHF = TwitchStreamLinkHandlerFactory.getInstance();
        channelLHF = TwitchChannelLinkHandlerFactory.getInstance();
    }

    @Test
    void streamIds() throws ParsingException {
        assertEquals("ibai", streamLHF.fromUrl("https://www.twitch.tv/ibai").getId());
        assertEquals("ibai", streamLHF.fromUrl("https://twitch.tv/Ibai/").getId());
        assertEquals("ibai", streamLHF.fromUrl("https://m.twitch.tv/ibai?sr=a").getId());
        assertEquals("videos/2903404377",
                streamLHF.fromUrl("https://www.twitch.tv/videos/2903404377").getId());
        assertEquals("videos/2903404377",
                streamLHF.fromUrl("https://www.twitch.tv/videos/2903404377?t=1h2m").getId());
        assertEquals("videos/2903404377",
                streamLHF.fromUrl("https://www.twitch.tv/ibai/v/2903404377").getId());
    }

    @Test
    void streamUrls() throws ParsingException {
        assertEquals("https://www.twitch.tv/ibai", streamLHF.fromId("ibai").getUrl());
        assertEquals("https://www.twitch.tv/videos/123",
                streamLHF.fromId("videos/123").getUrl());
    }

    @Test
    void streamRejects() throws ParsingException {
        assertFalse(streamLHF.acceptUrl("https://www.twitch.tv/directory"));
        assertFalse(streamLHF.acceptUrl("https://www.twitch.tv/ibai/videos"));
        assertFalse(streamLHF.acceptUrl("https://www.youtube.com/ibai"));
    }

    @Test
    void channels() throws ParsingException {
        assertEquals("ibai", channelLHF.fromUrl("https://www.twitch.tv/ibai/videos").getId());
        assertEquals("ibai", channelLHF.fromUrl("https://www.twitch.tv/Ibai/about").getId());
        assertEquals("https://www.twitch.tv/ibai/videos", channelLHF.fromId("ibai").getUrl());
        assertTrue(channelLHF.acceptUrl("https://www.twitch.tv/ibai/schedule"));
        assertFalse(channelLHF.acceptUrl("https://www.twitch.tv/ibai"));
    }
}
