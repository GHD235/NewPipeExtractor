package org.schabi.newpipe.extractor.services.tv.extractors;

import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ContentNotSupportedException;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.services.tv.TvChannelList;
import org.schabi.newpipe.extractor.services.tv.TvChannelList.TvChannel;
import org.schabi.newpipe.extractor.services.tv.TvChannelList.TvOption;
import org.schabi.newpipe.extractor.services.tv.TvParsingHelper;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.DeliveryMethod;
import org.schabi.newpipe.extractor.stream.Description;
import org.schabi.newpipe.extractor.stream.Stream;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;

/** A TV channel's live broadcast, played from the broadcaster's own HLS playlist. */
public class TvStreamExtractor extends StreamExtractor {
    private TvChannel channel;
    private TvOption option;

    public TvStreamExtractor(final StreamingService service, final LinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        channel = TvChannelList.getChannel(getId());
        option = channel.getOption(TvChannelList.Format.M3U8);
        if (option == null) {
            throw new ContentNotSupportedException(
                    channel.getName() + " can only be watched on its website");
        }
    }

    @Nonnull
    @Override
    public String getName() {
        return channel.getName();
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        return TvParsingHelper.getLogo(channel.getLogo());
    }

    @Nonnull
    @Override
    public Description getDescription() {
        return new Description(channel.getGroup(), Description.Type.PLAIN_TEXT);
    }

    @Nonnull
    @Override
    public String getUploaderUrl() {
        return "";
    }

    @Nonnull
    @Override
    public String getUploaderName() {
        return channel.getGroup();
    }

    @Nonnull
    @Override
    public String getCategory() {
        return channel.getGroup();
    }

    @Nonnull
    @Override
    public String getHlsUrl() {
        return option.getUrl();
    }

    @Override
    public List<AudioStream> getAudioStreams() {
        return List.of();
    }

    @Override
    public List<VideoStream> getVideoStreams() {
        return List.of(new VideoStream.Builder()
                .setId(Stream.ID_UNKNOWN)
                .setContent(option.getUrl(), true)
                .setMediaFormat(MediaFormat.MPEG_4)
                .setDeliveryMethod(DeliveryMethod.HLS)
                .setIsVideoOnly(false)
                .setResolution("")
                .build());
    }

    @Override
    public List<VideoStream> getVideoOnlyStreams() {
        return List.of();
    }

    @Override
    public StreamType getStreamType() {
        return StreamType.LIVE_STREAM;
    }
}
