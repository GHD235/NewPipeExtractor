package org.schabi.newpipe.extractor.services.twitch.linkHandler;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.services.twitch.TwitchParsingHelper;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A plain {@code twitch.tv/<login>} link opens the live broadcast (see
 * {@link TwitchStreamLinkHandlerFactory}), so channels use the {@code /videos} page of the
 * channel as their URL. The id is the channel login.
 */
public final class TwitchChannelLinkHandlerFactory extends ListLinkHandlerFactory {
    private static final TwitchChannelLinkHandlerFactory INSTANCE =
            new TwitchChannelLinkHandlerFactory();

    private static final Pattern CHANNEL_PATTERN = Pattern.compile(
            "^(?:https?://)?(?:www\\.|m\\.)?twitch\\.tv/([A-Za-z0-9_]{1,25})"
                    + "/(?:videos|about|schedule|clips|collections)/?(?:[?#].*)?$");

    private TwitchChannelLinkHandlerFactory() {
    }

    public static TwitchChannelLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getId(final String url) throws ParsingException {
        final Matcher matcher = CHANNEL_PATTERN.matcher(url);
        if (matcher.find()) {
            final String login = matcher.group(1).toLowerCase(Locale.ROOT);
            if (!TwitchParsingHelper.RESERVED_PATHS.contains(login)) {
                return login;
            }
        }
        throw new ParsingException("Not a Twitch channel URL: " + url);
    }

    @Override
    public String getUrl(final String id,
                         final List<String> contentFilter,
                         final String sortFilter) {
        return TwitchParsingHelper.getChannelUrl(id) + "/videos";
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
