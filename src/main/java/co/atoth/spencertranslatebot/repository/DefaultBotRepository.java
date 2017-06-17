package co.atoth.spencertranslatebot.repository;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Just store it in memory
 */
public class DefaultBotRepository implements BotRepository {

    private byte minimumConfidence = BotRepository.DEFAULT_MIN_CONFIDENCE;
    private Set<String> activeSlackChannelIds = new LinkedHashSet<>();

    @Override
    public boolean isActiveOnChannel(String channelId) {
        return !(channelId == null || channelId.trim().length() == 0) && activeSlackChannelIds.contains(channelId);
    }

    @Override
    public boolean activateOnChannel(String channelId) {
        return !(channelId == null || channelId.trim().length() == 0) && activeSlackChannelIds.add(channelId);
    }

    @Override
    public boolean deactivateOnChannel(String channelId) {
        return !(channelId == null || channelId.trim().length() == 0) && activeSlackChannelIds.remove(channelId);
    }

    @Override
    public Set<String> getActiveChannelIds() {
        return activeSlackChannelIds;
    }

    @Override
    public void saveMinimumConfidence(byte confidence) {
        this.minimumConfidence = minimumConfidence;
    }

    @Override
    public byte getMinimumConfidence() {
        return minimumConfidence;
    }

}