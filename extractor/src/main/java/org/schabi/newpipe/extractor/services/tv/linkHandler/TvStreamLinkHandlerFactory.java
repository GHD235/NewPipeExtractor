package org.schabi.newpipe.extractor.services.tv.linkHandler;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory;
import org.schabi.newpipe.extractor.services.tv.TvParsingHelper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A TV channel: {@code https://www.tdtchannels.com/tv/<channel id>}. */
public final class TvStreamLinkHandlerFactory extends LinkHandlerFactory {
    private static final TvStreamLinkHandlerFactory INSTANCE = new TvStreamLinkHandlerFactory();
    private static final Pattern PATTERN = Pattern.compile(
            "^https?://(?:www\\.)?tdtchannels\\.com/tv/([a-z0-9-]+)/?$");

    private TvStreamLinkHandlerFactory() {
    }

    public static TvStreamLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getId(final String url) throws ParsingException {
        final Matcher matcher = PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new ParsingException("Not a TV channel URL: " + url);
    }

    @Override
    public String getUrl(final String id) {
        return TvParsingHelper.getChannelUrl(id);
    }

    @Override
    public boolean onAcceptUrl(final String url) {
        return PATTERN.matcher(url).find();
    }
}
