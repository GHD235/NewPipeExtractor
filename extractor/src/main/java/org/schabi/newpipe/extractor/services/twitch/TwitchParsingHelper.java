package org.schabi.newpipe.extractor.services.twitch;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonWriter;

import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.utils.JsonUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Helpers shared by the Twitch extractors.
 *
 * <p>
 * Everything goes through Twitch's public GraphQL endpoint, using the same anonymous client ID
 * as the Twitch website, so no account is needed.
 * </p>
 */
public final class TwitchParsingHelper {
    public static final String BASE_URL = "https://www.twitch.tv";
    public static final String GQL_URL = "https://gql.twitch.tv/gql";
    private static final String CLIENT_ID = "kimne78kx3ncx6brgo4mv6wki5h1ko";
    private static final String USHER_URL = "https://usher.ttvnw.net";

    /** Paths on twitch.tv that are not channel logins. */
    public static final List<String> RESERVED_PATHS = List.of(
            "directory", "videos", "search", "settings", "subscriptions", "inventory", "wallet",
            "drops", "downloads", "jobs", "turbo", "prime", "p", "friends", "messages",
            "following", "login", "signup", "store", "moderator", "popout", "embed", "u");

    private TwitchParsingHelper() {
    }

    /**
     * Run a GraphQL query and return its {@code data} object.
     */
    @Nonnull
    public static JsonObject gql(@Nonnull final String query,
                                 @Nonnull final Map<String, Object> variables)
            throws IOException, ExtractionException {
        final String body = JsonWriter.string()
                .object()
                .value("query", query)
                .object("variables", variables)
                .end()
                .done();

        final Downloader downloader = NewPipe.getDownloader();
        final Response response = downloader.postWithContentTypeJson(GQL_URL,
                Map.of("Client-ID", List.of(CLIENT_ID)),
                body.getBytes(StandardCharsets.UTF_8));
        if (response.responseCode() != 200) {
            throw new ExtractionException("Twitch GQL returned HTTP " + response.responseCode());
        }

        final JsonObject json = JsonUtils.toJsonObject(response.responseBody());
        final JsonArray errors = json.getArray("errors");
        final JsonObject data = json.getObject("data");
        // an error in one field (e.g. one channel of a batch) still comes with the rest of the
        // data: each extractor checks the fields it needs
        if (!errors.isEmpty() && data.isEmpty()) {
            throw new ExtractionException("Twitch GQL error: "
                    + errors.getObject(0).getString("message"));
        }
        return data;
    }

    /**
     * Build the URL of the HLS master playlist from a playback access token.
     *
     * @param path            {@code api/channel/hls/<login>} for live streams or
     *                        {@code vod/<id>} for videos
     * @param tokenObject     the {@code streamPlaybackAccessToken} or
     *                        {@code videoPlaybackAccessToken} object
     */
    @Nonnull
    public static String getHlsMasterUrl(@Nonnull final String path,
                                         @Nonnull final JsonObject tokenObject)
            throws ParsingException {
        final String value = tokenObject.getString("value");
        final String signature = tokenObject.getString("signature");
        if (value == null || signature == null) {
            throw new ParsingException("Could not get Twitch playback access token");
        }
        return USHER_URL + "/" + path + ".m3u8"
                + "?allow_source=true&allow_audio_only=true&fast_bread=true"
                + "&playlist_include_framerate=true"
                + "&sig=" + signature
                + "&token=" + encode(value);
    }

    @Nonnull
    public static String encode(@Nonnull final String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @Nonnull
    public static String getChannelUrl(@Nonnull final String login) {
        return BASE_URL + "/" + login;
    }

    @Nonnull
    public static String getVideoUrl(@Nonnull final String videoId) {
        return BASE_URL + "/videos/" + videoId;
    }

    /**
     * Twitch image URLs either contain a fixed size or {@code {width}x{height}} (or
     * {@code %{width}x%{height}}) placeholders.
     *
     * @param url    the image URL given by Twitch
     * @param width  the width to ask for
     * @param height the height to ask for
     * @return the image, or no image if Twitch has none yet
     */
    @Nonnull
    public static List<Image> getImages(@Nullable final String url,
                                        final int width,
                                        final int height) {
        // past broadcasts that are still being processed point to an image that Twitch refuses
        // (403), better no image than a broken one
        if (url == null || url.isEmpty() || url.contains("/_404/")) {
            return List.of();
        }
        // "%{width}" must go first: replacing "{width}" first would leave "%640"
        final String fixedUrl = url.replace("%{width}", String.valueOf(width))
                .replace("{width}", String.valueOf(width))
                .replace("%{height}", String.valueOf(height))
                .replace("{height}", String.valueOf(height));
        return List.of(new Image(fixedUrl, height, width,
                Image.ResolutionLevel.fromHeight(height)));
    }

    @Nullable
    public static DateWrapper parseDate(@Nullable final String date) throws ParsingException {
        if (date == null || date.isEmpty()) {
            return null;
        }
        try {
            return new DateWrapper(OffsetDateTime.parse(date));
        } catch (final DateTimeParseException e) {
            throw new ParsingException("Could not parse date: " + date, e);
        }
    }
}
