package org.schabi.newpipe.extractor.services.twitch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.schabi.newpipe.extractor.ServiceList.Twitch;

import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.InitNewPipeTest;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.channel.ChannelExtractor;
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor;
import org.schabi.newpipe.extractor.kiosk.KioskExtractor;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamType;

import java.util.List;

/**
 * End-to-end checks of the Twitch service. Live content changes all the time, so the tests pick
 * whatever is live or recent instead of hardcoding streams.
 */
public class TwitchExtractorsTest implements InitNewPipeTest {
    private static final String CHANNEL = "ibai";

    @Test
    void liveKioskAndLiveStream() throws Exception {
        final KioskExtractor<?> kiosk = Twitch.getKioskList().getDefaultKioskExtractor();
        kiosk.fetchPage();
        final ListExtractor.InfoItemsPage<?> page = kiosk.getInitialPage();
        assertFalse(page.getItems().isEmpty(), "no live channels");
        assertTrue(page.hasNextPage());

        final StreamInfoItem first = (StreamInfoItem) page.getItems().get(0);
        assertEquals(StreamType.LIVE_STREAM, first.getStreamType());
        assertTrue(first.getViewCount() > 0);

        final StreamExtractor stream = Twitch.getStreamExtractor(first.getUrl());
        stream.fetchPage();
        assertEquals(StreamType.LIVE_STREAM, stream.getStreamType());
        assertFalse(stream.getName().isEmpty());
        assertFalse(stream.getUploaderName().isEmpty());
        assertTrue(stream.getHlsUrl().startsWith("https://usher.ttvnw.net/"));
        assertFalse(stream.getVideoStreams().isEmpty());
        assertFalse(stream.getAudioStreams().isEmpty());
    }

    @Test
    void channelAndVideo() throws Exception {
        final ChannelExtractor channel = Twitch.getChannelExtractor(
                "https://www.twitch.tv/" + CHANNEL + "/videos");
        channel.fetchPage();
        assertEquals(CHANNEL, channel.getName().toLowerCase());
        assertTrue(channel.getSubscriberCount() > 1_000_000);
        assertFalse(channel.getAvatars().isEmpty());

        final ChannelTabExtractor tab = Twitch.getChannelTabExtractor(channel.getTabs().get(0));
        tab.fetchPage();
        final List<InfoItem> items = tab.getInitialPage().getItems();
        final StreamInfoItem video = items.stream()
                .map(StreamInfoItem.class::cast)
                .filter(item -> item.getStreamType() == StreamType.VIDEO_STREAM)
                .findFirst()
                .orElseThrow();
        assertTrue(video.getDuration() > 0);
        assertNotNull(video.getUploadDate());

        final StreamExtractor stream = Twitch.getStreamExtractor(video.getUrl());
        stream.fetchPage();
        assertEquals(StreamType.VIDEO_STREAM, stream.getStreamType());
        assertEquals(video.getName(), stream.getName());
        assertTrue(stream.getLength() > 0);
        assertFalse(stream.getVideoStreams().isEmpty());
    }

    @Test
    void search() throws Exception {
        final SearchExtractor search = Twitch.getSearchExtractor(CHANNEL);
        search.fetchPage();
        final ListExtractor.InfoItemsPage<InfoItem> page = search.getInitialPage();
        assertTrue(page.getItems().stream()
                .anyMatch(item -> item.getInfoType() == InfoItem.InfoType.CHANNEL
                        && CHANNEL.equalsIgnoreCase(item.getName())));
        assertTrue(page.hasNextPage());
        assertFalse(search.getPage(page.getNextPage()).getItems().isEmpty());
    }
}
