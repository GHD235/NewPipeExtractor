package org.schabi.newpipe.extractor.services.twitch;

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
import org.schabi.newpipe.extractor.services.twitch.extractors.TwitchChannelExtractor;
import org.schabi.newpipe.extractor.services.twitch.extractors.TwitchChannelTabExtractor;
import org.schabi.newpipe.extractor.services.twitch.extractors.TwitchLiveKioskExtractor;
import org.schabi.newpipe.extractor.services.twitch.extractors.TwitchSearchExtractor;
import org.schabi.newpipe.extractor.services.twitch.extractors.TwitchStreamExtractor;
import org.schabi.newpipe.extractor.services.twitch.linkHandler.TwitchChannelLinkHandlerFactory;
import org.schabi.newpipe.extractor.services.twitch.linkHandler.TwitchKioskLinkHandlerFactory;
import org.schabi.newpipe.extractor.services.twitch.linkHandler.TwitchSearchQueryHandlerFactory;
import org.schabi.newpipe.extractor.services.twitch.linkHandler.TwitchStreamLinkHandlerFactory;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.subscription.SubscriptionExtractor;
import org.schabi.newpipe.extractor.suggestion.SuggestionExtractor;

import java.util.EnumSet;

/**
 * Twitch: live channels, past broadcasts and channel pages, without an account.
 */
public class TwitchService extends StreamingService {
    public TwitchService(final int id) {
        super(id, "Twitch", EnumSet.of(AUDIO, VIDEO, LIVE));
    }

    @Override
    public String getBaseUrl() {
        return TwitchParsingHelper.BASE_URL;
    }

    @Override
    public LinkHandlerFactory getStreamLHFactory() {
        return TwitchStreamLinkHandlerFactory.getInstance();
    }

    @Override
    public ListLinkHandlerFactory getChannelLHFactory() {
        return TwitchChannelLinkHandlerFactory.getInstance();
    }

    @Override
    public ListLinkHandlerFactory getChannelTabLHFactory() {
        // a single "videos" tab, whose URL is the channel URL
        return TwitchChannelLinkHandlerFactory.getInstance();
    }

    @Override
    public ListLinkHandlerFactory getPlaylistLHFactory() {
        return null;
    }

    @Override
    public SearchQueryHandlerFactory getSearchQHFactory() {
        return TwitchSearchQueryHandlerFactory.getInstance();
    }

    @Override
    public ListLinkHandlerFactory getCommentsLHFactory() {
        return null;
    }

    @Override
    public SearchExtractor getSearchExtractor(final SearchQueryHandler queryHandler) {
        return new TwitchSearchExtractor(this, queryHandler);
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
        final TwitchKioskLinkHandlerFactory kioskLHF = TwitchKioskLinkHandlerFactory.getInstance();
        try {
            list.addKioskEntry(
                    (streamingService, url, kioskId) -> new TwitchLiveKioskExtractor(
                            TwitchService.this, kioskLHF.fromUrl(url), kioskId),
                    kioskLHF,
                    TwitchLiveKioskExtractor.KIOSK_ID);
            list.setDefaultKiosk(TwitchLiveKioskExtractor.KIOSK_ID);
        } catch (final Exception e) {
            throw new ExtractionException(e);
        }
        return list;
    }

    @Override
    public ChannelExtractor getChannelExtractor(final ListLinkHandler linkHandler) {
        return new TwitchChannelExtractor(this, linkHandler);
    }

    @Override
    public ChannelTabExtractor getChannelTabExtractor(final ListLinkHandler linkHandler) {
        return new TwitchChannelTabExtractor(this, linkHandler);
    }

    @Override
    public PlaylistExtractor getPlaylistExtractor(final ListLinkHandler linkHandler) {
        return null;
    }

    @Override
    public StreamExtractor getStreamExtractor(final LinkHandler linkHandler) {
        return new TwitchStreamExtractor(this, linkHandler);
    }

    @Override
    public CommentsExtractor getCommentsExtractor(final ListLinkHandler linkHandler) {
        return null;
    }
}
