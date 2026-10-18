package org.schabi.newpipe.extractor.services.twitch.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.MetaInfo;
import org.schabi.newpipe.extractor.MultiInfoItemsCollector;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.services.twitch.TwitchParsingHelper;
import org.schabi.newpipe.extractor.services.twitch.linkHandler.TwitchSearchQueryHandlerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * Searches Twitch channels and videos. The next page keeps one cursor per result type in
 * {@link Page#getIds()}: {@code [channelsCursor, videosCursor]}, empty when exhausted.
 */
public class TwitchSearchExtractor extends SearchExtractor {
    private static final String CHANNEL = "CHANNEL";
    private static final String VOD = "VOD";
    private static final String QUERY = "query($query: String!, $targets: [SearchForTarget!]) {"
            + " searchFor(userQuery: $query, platform: \"web\", options: {targets: $targets}) {"
            + " channels { cursor edges { item { ... on User { " + TwitchGqlFields.USER
            + " } } } }"
            + " videos { cursor edges { item { ... on Video { " + TwitchGqlFields.VIDEO
            + " } } } } } }";

    public TwitchSearchExtractor(final StreamingService service,
                                 final SearchQueryHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader) {
        // everything is fetched in getInitialPage() and getPage()
    }

    private String getFilter() {
        final List<String> filters = getLinkHandler().getContentFilters();
        return filters.isEmpty() ? TwitchSearchQueryHandlerFactory.ALL : filters.get(0);
    }

    @Nonnull
    @Override
    public InfoItemsPage<InfoItem> getInitialPage() throws IOException, ExtractionException {
        final String filter = getFilter();
        final boolean channels = !TwitchSearchQueryHandlerFactory.VIDEOS.equals(filter);
        final boolean videos = !TwitchSearchQueryHandlerFactory.CHANNELS.equals(filter);
        return fetch(channels ? "" : null, videos ? "" : null);
    }

    @Override
    public InfoItemsPage<InfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        final List<String> ids = page.getIds();
        return fetch(emptyToNull(ids.get(0)), emptyToNull(ids.get(1)));
    }

    private static String emptyToNull(final String cursor) {
        return cursor == null || cursor.isEmpty() ? null : cursor;
    }

    /**
     * @param channelsCursor {@code null} to skip channels, empty for the first page
     * @param videosCursor   {@code null} to skip videos, empty for the first page
     */
    private InfoItemsPage<InfoItem> fetch(final String channelsCursor, final String videosCursor)
            throws IOException, ExtractionException {
        final List<Map<String, Object>> targets = new ArrayList<>();
        if (channelsCursor != null) {
            targets.add(target(CHANNEL, channelsCursor));
        }
        if (videosCursor != null) {
            targets.add(target(VOD, videosCursor));
        }

        final Map<String, Object> variables = new HashMap<>();
        variables.put("query", getSearchString());
        variables.put("targets", targets);
        final JsonObject result = TwitchParsingHelper.gql(QUERY, variables)
                .getObject("searchFor");

        final MultiInfoItemsCollector collector = new MultiInfoItemsCollector(getServiceId());
        String nextChannels = "";
        String nextVideos = "";
        if (channelsCursor != null) {
            final JsonObject channels = result.getObject("channels");
            for (final JsonObject item : items(channels)) {
                collector.commit(new TwitchChannelInfoItemExtractor(item));
            }
            nextChannels = channels.getString("cursor", "");
        }
        if (videosCursor != null) {
            final JsonObject videos = result.getObject("videos");
            for (final JsonObject item : items(videos)) {
                collector.commit(new TwitchVideoInfoItemExtractor(item));
            }
            nextVideos = videos.getString("cursor", "");
        }

        final Page nextPage = nextChannels.isEmpty() && nextVideos.isEmpty()
                ? null
                : new Page(List.of(nextChannels, nextVideos));
        return new InfoItemsPage<>(collector, nextPage);
    }

    private static Map<String, Object> target(final String index, final String cursor) {
        final Map<String, Object> target = new HashMap<>();
        target.put("index", index);
        if (!cursor.isEmpty()) {
            target.put("cursor", cursor);
        }
        return target;
    }

    private static List<JsonObject> items(final JsonObject section) {
        final List<JsonObject> items = new ArrayList<>();
        final JsonArray edges = section.getArray("edges");
        for (int i = 0; i < edges.size(); i++) {
            final JsonObject item = edges.getObject(i).getObject("item");
            if (!item.isEmpty()) {
                items.add(item);
            }
        }
        return items;
    }

    @Nonnull
    @Override
    public String getSearchSuggestion() {
        return "";
    }

    @Override
    public boolean isCorrectedSearch() {
        return false;
    }

    @Nonnull
    @Override
    public List<MetaInfo> getMetaInfo() {
        return List.of();
    }
}
