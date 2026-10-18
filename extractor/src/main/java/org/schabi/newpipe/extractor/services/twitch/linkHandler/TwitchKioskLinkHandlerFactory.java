package org.schabi.newpipe.extractor.services.twitch.linkHandler;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.services.twitch.TwitchParsingHelper;
import org.schabi.newpipe.extractor.services.twitch.extractors.TwitchLiveKioskExtractor;

import java.util.List;

public final class TwitchKioskLinkHandlerFactory extends ListLinkHandlerFactory {
    private static final TwitchKioskLinkHandlerFactory INSTANCE =
            new TwitchKioskLinkHandlerFactory();

    private static final String LIVE_URL = TwitchParsingHelper.BASE_URL + "/directory/all";

    private TwitchKioskLinkHandlerFactory() {
    }

    public static TwitchKioskLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getId(final String url) throws ParsingException {
        if (onAcceptUrl(url)) {
            return TwitchLiveKioskExtractor.KIOSK_ID;
        }
        throw new ParsingException("Not a Twitch kiosk URL: " + url);
    }

    @Override
    public String getUrl(final String id,
                         final List<String> contentFilter,
                         final String sortFilter) {
        return LIVE_URL;
    }

    @Override
    public boolean onAcceptUrl(final String url) {
        return url.matches("^(?:https?://)?(?:www\\.)?twitch\\.tv/directory(?:/all)?/?$");
    }
}
