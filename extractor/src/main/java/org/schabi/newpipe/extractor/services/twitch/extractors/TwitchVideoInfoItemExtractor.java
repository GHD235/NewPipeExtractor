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

/** A past Twitch video, built from {@link TwitchGqlFields#VIDEO}. */
public class TwitchVideoInfoItemExtractor implements StreamInfoItemExtractor {
    private final JsonObject video;

    public TwitchVideoInfoItemExtractor(@Nonnull final JsonObject video) {
        this.video = video;
    }

    @Override
    public String getName() {
        return video.getString("title");
    }

    @Override
    public String getUrl() {
        return TwitchParsingHelper.getVideoUrl(video.getString("id"));
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        return TwitchParsingHelper.getImages(video.getString("previewThumbnailURL"), 640, 360);
    }

    @Override
    public StreamType getStreamType() {
        return StreamType.VIDEO_STREAM;
    }

    @Override
    public boolean isAd() {
        return false;
    }

    @Override
    public long getDuration() {
        return video.getLong("lengthSeconds", -1);
    }

    @Override
    public long getViewCount() {
        return video.getLong("viewCount", -1);
    }

    @Override
    public String getUploaderName() {
        return video.getObject("owner").getString("displayName");
    }

    @Override
    public String getUploaderUrl() {
        final String login = video.getObject("owner").getString("login");
        return login == null ? null : TwitchParsingHelper.getChannelUrl(login) + "/videos";
    }

    @Nonnull
    @Override
    public List<Image> getUploaderAvatars() {
        return TwitchParsingHelper.getImages(
                video.getObject("owner").getString("profileImageURL"), 150, 150);
    }

    @Override
    public boolean isUploaderVerified() {
        return false;
    }

    @Override
    public String getTextualUploadDate() {
        return video.getString("publishedAt");
    }

    @Nullable
    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        return TwitchParsingHelper.parseDate(video.getString("publishedAt"));
    }
}
