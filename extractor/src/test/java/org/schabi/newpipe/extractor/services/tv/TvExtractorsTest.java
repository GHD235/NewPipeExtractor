package org.schabi.newpipe.extractor.services.tv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.schabi.newpipe.extractor.ServiceList.TV;

import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.InitNewPipeTest;
import org.schabi.newpipe.extractor.kiosk.KioskExtractor;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamType;

import java.util.List;

public class TvExtractorsTest implements InitNewPipeTest {
    private static final String LA_1 = "https://www.tdtchannels.com/tv/generalistas-la-1";

    @Test
    void channelList() throws Exception {
        final List<TvChannelList.TvChannel> channels = TvChannelList.getChannels();
        assertTrue(channels.size() > 100, "only " + channels.size() + " channels");
        assertTrue(channels.stream().allMatch(c -> !c.getOptions().isEmpty()));
        assertEquals(channels.size(),
                channels.stream().map(TvChannelList.TvChannel::getId).distinct().count());
        assertEquals("Generalistas", channels.get(0).getGroup());
    }

    @Test
    void kioskListsPlayableChannels() throws Exception {
        final KioskExtractor<?> kiosk = TV.getKioskList().getDefaultKioskExtractor();
        kiosk.fetchPage();
        final List<?> items = kiosk.getInitialPage().getItems();
        assertTrue(items.size() > 100);
        final StreamInfoItem first = (StreamInfoItem) items.get(0);
        assertEquals(LA_1, first.getUrl());
        assertEquals(StreamType.LIVE_STREAM, first.getStreamType());
        assertFalse(first.getThumbnails().isEmpty());
    }

    @Test
    void streamOfLa1() throws Exception {
        final StreamExtractor extractor = TV.getStreamExtractor(LA_1);
        extractor.fetchPage();
        assertEquals("La 1", extractor.getName());
        assertEquals(StreamType.LIVE_STREAM, extractor.getStreamType());
        assertTrue(extractor.getHlsUrl().contains("rtve"), extractor.getHlsUrl());
        assertEquals(1, extractor.getVideoStreams().size());
    }

    @Test
    void searchIgnoresCaseAndAccents() throws Exception {
        final SearchExtractor search = TV.getSearchExtractor("TELEDEPÓRTE");
        search.fetchPage();
        final List<InfoItem> items = search.getInitialPage().getItems();
        assertTrue(items.stream().anyMatch(item -> item.getName().equals("Teledeporte")));
    }

    @Test
    void youtubeLiveOf24h() throws Exception {
        // 24h (RTVE) streams on YouTube around the clock
        final String url = TvParsingHelper.resolveYoutubeLive(
                "https://www.youtube.com/channel/UC7QZIf0dta-XPXsp9Hv4dTw/live");
        assertTrue(url != null && url.matches("https://www\\.youtube\\.com/watch\\?v=[\\w-]{11}"),
                String.valueOf(url));
    }

    @Test
    void twitchPlayerUrl() {
        assertEquals("https://www.twitch.tv/realmadrid", TvParsingHelper.twitchUrlFromPlayer(
                "https://player.twitch.tv/?channel=realmadrid&parent=play.tdtchannels.com"));
    }

    @Test
    void slugs() {
        assertEquals("c-de-madrid", TvChannelList.slug("C. de Madrid"));
        assertEquals("andalucia", TvChannelList.slug("Andalucía"));
    }
}
