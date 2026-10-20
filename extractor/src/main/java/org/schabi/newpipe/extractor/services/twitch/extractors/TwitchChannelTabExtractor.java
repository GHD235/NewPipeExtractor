package org.schabi.newpipe.extractor.services.twitch.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.MultiInfoItemsCollector;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.services.twitch.TwitchParsingHelper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * The videos tab of a Twitch channel. If the channel is live, its broadcast is shown first.
 */
public class TwitchChannelTabExtractor extends ChannelTabExtractor {
    private static final int PAGE_SIZE = 30;
    private static final String QUERY = "query($login: String!, $cursor: Cursor) {"
            + " user(login: $login) { stream { " + TwitchGqlFields.STREAM + " }"
            + " videos(first: " + PAGE_SIZE + ", after: $cursor, sort: TIME) {"
            + " edges { cursor node { " + TwitchGqlFields.VIDEO + " } }"
            + " pageInfo { hasNextPage } } } }";

    public TwitchChannelTabExtractor(final StreamingService service,
                                     final ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader) {
        // everything is fetched in getInitialPage() and getPage()
    }

    @Nonnull
    @Override
    public InfoItemsPage<InfoItem> getInitialPage() throws IOException, ExtractionException {
        return fetch(null);
    }

    @Override
    public InfoItemsPage<InfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        return fetch(page.getId());
    }

    private InfoItemsPage<InfoItem> fetch(final String cursor)
            throws IOException, ExtractionException {
        final Map<String, Object> variables = new HashMap<>();
        variables.put("login", getId());
        variables.put("cursor", cursor);
        final JsonObject user = TwitchParsingHelper.gql(QUERY, variables).getObject("user");

        final MultiInfoItemsCollector collector = new MultiInfoItemsCollector(getServiceId());
        final JsonObject stream = user.getObject("stream");
        if (cursor == null && !stream.isEmpty()) {
            collector.commit(new TwitchLiveInfoItemExtractor(stream));
        }

        final JsonObject videos = user.getObject("videos");
        final JsonArray edges = videos.getArray("edges");
        String lastCursor = null;
        for (int i = 0; i < edges.size(); i++) {
            final JsonObject edge = edges.getObject(i);
            lastCursor = edge.getString("cursor");
            // the broadcast being recorded is the live stream above, not a video yet: listed,
            // it looked like a new one-minute video next to the live stream
            if ("RECORDING".equals(edge.getObject("node").getString("status"))) {
                continue;
            }
            collector.commit(new TwitchVideoInfoItemExtractor(edge.getObject("node")));
        }

        final boolean hasNext = videos.getObject("pageInfo").getBoolean("hasNextPage", false);
        final Page nextPage = hasNext && lastCursor != null ? new Page(getUrl(), lastCursor) : null;
        return new InfoItemsPage<>(collector, nextPage);
    }
}
