package org.schabi.newpipe.extractor.services.tv;

import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TvParsingHelper {
    /** Channel URLs are VideoPrime's own ids under the list's site; they are not real pages. */
    public static final String CHANNEL_URL_PREFIX = "https://www.tdtchannels.com/tv/";

    private static final Pattern YOUTUBE_VIDEO_ID = Pattern.compile(
            "\"videoId\":\"([\\w-]{11})\"");
    private static final Pattern TWITCH_PLAYER = Pattern.compile(
            "player\\.twitch\\.tv/\\?(?:.*&)?channel=([A-Za-z0-9_]+)");

    private TvParsingHelper() {
    }

    @Nonnull
    public static String getChannelUrl(@Nonnull final String channelId) {
        return CHANNEL_URL_PREFIX + channelId;
    }

    @Nonnull
    public static List<Image> getLogo(@Nullable final String logo) {
        if (logo == null) {
            return List.of();
        }
        return List.of(new Image(logo, Image.HEIGHT_UNKNOWN, Image.WIDTH_UNKNOWN,
                Image.ResolutionLevel.MEDIUM));
    }

    /**
     * Find the video that a YouTube channel is streaming live right now.
     *
     * @param channelLiveUrl a {@code youtube.com/channel/<id>/live} URL
     * @return the {@code watch?v=} URL of the live stream, or {@code null} if it is not live
     */
    @Nullable
    public static String resolveYoutubeLive(@Nonnull final String channelLiveUrl)
            throws IOException, ExtractionException {
        // the /live page of a channel that is streaming embeds the player of the live video
        final String page = NewPipe.getDownloader().get(channelLiveUrl).responseBody();
        if (!page.contains("\"isLive\":true")) {
            return null;
        }
        final Matcher matcher = YOUTUBE_VIDEO_ID.matcher(page);
        return matcher.find() ? "https://www.youtube.com/watch?v=" + matcher.group(1) : null;
    }

    /**
     * @return the {@code twitch.tv/<channel>} URL of a Twitch embedded player URL, or
     * {@code null} if the URL is not one
     */
    @Nullable
    public static String twitchUrlFromPlayer(@Nonnull final String url) {
        final Matcher matcher = TWITCH_PLAYER.matcher(url);
        return matcher.find() ? "https://www.twitch.tv/" + matcher.group(1) : null;
    }
}
