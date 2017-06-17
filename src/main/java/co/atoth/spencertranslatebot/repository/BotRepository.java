package co.atoth.spencertranslatebot.repository;

import java.util.Set;

/**
 * Should be used in the context of a slack team
 */
public interface BotRepository {

    byte DEFAULT_MIN_CONFIDENCE  = 30;

    boolean isActiveOnChannel(String channelId);
    boolean activateOnChannel(String channelId);
    boolean deactivateOnChannel(String channelId);
    Set<String> getActiveChannelIds();

    void saveMinimumConfidence(byte confidence);
    byte getMinimumConfidence();

}