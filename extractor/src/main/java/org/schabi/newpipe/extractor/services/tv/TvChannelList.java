package org.schabi.newpipe.extractor.services.tv;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.utils.JsonUtils;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The free-to-air channels of Spain, as listed by the open TDTChannels project
 * (https://github.com/LaQuay/TDTChannels), which collects the official streams that each
 * broadcaster offers on its own website.
 */
public final class TvChannelList {
    public static final String LIST_URL = "https://www.tdtchannels.com/lists/tv.json";
    private static final long CACHE_MILLIS = 60 * 60 * 1000L;

    private static List<TvChannel> cache;
    private static long cacheTime;

    private TvChannelList() {
    }

    /** How a channel can be watched. */
    public enum Format {
        /** An HLS playlist, played inside the app. */
        M3U8,
        /** A YouTube live stream ({@code youtube.com/channel/<id>/live}). */
        YOUTUBE,
        /** A web player (Dailymotion, Twitch embed, ...). */
        WEB
    }

    public static final class TvOption {
        private final Format format;
        private final String url;

        TvOption(@Nonnull final Format format, @Nonnull final String url) {
            this.format = format;
            this.url = url;
        }

        @Nonnull
        public Format getFormat() {
            return format;
        }

        @Nonnull
        public String getUrl() {
            return url;
        }
    }

    public static final class TvChannel {
        private final String id;
        private final String name;
        private final String group;
        @Nullable private final String logo;
        @Nullable private final String web;
        private final boolean onlyInSpain;
        private final List<TvOption> options;

        TvChannel(final String id, final String name, final String group,
                  @Nullable final String logo, @Nullable final String web,
                  final boolean onlyInSpain, final List<TvOption> options) {
            this.id = id;
            this.name = name;
            this.group = group;
            this.logo = logo;
            this.web = web;
            this.onlyInSpain = onlyInSpain;
            this.options = options;
        }

        @Nonnull
        public String getId() {
            return id;
        }

        @Nonnull
        public String getName() {
            return name;
        }

        /** E.g. "Generalistas", "Deportivos" or "Andalucía". */
        @Nonnull
        public String getGroup() {
            return group;
        }

        @Nullable
        public String getLogo() {
            return logo;
        }

        /** The broadcaster's own page of the channel. */
        @Nullable
        public String getWeb() {
            return web;
        }

        /** Whether the broadcaster only allows viewers in Spain. */
        public boolean isOnlyInSpain() {
            return onlyInSpain;
        }

        @Nonnull
        public List<TvOption> getOptions() {
            return options;
        }

        @Nullable
        public TvOption getOption(@Nonnull final Format format) {
            for (final TvOption option : options) {
                if (option.getFormat() == format) {
                    return option;
                }
            }
            return null;
        }

        /** The option VideoPrime prefers: in-app HLS, then YouTube, then a web player. */
        @Nonnull
        public TvOption getBestOption() {
            for (final Format format : Format.values()) {
                final TvOption option = getOption(format);
                if (option != null) {
                    return option;
                }
            }
            throw new IllegalStateException("Channels without options are never listed");
        }

        @Nonnull
        public String getUrl() {
            return TvParsingHelper.getChannelUrl(id);
        }
    }

    /**
     * @return every Spanish channel that can be watched, in the list's order (national groups
     * first, then each region)
     */
    @Nonnull
    public static synchronized List<TvChannel> getChannels()
            throws IOException, ExtractionException {
        final long now = System.currentTimeMillis();
        if (cache == null || now - cacheTime > CACHE_MILLIS) {
            final Response response = NewPipe.getDownloader().get(LIST_URL);
            if (response.responseCode() != 200) {
                throw new ExtractionException(
                        "Could not get the TV channel list: HTTP " + response.responseCode());
            }
            cache = Collections.unmodifiableList(parse(response.responseBody()));
            cacheTime = now;
        }
        return cache;
    }

    @Nonnull
    public static TvChannel getChannel(@Nonnull final String id)
            throws IOException, ExtractionException {
        for (final TvChannel channel : getChannels()) {
            if (channel.getId().equals(id)) {
                return channel;
            }
        }
        throw new ParsingException("No TV channel with id " + id);
    }

    @Nonnull
    static List<TvChannel> parse(@Nonnull final String json) throws ParsingException {
        final JsonObject root = JsonUtils.toJsonObject(json);
        final JsonArray countries = root.getArray("countries");
        final List<TvChannel> channels = new ArrayList<>();
        final Set<String> usedIds = new HashSet<>();

        for (int c = 0; c < countries.size(); c++) {
            final JsonObject country = countries.getObject(c);
            if (!"Spain".equals(country.getString("name"))) {
                continue;
            }
            final JsonArray ambits = country.getArray("ambits");
            for (int a = 0; a < ambits.size(); a++) {
                final JsonObject ambit = ambits.getObject(a);
                final String group = ambit.getString("name", "");
                final JsonArray list = ambit.getArray("channels");
                for (int i = 0; i < list.size(); i++) {
                    final TvChannel channel = parseChannel(list.getObject(i), group, usedIds);
                    if (channel != null) {
                        channels.add(channel);
                    }
                }
            }
        }
        return channels;
    }

    @Nullable
    private static TvChannel parseChannel(final JsonObject json, final String group,
                                          final Set<String> usedIds) {
        final String name = json.getString("name");
        if (name == null || name.isEmpty()) {
            return null;
        }

        final List<TvOption> options = new ArrayList<>();
        final JsonArray jsonOptions = json.getArray("options");
        for (int o = 0; o < jsonOptions.size(); o++) {
            final JsonObject option = jsonOptions.getObject(o);
            final String url = option.getString("url");
            if (url == null || !url.startsWith("http")) {
                continue;
            }
            final String format = option.getString("format", "");
            if ("m3u8".equals(format)) {
                options.add(new TvOption(Format.M3U8, url));
            } else if ("youtube".equals(format)) {
                options.add(new TvOption(Format.YOUTUBE, url));
            } else if ("stream".equals(format)) {
                options.add(new TvOption(Format.WEB, url));
            }
        }
        if (options.isEmpty()) {
            return null; // e.g. channels that need the broadcaster's own app
        }

        final JsonArray extraInfo = json.getArray("extra_info");
        boolean onlyInSpain = false;
        for (int e = 0; e < extraInfo.size(); e++) {
            if ("GEO".equals(extraInfo.getString(e))) {
                onlyInSpain = true;
            }
        }

        String id = slug(group) + "-" + slug(name);
        for (int n = 2; usedIds.contains(id); n++) {
            id = slug(group) + "-" + slug(name) + "-" + n;
        }
        usedIds.add(id);

        return new TvChannel(id, name, group, emptyToNull(json.getString("logo")),
                emptyToNull(json.getString("web")), onlyInSpain,
                Collections.unmodifiableList(options));
    }

    @Nonnull
    static String slug(@Nonnull final String text) {
        final String ascii = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        final String slug = ascii.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return slug.isEmpty() ? "channel" : slug;
    }

    @Nullable
    private static String emptyToNull(@Nullable final String text) {
        return text == null || text.isEmpty() ? null : text;
    }
}
