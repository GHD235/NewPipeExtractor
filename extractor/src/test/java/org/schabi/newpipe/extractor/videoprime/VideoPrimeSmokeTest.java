package org.schabi.newpipe.extractor.videoprime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.downloader.DownloaderTestImpl;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.channel.ChannelInfo;
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.kiosk.KioskInfo;
import org.schabi.newpipe.extractor.playlist.PlaylistInfo;
import org.schabi.newpipe.extractor.search.SearchInfo;
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.DeliveryMethod;
import org.schabi.newpipe.extractor.stream.StreamInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * VideoPrime: daily check, against the real services, of what the app does every day: play a
 * YouTube video (and download its audio), search YouTube and YouTube Music, open a channel and a
 * playlist, and watch a Twitch live stream and a past broadcast.
 *
 * <p>Only runs with the {@code VIDEOPRIME_SMOKE} environment variable, from the
 * "VideoPrime: vigilancia" workflow, so that it never runs with the recorded mocks.</p>
 */
class VideoPrimeSmokeTest {
    /** Videos that have been available for years; some may still fail from GitHub's IPs. */
    private static final String[] VIDEO_IDS = {
            "dQw4w9WgXcQ", "JGwWNGJdvx8", "fJ9rUzIMcZQ", "60ItHLz5WEA", "RgKAFK5djSk",
            "YQHsXMglC9A",
    };

    @BeforeAll
    static void setUp() {
        assumeTrue(System.getenv("VIDEOPRIME_SMOKE") != null,
                "Only for the daily check against the real services");
        NewPipe.init(DownloaderTestImpl.getInstance());
    }

    private static long timed(final String what, final long start) {
        final long millis = (System.nanoTime() - start) / 1_000_000;
        System.out.println("VIDEOPRIME " + what + ": " + millis + " ms");
        return millis;
    }

    @Test
    void youtubeVideosPlay() throws Exception {
        final List<String> failures = new ArrayList<>();
        StreamInfo playable = null;
        for (final String id : VIDEO_IDS) {
            final long start = System.nanoTime();
            try {
                final StreamInfo info = StreamInfo.getInfo(ServiceList.YouTube,
                        "https://www.youtube.com/watch?v=" + id);
                timed("YouTube video " + id, start);
                if (info.getAudioStreams().isEmpty() || info.getVideoStreams().isEmpty()
                        && info.getVideoOnlyStreams().isEmpty()) {
                    failures.add(id + ": no streams");
                } else if (playable == null) {
                    playable = info;
                }
            } catch (final Exception e) {
                timed("YouTube video " + id + " FAILED", start);
                failures.add(id + ": " + e);
            }
        }
        System.out.println("VIDEOPRIME YouTube failures: " + failures);
        assertTrue(failures.size() <= VIDEO_IDS.length / 2,
                "Too many YouTube videos fail: " + failures);

        // the audio must really download: this is where signature or "n" parameter
        // deobfuscation breaks, with 403 errors
        final AudioStream audio = playable.getAudioStreams().stream()
                .filter(s -> s.isUrl() && s.getDeliveryMethod() == DeliveryMethod.PROGRESSIVE_HTTP)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No progressive audio stream"));
        final Response response = NewPipe.getDownloader().get(audio.getContent(),
                Map.of("Range", List.of("bytes=0-65535")));
        assertTrue(response.responseCode() == 200 || response.responseCode() == 206,
                "The audio of " + playable.getId() + " cannot be downloaded: HTTP "
                        + response.responseCode());
    }

    @Test
    void youtubeSearch() throws Exception {
        final long start = System.nanoTime();
        final SearchInfo info = SearchInfo.getInfo(ServiceList.YouTube,
                ServiceList.YouTube.getSearchQHFactory().fromQuery("fito y fitipaldis"));
        timed("YouTube search", start);
        assertFalse(info.getRelatedItems().isEmpty(), "YouTube search found nothing");
    }

    @Test
    void youtubeMusicSearch() throws Exception {
        final StreamingService yt = ServiceList.YouTube;
        long start = System.nanoTime();
        final SearchInfo videos = SearchInfo.getInfo(yt, yt.getSearchQHFactory().fromQuery(
                "fito y fitipaldis", List.of(YoutubeSearchQueryHandlerFactory.MUSIC_VIDEOS), ""));
        timed("YouTube Music search (videos)", start);
        assertFalse(videos.getRelatedItems().isEmpty(), "YouTube Music finds no videos");

        // songs are only reported: from GitHub's servers YouTube Music may not offer them
        start = System.nanoTime();
        final SearchInfo songs = SearchInfo.getInfo(yt, yt.getSearchQHFactory().fromQuery(
                "fito y fitipaldis", List.of(YoutubeSearchQueryHandlerFactory.MUSIC_SONGS), ""));
        timed("YouTube Music search (songs, " + songs.getRelatedItems().size() + " found)",
                start);
    }

    @Test
    void youtubeChannelAndPlaylist() throws Exception {
        long start = System.nanoTime();
        final ChannelInfo channel = ChannelInfo.getInfo(ServiceList.YouTube,
                "https://www.youtube.com/@Gronkh");
        assertFalse(channel.getTabs().isEmpty(), "YouTube channel without tabs");
        final ChannelTabInfo videos = ChannelTabInfo.getInfo(ServiceList.YouTube,
                channel.getTabs().get(0));
        timed("YouTube channel", start);
        assertFalse(videos.getRelatedItems().isEmpty(), "YouTube channel without videos");

        start = System.nanoTime();
        final PlaylistInfo playlist = PlaylistInfo.getInfo(ServiceList.YouTube,
                "https://www.youtube.com/playlist?list=PLMC9KNkIncKtPzgY-5rmhvj7fax8fdxoj");
        timed("YouTube playlist", start);
        assertFalse(playlist.getRelatedItems().isEmpty(), "YouTube playlist without videos");
    }

    @Test
    void twitchLiveAndPastBroadcast() throws Exception {
        final StreamingService twitch = ServiceList.Twitch;
        long start = System.nanoTime();
        final KioskInfo live = KioskInfo.getInfo(twitch.getKioskList().getDefaultKioskExtractor());
        timed("Twitch live channels", start);
        assertFalse(live.getRelatedItems().isEmpty(), "Twitch has no live channels");

        final InfoItem first = live.getRelatedItems().get(0);
        start = System.nanoTime();
        final StreamInfo stream = StreamInfo.getInfo(twitch, first.getUrl());
        timed("Twitch live stream " + first.getUrl(), start);
        if (stream.getVideoStreams().isEmpty() && stream.getHlsUrl().isEmpty()) {
            fail("Twitch live stream without video: " + first.getUrl());
        }

        start = System.nanoTime();
        final ChannelInfo channel = ChannelInfo.getInfo(twitch, first.getUrl() + "/videos");
        final ChannelTabInfo videos = ChannelTabInfo.getInfo(twitch, channel.getTabs().get(0));
        timed("Twitch channel", start);
        final InfoItem vod = videos.getRelatedItems().stream()
                .filter(item -> item.getUrl().contains("/videos/"))
                .findFirst()
                .orElse(null);
        if (vod == null) {
            System.out.println("VIDEOPRIME Twitch channel without past broadcasts, skipped");
            return;
        }
        start = System.nanoTime();
        final StreamInfo past = StreamInfo.getInfo(twitch, vod.getUrl());
        timed("Twitch past broadcast", start);
        if (past.getVideoStreams().isEmpty() && past.getHlsUrl().isEmpty()) {
            fail("Twitch past broadcast without video: " + vod.getUrl());
        }
    }
}
