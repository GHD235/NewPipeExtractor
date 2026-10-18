package org.schabi.newpipe.extractor.services.tv.extractors;

import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.services.tv.TvChannelList.TvChannel;
import org.schabi.newpipe.extractor.services.tv.TvParsingHelper;
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor;
import org.schabi.newpipe.extractor.stream.StreamType;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** A TV channel in a list: always live, with its group (e.g. "Deportivos") as uploader. */
public class TvChannelInfoItemExtractor implements StreamInfoItemExtractor {
    private final TvChannel channel;

    public TvChannelInfoItemExtractor(@Nonnull final TvChannel channel) {
        this.channel = channel;
    }

    @Override
    public String getName() {
        return channel.getName();
    }

    @Override
    public String getUrl() {
        return channel.getUrl();
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        return TvParsingHelper.getLogo(channel.getLogo());
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
        return -1;
    }

    @Override
    public String getUploaderName() {
        return channel.getGroup();
    }

    @Override
    public String getUploaderUrl() {
        return null;
    }

    @Override
    public boolean isUploaderVerified() {
        return false;
    }

    @Nullable
    @Override
    public String getTextualUploadDate() {
        return null;
    }

    @Nullable
    @Override
    public DateWrapper getUploadDate() {
        return null;
    }
}
