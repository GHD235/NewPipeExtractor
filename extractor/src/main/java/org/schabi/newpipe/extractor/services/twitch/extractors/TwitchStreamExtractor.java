package org.schabi.newpipe.extractor.services.twitch.extractors;

import com.grack.nanojson.JsonObject;

import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.PaidContentException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.services.twitch.TwitchParsingHelper;
import org.schabi.newpipe.extractor.services.twitch.linkHandler.TwitchStreamLinkHandlerFactory;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.DeliveryMethod;
import org.schabi.newpipe.extractor.stream.Description;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Live broadcasts and past videos of Twitch. Both are played through the HLS master playlist
 * served by {@code usher.ttvnw.net}; every quality listed there is exposed as a separate stream.
 */
public class TwitchStreamExtractor extends StreamExtractor {
    private static final String TOKEN_PARAMS =
            "params: {platform: \"web\", playerBackend: \"mediaplayer\", playerType: \"site\"}";

    private static final String LIVE_QUERY = "query($login: String!) {"
            + " user(login: $login) { login displayName description profileImageURL(width: 150)"
            + " followers { totalCount } stream { " + TwitchGqlFields.STREAM + " } }"
            + " streamPlaybackAccessToken(channelName: $login, " + TOKEN_PARAMS + ")"
            + " { value signature } }";

    private static final String VIDEO_QUERY = "query($id: ID!) {"
            + " video(id: $id) { " + TwitchGqlFields.VIDEO + " description"
            + " owner { followers { totalCount } } }"
            + " videoPlaybackAccessToken(id: $id, " + TOKEN_PARAMS + ") { value signature } }";

    private static final Pattern ATTRIBUTE = Pattern.compile("([A-Z-]+)=(\"[^\"]*\"|[^,]*)");

    private boolean isLive;
    private JsonObject owner;
    /** The live {@code Stream} or the {@code Video}, depending on {@link #isLive}. */
    private JsonObject item;
    private String hlsMasterUrl;
    private final List<VideoStream> videoStreams = new ArrayList<>();
    private final List<AudioStream> audioStreams = new ArrayList<>();

    public TwitchStreamExtractor(final StreamingService service, final LinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        final String id = getId();
        isLive = !TwitchStreamLinkHandlerFactory.isVideoId(id);

        final JsonObject data;
        final JsonObject token;
        if (isLive) {
            data = TwitchParsingHelper.gql(LIVE_QUERY, Map.of("login", id));
            owner = data.getObject("user");
            if (owner.isEmpty()) {
                throw new ContentNotAvailableException("Twitch channel does not exist: " + id);
            }
            item = owner.getObject("stream");
            if (item.isEmpty()) {
                throw new ContentNotAvailableException(
                        owner.getString("displayName") + " is not live right now");
            }
            token = data.getObject("streamPlaybackAccessToken");
            hlsMasterUrl = TwitchParsingHelper.getHlsMasterUrl("api/channel/hls/" + id, token);
        } else {
            final String videoId = id.substring(
                    TwitchStreamLinkHandlerFactory.VIDEO_ID_PREFIX.length());
            data = TwitchParsingHelper.gql(VIDEO_QUERY, Map.of("id", videoId));
            item = data.getObject("video");
            if (item.isEmpty()) {
                throw new ContentNotAvailableException("Twitch video does not exist: " + videoId);
            }
            owner = item.getObject("owner");
            token = data.getObject("videoPlaybackAccessToken");
            hlsMasterUrl = TwitchParsingHelper.getHlsMasterUrl("vod/" + videoId, token);
        }

        final Response response = downloader.get(hlsMasterUrl);
        if (response.responseCode() == 403) {
            throw new PaidContentException("This Twitch content is only for subscribers");
        }
        if (response.responseCode() != 200) {
            throw new ContentNotAvailableException(
                    "Could not get Twitch playlist: HTTP " + response.responseCode());
        }
        parseMasterPlaylist(response.responseBody());
    }

    /**
     * Turn every variant of the HLS master playlist into a stream. Variant names come from the
     * {@code EXT-X-MEDIA} tags, e.g. {@code 1080p60 (source)}, {@code 720p60} or
     * {@code audio_only}.
     */
    private void parseMasterPlaylist(@Nonnull final String playlist) {
        final Map<String, String> groupNames = new HashMap<>();
        Map<String, String> pendingVariant = null;

        for (final String rawLine : playlist.split("\n")) {
            final String line = rawLine.trim();
            if (line.startsWith("#EXT-X-MEDIA:")) {
                final Map<String, String> attributes = parseAttributes(line);
                groupNames.put(attributes.get("GROUP-ID"), attributes.get("NAME"));
            } else if (line.startsWith("#EXT-X-STREAM-INF:")) {
                pendingVariant = parseAttributes(line);
            } else if (pendingVariant != null && !line.isEmpty() && !line.startsWith("#")) {
                addVariant(pendingVariant, groupNames, line);
                pendingVariant = null;
            }
        }
    }

