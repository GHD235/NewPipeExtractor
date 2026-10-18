package org.schabi.newpipe.extractor.services.twitch.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;

import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.kiosk.KioskExtractor;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.services.twitch.TwitchParsingHelper;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

/** The most watched live channels on Twitch right now. */
public class TwitchLiveKioskExtractor extends KioskExtractor<StreamInfoItem> {
    public static final String KIOSK_ID = "live";
    private static final int PAGE_SIZE = 30;
    private static final String QUERY = "query($cursor: Cursor) {"
            + " streams(first: " + PAGE_SIZE + ", after: $cursor) {"
            + " edges { cursor node { " + TwitchGqlFields.STREAM + " } }"
            + " pageInfo { hasNextPage } } }";

    public TwitchLiveKioskExtractor(final StreamingService service,
                                    final ListLinkHandler linkHandler,
                                    final String kioskId) {
        super(service, linkHandler, kioskId);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader) {
        // everything is fetched in getInitialPage() and getPage()
    }

    @Nonnull
    @Override
    public String getName() {
        return KIOSK_ID;
    }

    @Nonnull
    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage() throws IOException, ExtractionException {
        return fetch(null);
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        return fetch(page.getId());
    }

    private InfoItemsPage<StreamInfoItem> fetch(final String cursor)
            throws IOException, ExtractionException {
        final Map<String, Object> variables = new HashMap<>();
        variables.put("cursor", cursor);
        final JsonObject streams = TwitchParsingHelper.gql(QUERY, variables).getObject("streams");

        final StreamInfoItemsCollector collector = new StreamInfoItemsCollector(getServiceId());
        final JsonArray edges = streams.getArray("edges");
        String lastCursor = null;
        for (int i = 0; i < edges.size(); i++) {
            final JsonObject edge = edges.getObject(i);
            collector.commit(new TwitchLiveInfoItemExtractor(edge.getObject("node")));
            lastCursor = edge.getString("cursor");
        }

        final boolean hasNext = streams.getObject("pageInfo").getBoolean("hasNextPage", false);
        final Page nextPage = hasNext && lastCursor != null ? new Page(getUrl(), lastCursor) : null;
        return new InfoItemsPage<>(collector, nextPage);
    }
}
