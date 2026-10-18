package org.schabi.newpipe.extractor.services.tv.linkHandler;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.services.tv.TvChannelList;
import org.schabi.newpipe.extractor.services.tv.extractors.TvChannelsKioskExtractor;

import java.util.List;

public final class TvKioskLinkHandlerFactory extends ListLinkHandlerFactory {
    private static final TvKioskLinkHandlerFactory INSTANCE = new TvKioskLinkHandlerFactory();

    private TvKioskLinkHandlerFactory() {
    }

    public static TvKioskLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getId(final String url) throws ParsingException {
        if (onAcceptUrl(url)) {
            return TvChannelsKioskExtractor.KIOSK_ID;
        }
        throw new ParsingException("Not a TV kiosk URL: " + url);
    }

    @Override
    public String getUrl(final String id, final List<String> contentFilter,
                         final String sortFilter) {
        return TvChannelList.LIST_URL;
    }

    @Override
    public boolean onAcceptUrl(final String url) {
        return TvChannelList.LIST_URL.equals(url);
    }
}
