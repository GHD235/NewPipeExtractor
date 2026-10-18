package org.schabi.newpipe.extractor.services.twitch.linkHandler;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory;
import org.schabi.newpipe.extractor.services.twitch.TwitchParsingHelper;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Twitch streams are either the live broadcast of a channel ({@code twitch.tv/<login>}, id
 * {@code <login>}) or a past video ({@code twitch.tv/videos/<id>}, id {@code videos/<id>}).
 */
public final class TwitchStreamLinkHandlerFactory extends LinkHandlerFactory {
    private static final TwitchStreamLinkHandlerFactory INSTANCE =
            new TwitchStreamLinkHandlerFactory();

    public static final String VIDEO_ID_PREFIX = "videos/";

    private static final String HOST = "^(?:https?://)?(?:www\\.|m\\.|go\\.)?twitch\\.tv/";
    private static final Pattern VIDEO_PATTERN = Pattern.compile(
            HOST + "(?:[A-Za-z0-9_]+/)?(?:videos|v)/(\\d+)/?(?:[?#].*)?$");
    private static final Pattern LIVE_PATTERN = Pattern.compile(
            HOST + "([A-Za-z0-9_]{1,25})/?(?:[?#].*)?$");

    private TwitchStreamLinkHandlerFactory() {
    }

    public static TwitchStreamLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    public static boolean isVideoId(final String id) {
        return id.startsWith(VIDEO_ID_PREFIX);
    }

    @Override
    public String getId(final String url) throws ParsingException {
        final Matcher video = VIDEO_PATTERN.matcher(url);
        if (video.find()) {
            return VIDEO_ID_PREFIX + video.group(1);
        }
        final Matcher live = LIVE_PATTERN.matcher(url);
        if (live.find()) {
            final String login = live.group(1).toLowerCase(Locale.ROOT);
            if (!TwitchParsingHelper.RESERVED_PATHS.contains(login)) {
                return login;
            }
        }
        throw new ParsingException("Not a Twitch stream URL: " + url);
    }

    @Override
    public String getUrl(final String id) {
        return TwitchParsingHelper.BASE_URL + "/" + id;
    }

    @Override
    public boolean onAcceptUrl(final String url) {
        try {
            getId(url);
            return true;
        } catch (final ParsingException e) {
            return false;
        }
    }
}
