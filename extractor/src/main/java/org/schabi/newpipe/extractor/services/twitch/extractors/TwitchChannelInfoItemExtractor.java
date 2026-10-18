package org.schabi.newpipe.extractor.services.twitch.extractors;

import com.grack.nanojson.JsonObject;

import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.channel.ChannelInfoItemExtractor;
import org.schabi.newpipe.extractor.services.twitch.TwitchParsingHelper;

import java.util.List;

import javax.annotation.Nonnull;

/** A Twitch channel, built from {@link TwitchGqlFields#USER}. */
public class TwitchChannelInfoItemExtractor implements ChannelInfoItemExtractor {
    private final JsonObject user;

    public TwitchChannelInfoItemExtractor(@Nonnull final JsonObject user) {
        this.user = user;
    }

    @Override
    public String getName() {
        return user.getString("displayName");
    }

    @Override
    public String getUrl() {
        return TwitchParsingHelper.getChannelUrl(user.getString("login")) + "/videos";
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        return TwitchParsingHelper.getImages(user.getString("profileImageURL"), 300, 300);
    }

    @Override
    public String getDescription() {
        return user.getString("description");
    }

    @Override
    public long getSubscriberCount() {
        return user.getObject("followers").getLong("totalCount", -1);
    }

    @Override
    public long getStreamCount() {
        return ListExtractor.ITEM_COUNT_UNKNOWN;
    }

    @Override
    public boolean isVerified() {
        return user.getObject("roles").getBoolean("isPartner", false);
    }
}
