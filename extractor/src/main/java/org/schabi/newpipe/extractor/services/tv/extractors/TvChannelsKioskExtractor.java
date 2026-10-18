package org.schabi.newpipe.extractor.services.tv.extractors;

import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.kiosk.KioskExtractor;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.services.tv.TvChannelList;
import org.schabi.newpipe.extractor.services.tv.TvChannelList.TvChannel;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector;

import java.io.IOException;

import javax.annotation.Nonnull;

/** Every channel that can be played inside the app, in the list's order. */
public class TvChannelsKioskExtractor extends KioskExtractor<StreamInfoItem> {
    public static final String KIOSK_ID = "tv_channels";

    public TvChannelsKioskExtractor(final StreamingService service,
                                    final ListLinkHandler linkHandler,
                                    final String kioskId) {
        super(service, linkHandler, kioskId);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader) {
        // the list is fetched (and cached) by TvChannelList
    }

    @Nonnull
    @Override
    public String getName() {
        return KIOSK_ID;
    }

    @Nonnull
    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage() throws IOException, ExtractionException {
        final StreamInfoItemsCollector collector = new StreamInfoItemsCollector(getServiceId());
        for (final TvChannel channel : TvChannelList.getChannels()) {
            if (channel.getOption(TvChannelList.Format.M3U8) != null) {
                collector.commit(new TvChannelInfoItemExtractor(channel));
            }
        }
        return new InfoItemsPage<>(collector, null);
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getPage(final Page page) {
        return InfoItemsPage.emptyPage();
    }
}