    private void addVariant(@Nonnull final Map<String, String> attributes,
                            @Nonnull final Map<String, String> groupNames,
                            @Nonnull final String url) {
        final String group = attributes.getOrDefault("VIDEO", "");
        final String name = groupNames.getOrDefault(group, group);

        if ("audio_only".equals(group)) {
            audioStreams.add(new AudioStream.Builder()
                    .setId(group)
                    .setContent(url, true)
                    .setMediaFormat(MediaFormat.M4A)
                    .setDeliveryMethod(DeliveryMethod.HLS)
                    .setAverageBitrate(parseBitrateKbps(attributes.get("BANDWIDTH")))
                    .build());
            return;
        }

        videoStreams.add(new VideoStream.Builder()
                .setId(group)
                .setContent(url, true)
                .setMediaFormat(MediaFormat.MPEG_4)
                .setDeliveryMethod(DeliveryMethod.HLS)
                .setIsVideoOnly(false)
                .setResolution(name.replace(" (source)", ""))
                .build());
    }

    @Nonnull
    private static Map<String, String> parseAttributes(@Nonnull final String line) {
        final Map<String, String> attributes = new HashMap<>();
        final Matcher matcher = ATTRIBUTE.matcher(line.substring(line.indexOf(':') + 1));
        while (matcher.find()) {
            attributes.put(matcher.group(1), matcher.group(2).replace("\"", ""));
        }
        return attributes;
    }

    private static int parseBitrateKbps(@Nullable final String bandwidth) {
        try {
            return Integer.parseInt(bandwidth) / 1000;
        } catch (final NumberFormatException e) {
            return AudioStream.UNKNOWN_BITRATE;
        }
    }

    @Nonnull
    @Override
    public String getName() {
        return item.getString("title", "");
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        return TwitchParsingHelper.getImages(
                item.getString(isLive ? "previewImageURL" : "previewThumbnailURL"), 640, 360);
    }

    @Nonnull
    @Override
    public Description getDescription() {
        final String text = isLive
                ? owner.getString("description", "")
                : item.getString("description", "");
        return new Description(text, Description.Type.PLAIN_TEXT);
    }

    @Override
    public long getLength() {
        return isLive ? 0 : item.getLong("lengthSeconds", 0);
    }

    @Override
    public long getViewCount() {
        return item.getLong(isLive ? "viewersCount" : "viewCount", -1);
    }

    @Nullable
    @Override
    public String getTextualUploadDate() {
        return item.getString(isLive ? "createdAt" : "publishedAt");
    }

    @Nullable
    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        return TwitchParsingHelper.parseDate(getTextualUploadDate());
    }

    @Nonnull
    @Override
    public String getUploaderUrl() {
        final String login = isLive
                ? owner.getString("login")
                : item.getObject("owner").getString("login");
        return TwitchParsingHelper.getChannelUrl(login) + "/videos";
    }

    @Nonnull
    @Override
    public String getUploaderName() {
        return isLive
                ? owner.getString("displayName", "")
                : item.getObject("owner").getString("displayName", "");
    }

    @Nonnull
    @Override
    public List<Image> getUploaderAvatars() {
        final String avatar = isLive
                ? owner.getString("profileImageURL")
                : item.getObject("owner").getString("profileImageURL");
        return TwitchParsingHelper.getImages(avatar, 150, 150);
    }

    @Override
    public long getUploaderSubscriberCount() {
        return owner.getObject("followers").getLong("totalCount", -1);
    }

    @Nonnull
    @Override
    public String getCategory() {
        return item.getObject("game").getString("displayName", "");
    }

    @Nonnull
    @Override
    public String getHlsUrl() {
        return hlsMasterUrl;
    }

    @Override
    public List<AudioStream> getAudioStreams() {
        return audioStreams;
    }

    @Override
    public List<VideoStream> getVideoStreams() {
        return videoStreams;
    }

    @Override
    public List<VideoStream> getVideoOnlyStreams() {
        return List.of();
    }

    @Override
    public StreamType getStreamType() {
        return isLive ? StreamType.LIVE_STREAM : StreamType.VIDEO_STREAM;
    }
}
