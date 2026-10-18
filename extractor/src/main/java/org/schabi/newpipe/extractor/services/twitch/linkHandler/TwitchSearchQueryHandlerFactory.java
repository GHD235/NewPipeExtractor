package org.schabi.newpipe.extractor.services.twitch.linkHandler;

import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory;
import org.schabi.newpipe.extractor.services.twitch.TwitchParsingHelper;

import java.util.List;

public final class TwitchSearchQueryHandlerFactory extends SearchQueryHandlerFactory {
    private static final TwitchSearchQueryHandlerFactory INSTANCE =
            new TwitchSearchQueryHandlerFactory();

    public static final String ALL = "all";
    public static final String CHANNELS = "channels";
    public static final String VIDEOS = "videos";

    private TwitchSearchQueryHandlerFactory() {
    }

    public static TwitchSearchQueryHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getUrl(final String query,
                         final List<String> contentFilter,
                         final String sortFilter) {
        String url = TwitchParsingHelper.BASE_URL + "/search?term="
                + TwitchParsingHelper.encode(query);
        if (!contentFilter.isEmpty() && !ALL.equals(contentFilter.get(0))) {
            url += "&type=" + contentFilter.get(0);
        }
        return url;
    }

    @Override
    public String[] getAvailableContentFilter() {
        return new String[] {ALL, CHANNELS, VIDEOS};
    }
}
