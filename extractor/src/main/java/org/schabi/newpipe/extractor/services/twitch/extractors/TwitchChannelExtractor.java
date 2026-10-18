package org.schabi.newpipe.extractor.services.twitch.extractors;

import com.grack.nanojson.JsonObject;

import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.channel.ChannelExtractor;
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.services.twitch.TwitchParsingHelper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

public class TwitchChannelExtractor extends ChannelExtractor {
    private static final String QUERY = "query($login: String!) { user(login: $login) { "
            + TwitchGqlFields.USER + " bannerImageURL } }";

    private JsonObject user;

    public TwitchChannelExtractor(final StreamingService service,
                                  final ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        user = TwitchParsingHelper.gql(QUERY, Map.of("login", getId())).getObject("user");
        if (user.isEmpty()) {
            throw new ContentNotAvailableException("Twitch channel does not exist: " + getId());
        }
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException {
        return user.getString("displayName", getId());
    }

    @Nonnull
    @Override
    public List<Image> getAvatars() {
        return TwitchParsingHelper.getImages(user.getString("profileImageURL"), 300, 300);
    }

    @Nonnull
    @Override
    public List<Image> getBanners() {
        return TwitchParsingHelper.getImages(user.getString("bannerImageURL"), 1200, 380);
    }

    @Override
    public String getFeedUrl() {
        return null;
    }

    @Override
    public long getSubscriberCount() {
        return user.getObject("followers").getLong("totalCount", -1);
    }

    @Override
    public String getDescription() {
        return user.getString("description");
    }

    @Override
    public String getParentChannelName() {
        return "";
    }

    @Override
    public String getParentChannelUrl() {
        return "";
    }

    @Nonnull
    @Override
    public List<Image> getParentChannelAvatars() {
        return List.of();
    }

    @Override
    public boolean isVerified() {
        return user.getObject("roles").getBoolean("isPartner", false);
    }

    @Nonnull
    @Override
    public List<ListLinkHandler> getTabs() throws ParsingException {
        return List.of(new ListLinkHandler(getOriginalUrl(), getUrl(), getId(),
                List.of(ChannelTabs.VIDEOS), ""));
    }
}
