package org.schabi.newpipe.extractor.services.tv;

import static org.schabi.newpipe.extractor.StreamingService.ServiceInfo.MediaCapability.AUDIO;
import static org.schabi.newpipe.extractor.StreamingService.ServiceInfo.MediaCapability.LIVE;
import static org.schabi.newpipe.extractor.StreamingService.ServiceInfo.MediaCapability.VIDEO;

import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.channel.ChannelExtractor;
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor;
import org.schabi.newpipe.extractor.comments.CommentsExtractor;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.kiosk.KioskList;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory;
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.services.tv.extractors.TvChannelsKioskExtractor;
import org.schabi.newpipe.extractor.services.tv.extractors.TvSearchExtractor;
import org.schabi.newpipe.extractor.services.tv.extractors.TvStreamExtractor;
import org.schabi.newpipe.extractor.services.tv.linkHandler.TvKioskLinkHandlerFactory;
import org.schabi.newpipe.extractor.services.tv.linkHandler.TvSearchQueryHandlerFactory;
import org.schabi.newpipe.extractor.services.tv.linkHandler.TvStreamLinkHandlerFactory;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.subscription.SubscriptionExtractor;
import org.schabi.newpipe.extractor.suggestion.SuggestionExtractor;

import java.util.EnumSet;

/**
 * VideoPrime: the free-to-air TV channels of Spain (TDT), from the official streams that the
 * broadcasters publish, as listed by TDTChannels. There are no channel pages or playlists:
 * every item is a live channel.
 */
public class TvService extends StreamingService {
    public TvService(final int id) {
        super(id, "TV", EnumSet.of(AUDIO, VIDEO, LIVE));
    }

    @Override
    public String getBaseUrl() {
        return "https://www.tdtchannels.com";
    }

    @Override
    public LinkHandlerFactory getStreamLHFactory() {
        return TvStreamLinkHandlerFactory.getInstance();
    }

    @Override
    public ListLinkHandlerFactory getChannelLHFactory() {
        return null;
    }

    @Override
    public ListLinkHandlerFactory getChannelTabLHFactory() {
        return null;
    }

    @Override
    public ListLinkHandlerFactory getPlaylistLHFactory() {
        return null;
    }

    @Override
    public SearchQueryHandlerFactory getSearchQHFactory() {
        return TvSearchQueryHandlerFactory.getInstance();
    }

    @Override
    public ListLinkHandlerFactory getCommentsLHFactory() {
        return null;
    }

    @Override
    public SearchExtractor getSearchExtractor(final SearchQueryHandler queryHandler) {
        return new TvSearchExtractor(this, queryHandler);
    }

    @Override
    public SuggestionExtractor getSuggestionExtractor() {
        return null;
    }

    @Override
    public SubscriptionExtractor getSubscriptionExtractor() {
        return null;
    }

    @Override
    public KioskList getKioskList() throws ExtractionException {
        final KioskList list = new KioskList(this);
        final TvKioskLinkHandlerFactory kioskLHF = TvKioskLinkHandlerFactory.getInstance();
        try {
            list.addKioskEntry(
                    (streamingService, url, kioskId) -> new TvChannelsKioskExtractor(
                            TvService.this, kioskLHF.fromUrl(url), kioskId),
                    kioskLHF,
                    TvChannelsKioskExtractor.KIOSK_ID);
            list.setDefaultKiosk(TvChannelsKioskExtractor.KIOSK_ID);
        } catch (final Exception e) {
            throw new ExtractionException(e);
        }
        return list;
    }

    @Override
    public ChannelExtractor getChannelExtractor(final ListLinkHandler linkHandler) {
        return null;
    }

    @Override
    public ChannelTabExtractor getChannelTabExtractor(final ListLinkHandler linkHandler) {
        return null;
    }

    @Override
    public PlaylistExtractor getPlaylistExtractor(final ListLinkHandler linkHandler) {
        return null;
    }

    @Override
    public StreamExtractor getStreamExtractor(final LinkHandler linkHandler) {
        return new TvStreamExtractor(this, linkHandler);
    }

    @Override
    public CommentsExtractor getCommentsExtractor(final ListLinkHandler linkHandler) {
        return null;
    }
}
