package org.schabi.newpipe.extractor.services.tv.extractors;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.MetaInfo;
import org.schabi.newpipe.extractor.MultiInfoItemsCollector;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.services.tv.TvChannelList;
import org.schabi.newpipe.extractor.services.tv.TvChannelList.TvChannel;

import java.io.IOException;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;

/** Finds TV channels whose name or group contains the query (ignoring case and accents). */
public class TvSearchExtractor extends SearchExtractor {
    public TvSearchExtractor(final StreamingService service, final SearchQueryHandler query) {
        super(service, query);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader) {
        // the list is fetched (and cached) by TvChannelList
    }

    @Nonnull
    @Override
    public InfoItemsPage<InfoItem> getInitialPage() throws IOException, ExtractionException {
        final String query = normalize(getSearchString());
        final MultiInfoItemsCollector collector = new MultiInfoItemsCollector(getServiceId());
        for (final TvChannel channel : TvChannelList.getChannels()) {
            if (channel.getOption(TvChannelList.Format.M3U8) != null
                    && (normalize(channel.getName()).contains(query)
                    || normalize(channel.getGroup()).contains(query))) {
                collector.commit(new TvChannelInfoItemExtractor(channel));
            }
        }
        return new InfoItemsPage<>(collector, null);
    }

    private static String normalize(final String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    @Override
    public InfoItemsPage<InfoItem> getPage(final Page page) {
        return InfoItemsPage.emptyPage();
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
