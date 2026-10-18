package org.schabi.newpipe.extractor.services.tv.linkHandler;

import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory;
import org.schabi.newpipe.extractor.services.tv.TvChannelList;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Searches are done locally on the channel names; the URL only carries the query. */
public final class TvSearchQueryHandlerFactory extends SearchQueryHandlerFactory {
    private static final TvSearchQueryHandlerFactory INSTANCE = new TvSearchQueryHandlerFactory();

    private TvSearchQueryHandlerFactory() {
    }

    public static TvSearchQueryHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getUrl(final String query,
                         final List<String> contentFilter,
                         final String sortFilter) {
        return TvChannelList.LIST_URL + "?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
    }
}
