package org.schabi.newpipe.extractor.services.youtube;

import com.grack.nanojson.JsonBuilder;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonWriter;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.localization.ContentCountry;
import org.schabi.newpipe.extractor.localization.Localization;
import org.schabi.newpipe.extractor.utils.JsonUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import static org.schabi.newpipe.extractor.NewPipe.getDownloader;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.CONTENT_CHECK_OK;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.CPN;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.RACY_CHECK_OK;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.VIDEO_ID;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.YOUTUBEI_V1_GAPIS_URL;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.YOUTUBEI_V1_URL;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.generateTParameter;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getClientVersion;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getValidJsonResponseBody;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getVisionOsUserAgent;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getYouTubeHeaders;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.prepareJsonBuilder;

public final class YoutubeStreamHelper {

    private static final String PLAYER = "player";

    private YoutubeStreamHelper() {
    }

    /**
     * VideoPrime: asks YouTube now for what every video needs first (the web client's version
     * and the visitor data of both clients used for a video), so that the first video opened
     * after starting the app does not wait for them (measured: about 3.4 s instead of 1.2 s).
     * Both are kept for a while, see {@link YoutubeParsingHelper#getVisitorDataFromInnertube}.
     *
     * @param localization   the localization the videos will be asked with
     * @param contentCountry the content country the videos will be asked with
     * @throws IOException         if a request fails
     * @throws ExtractionException if a response cannot be used
     */
    public static void warmUp(@Nonnull final Localization localization,
                              @Nonnull final ContentCountry contentCountry)
            throws IOException, ExtractionException {
        final Future<String> visionOs = YoutubeParsingHelper.inParallel(
                () -> {
                    final InnertubeClientRequestInfo info =
                            InnertubeClientRequestInfo.ofVisionOsClient();
                    return YoutubeParsingHelper.getVisitorDataFromInnertube(info, localization,
                            contentCountry, visionOsHeaders(localization), YOUTUBEI_V1_URL, null,
                            false);
                });
        final InnertubeClientRequestInfo web = InnertubeClientRequestInfo.ofWebClient();
        web.clientInfo.clientVersion = getClientVersion();
        YoutubeParsingHelper.getVisitorDataFromInnertube(web, localization, contentCountry,
                getYouTubeHeaders(), YOUTUBEI_V1_URL, null, false);
        YoutubeParsingHelper.awaitParallel(visionOs);
    }

    @Nonnull
    private static Map<String, List<String>> visionOsHeaders(
            @Nonnull final Localization localization) {
        return Map.of("User-Agent", List.of(getVisionOsUserAgent(localization)),
                "X-Goog-Api-Format-Version", List.of("2"));
    }

    @Nonnull
    public static JsonObject getWebMetadataPlayerResponse(
            @Nonnull final Localization localization,
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final String videoId) throws IOException, ExtractionException {
        final InnertubeClientRequestInfo innertubeClientRequestInfo =
                InnertubeClientRequestInfo.ofWebClient();
        innertubeClientRequestInfo.clientInfo.clientVersion = getClientVersion();

        final Map<String, List<String>> headers = getYouTubeHeaders();

        // We must always pass a valid visitorData to get valid player responses, which needs to be
        // got from YouTube
        innertubeClientRequestInfo.clientInfo.visitorData =
                YoutubeParsingHelper.getVisitorDataFromInnertube(innertubeClientRequestInfo,
                        localization, contentCountry, headers, YOUTUBEI_V1_URL, null, false);

        final JsonBuilder<JsonObject> builder = prepareJsonBuilder(localization, contentCountry,
                innertubeClientRequestInfo, null);

        addVideoIdCpnAndOkChecks(builder, videoId, null);

        final byte[] body = JsonWriter.string(builder.done())
                .getBytes(StandardCharsets.UTF_8);

        final String url = YOUTUBEI_V1_URL + PLAYER + "?" + DISABLE_PRETTY_PRINT_PARAMETER
                + "&$fields=microformat,videoDetails.thumbnail.thumbnails,videoDetails.videoId";

        return JsonUtils.toJsonObject(getValidJsonResponseBody(
                getDownloader().postWithContentTypeJson(
                        url, headers, body, localization)));
    }

    public static JsonObject getVisionOsPlayerResponse(@Nonnull final ContentCountry contentCountry,
                                                       @Nonnull final Localization localization,
                                                       @Nonnull final String videoId,
                                                       @Nonnull final String cpn)
            throws IOException, ExtractionException {
        final InnertubeClientRequestInfo innertubeClientRequestInfo =
                InnertubeClientRequestInfo.ofVisionOsClient();

        final Map<String, List<String>> headers = visionOsHeaders(localization);

        // We must always pass a valid visitorData to get valid player responses, which needs to be
        // got from YouTube
        innertubeClientRequestInfo.clientInfo.visitorData =
                YoutubeParsingHelper.getVisitorDataFromInnertube(innertubeClientRequestInfo,
                localization, contentCountry, headers, YOUTUBEI_V1_URL, null, false);

        final JsonBuilder<JsonObject> builder = prepareJsonBuilder(localization, contentCountry,
                innertubeClientRequestInfo, null);

        addVideoIdCpnAndOkChecks(builder, videoId, cpn);

        final byte[] body = JsonWriter.string(builder.done())
                .getBytes(StandardCharsets.UTF_8);

        final String url = YOUTUBEI_V1_GAPIS_URL + PLAYER + "?" + DISABLE_PRETTY_PRINT_PARAMETER
                + "&t=" + generateTParameter() + "&id=" + videoId;

        return JsonUtils.toJsonObject(getValidJsonResponseBody(
                getDownloader().postWithContentTypeJson(url, headers, body, localization)));
    }

    private static void addVideoIdCpnAndOkChecks(@Nonnull final JsonBuilder<JsonObject> builder,
                                                 @Nonnull final String videoId,
                                                 @Nullable final String cpn) {
        builder.value(VIDEO_ID, videoId);

        if (cpn != null) {
            builder.value(CPN, cpn);
        }

        builder.value(CONTENT_CHECK_OK, true)
                .value(RACY_CHECK_OK, true);
    }
}
