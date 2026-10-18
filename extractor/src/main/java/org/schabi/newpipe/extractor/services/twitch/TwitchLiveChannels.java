package org.schabi.newpipe.extractor.services.twitch;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;

import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.services.twitch.extractors.TwitchLiveInfoItemExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * Which of a set of Twitch channels are live right now, asked to Twitch in one request per
 * hundred channels. Used by VideoPrime's home screen, since a channel feed cannot tell when a
 * broadcast ended.
 */
public final class TwitchLiveChannels {
    private static final int MAX_LOGINS_PER_REQUEST = 100;
    private static final String QUERY = "query($logins: [String!]) {"
            + " users(logins: $logins) { stream { id title viewersCount createdAt"
            + " previewImageURL(width: 640, height: 360) game { displayName }"
            + " broadcaster { login displayName profileImageURL(width: 150) } } } }";

    private TwitchLiveChannels() {
    }

    /**
     * @param logins channel logins (as in {@code twitch.tv/<login>})
     * @return one live stream item per channel that is live, most watched first
     */
    @Nonnull
    public static List<StreamInfoItem> fetch(@Nonnull final List<String> logins)
            throws IOException, ExtractionException {
        final StreamInfoItemsCollector collector =
                new StreamInfoItemsCollector(ServiceList.Twitch.getServiceId());

        for (int from = 0; from < logins.size(); from += MAX_LOGINS_PER_REQUEST) {
            final List<String> chunk = new ArrayList<>(
                    logins.subList(from, Math.min(logins.size(), from + MAX_LOGINS_PER_REQUEST)));
            final JsonArray users = TwitchParsingHelper.gql(QUERY, Map.of("logins", chunk))
                    .getArray("users");
            for (int i = 0; i < users.size(); i++) {
                final JsonObject stream = users.getObject(i).getObject("stream");
                if (!stream.isEmpty()) {
                    collector.commit(new TwitchLiveInfoItemExtractor(stream));
                }
            }
        }

        final List<StreamInfoItem> items = new ArrayList<>(collector.getItems());
        items.sort((a, b) -> Long.compare(b.getViewCount(), a.getViewCount()));
        return items;
    }
}
