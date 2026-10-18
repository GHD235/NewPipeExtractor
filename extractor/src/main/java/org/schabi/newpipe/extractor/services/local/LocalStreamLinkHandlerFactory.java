package org.schabi.newpipe.extractor.services.local;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory;

/** A local media file: its {@code file://} or {@code content://} URI is both URL and id. */
public final class LocalStreamLinkHandlerFactory extends LinkHandlerFactory {
    private static final LocalStreamLinkHandlerFactory INSTANCE =
            new LocalStreamLinkHandlerFactory();

    private LocalStreamLinkHandlerFactory() {
    }

    public static LocalStreamLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    public static boolean isLocalUrl(final String url) {
        return url != null && (url.startsWith("file:") || url.startsWith("content:"));
    }

    @Override
    public String getId(final String url) throws ParsingException {
        if (!isLocalUrl(url)) {
            throw new ParsingException("Not a local file: " + url);
        }
        return url;
    }

    @Override
    public String getUrl(final String id) {
        return id;
    }

    @Override
    public boolean onAcceptUrl(final String url) {
        return isLocalUrl(url);
    }
}
