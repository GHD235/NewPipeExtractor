package org.schabi.newpipe.extractor.services.twitch.extractors;

/**
 * GraphQL selections shared by the Twitch extractors, so that the info item extractors always
 * receive the fields they need.
 */
final class TwitchGqlFields {
    /** Fields of a {@code Video} (past broadcast, highlight or upload). */
    static final String VIDEO = "id title lengthSeconds viewCount publishedAt status"
            + " previewThumbnailURL(width: 640, height: 360)"
            + " owner { login displayName profileImageURL(width: 150) }"
            + " game { displayName }";

    /** Fields of a live {@code Stream}. */
    static final String STREAM = "id title viewersCount createdAt"
            + " previewImageURL(width: 640, height: 360)"
            + " game { displayName }"
            + " broadcaster { login displayName profileImageURL(width: 150) }";

    /** Fields of a {@code User} (channel). */
    static final String USER = "id login displayName description"
            + " profileImageURL(width: 300)"
            + " followers { totalCount }"
            + " roles { isPartner }"
            + " stream { id }";

    private TwitchGqlFields() {
    }
}
