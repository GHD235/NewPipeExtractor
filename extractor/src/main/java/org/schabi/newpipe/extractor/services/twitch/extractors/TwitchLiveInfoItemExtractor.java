package org.schabi.newpipe.extractor.services.twitch.extractors;

import com.grack.nanojson.JsonObject;

import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.services.twitch.TwitchParsingHelper;
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor;
import org.schabi.newpipe.extractor.stream.StreamType;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** A channel that is live right now, built from {@link TwitchGqlFields#STREAM}. */
public class TwitchLiveInfoItemExtractor implements StreamInfoItemExtractor {
    private final JsonObject stream;
    private final JsonObject broadcaster;

    public TwitchLiveInfoItemExtractor(@Nonnull final JsonObject stream) {
        this.stream = stream;
        this.broadcaster = stream.getObject("broadcaster");
    }

    @Override
    public String getName() {
        return stream.getString("title");
    }

    @Override
    public String getUrl() {
        return TwitchParsingHelper.getChannelUrl(broadcaster.getString("login"));
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        return TwitchParsingHelper.getImages(stream.getString("previewImageURL"), 640, 360);
    }

    @Override
    public StreamType getStreamType() {
        return StreamType.LIVE_STREAM;
    }

    @Override
    public boolean isAd() {
        return false;
    }

    @Override
    public long getDuration() {
        return -1;
    }

    @Override
    public long getViewCount() {
        return stream.getLong("viewersCount", -1);
    }

    @Override
    public String getUploaderName() {
        return broadcaster.getString("displayName");
    }

    @Override
    public String getUploaderUrl() {
        return TwitchParsingHelper.getChannelUrl(broadcaster.getString("login")) + "/videos";
    }

    @Nonnull
    @Override
    public List<Image> getUploaderAvatars() {
        return TwitchParsingHelper.getImages(broadcaster.getString("profileImageURL"), 150, 150);
    }

    @Override
    public boolean isUploaderVerified() {
        return false;
    }

    @Override
    public String getTextualUploadDate() {
        return stream.getString("createdAt");
    }

    @Nullable
    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        return TwitchParsingHelper.parseDate(stream.getString("createdAt"));
    }

    @Override
    public String getShortDescription() {
        return stream.getObject("game").getString("displayName");
    }
}
