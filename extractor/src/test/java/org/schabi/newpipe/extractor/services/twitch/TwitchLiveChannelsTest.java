package org.schabi.newpipe.extractor.services.twitch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.schabi.newpipe.extractor.ServiceList.Twitch;

import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.InitNewPipeTest;
import org.schabi.newpipe.extractor.kiosk.KioskExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamType;

import java.util.List;

public class TwitchLiveChannelsTest implements InitNewPipeTest {
    @Test
    void reportsOnlyLiveChannels() throws Exception {
        final KioskExtractor<?> kiosk = Twitch.getKioskList().getDefaultKioskExtractor();
        kiosk.fetchPage();
        final StreamInfoItem live = (StreamInfoItem) kiosk.getInitialPage().getItems().get(0);
        final String liveLogin = live.getUrl().substring(live.getUrl().lastIndexOf('/') + 1);

        final List<StreamInfoItem> items = TwitchLiveChannels.fetch(
                List.of(liveLogin, "this_channel_does_not_exist_0042"));

        assertEquals(1, items.size());
        assertEquals(live.getUrl(), items.get(0).getUrl());
        assertEquals(StreamType.LIVE_STREAM, items.get(0).getStreamType());
        assertTrue(items.get(0).getViewCount() > 0);
    }
}
