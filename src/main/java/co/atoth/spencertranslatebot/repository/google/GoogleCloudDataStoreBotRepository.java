package co.atoth.spencertranslatebot.repository.google;

import co.atoth.spencertranslatebot.repository.BotRepository;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;

import java.util.LinkedHashSet;
import java.util.Set;

public class GoogleCloudDataStoreBotRepository implements BotRepository {

    private static final String KIND_ACTIVE_CHANNEL = "activeChannel";

    private static final String KIND_CONFIDENCE = "confidence";
    private static final String CONFIDENCE_KEY = "CONFIDENCE_KEY";

    private Set<String> activeSlackChannelIds = new LinkedHashSet<>();
    private byte minimumConfidece = BotRepository.DEFAULT_MIN_CONFIDENCE;

    private Datastore datastore;

    /**
     * Currently only works with service account credentials
     */
    public GoogleCloudDataStoreBotRepository(ServiceAccountCredentials credentials, String googleProjectName, String slackTeamId) {
        this.datastore = DatastoreOptions.newBuilder()
                .setNamespace(slackTeamId)
                .setCredentials(credentials)
                .setProjectId(googleProjectName)
                .build()
                .getService();

        loadActiveChannels();
        loadMinimumConfidence();
    }

    @Override
    public boolean isActiveOnChannel(String channelId) {
        //KeyFactory keyFactory = datastore.newKeyFactory().setKind(KIND_ACTIVE_CHANNEL);
        //return datastore.get(keyFactory.newKey(channelId)) != null;

        return activeSlackChannelIds.contains(channelId);
    }

    @Override
    public boolean activateOnChannel(String channelId) {
        if (isActiveOnChannel(channelId)) {
            return false;
        }

        KeyFactory keyFactory = datastore.newKeyFactory().setKind(KIND_ACTIVE_CHANNEL);
        Key key = keyFactory.newKey(channelId);

        datastore.put(
                Entity.newBuilder(key)
                        .set("created", Timestamp.now())
                        .build()
        );

        loadActiveChannels();
        return true;
    }

    @Override
    public boolean deactivateOnChannel(String channelId) {
        if (!isActiveOnChannel(channelId)) {
            return false;
        }

        KeyFactory keyFactory = datastore.newKeyFactory().setKind(KIND_ACTIVE_CHANNEL);
        datastore.delete(keyFactory.newKey(channelId));

        loadActiveChannels();
        return true;
    }

    @Override
    public Set<String> getActiveChannelIds() {
        //loadActiveChannels();
        return activeSlackChannelIds;
    }

    @Override
    public void saveMinimumConfidence(byte confidence) {
        KeyFactory keyFactory = datastore.newKeyFactory().setKind(KIND_CONFIDENCE);
        Key key = keyFactory.newKey(CONFIDENCE_KEY);
        Entity entity = datastore.get(keyFactory.newKey(CONFIDENCE_KEY));

        if (entity == null) {
            datastore.put(Entity.newBuilder(key)
                    .set("confidence", confidence)
                    .set("created", Timestamp.now())
                    .build());
        } else {
            Transaction transaction = datastore.newTransaction();
            try {
                transaction.put(Entity.newBuilder(entity).set("confidence", confidence).build());
                transaction.commit();
                minimumConfidece = confidence;
            } finally {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
            }
        }
    }

    @Override
    public byte getMinimumConfidence() {
        return minimumConfidece;
    }

    //I'm greedy, requests are expensive

    private void loadActiveChannels() {
        activeSlackChannelIds.clear();
        EntityQuery query = EntityQuery.newEntityQueryBuilder().setKind(KIND_ACTIVE_CHANNEL).build();
        datastore.run(query).forEachRemaining(entity -> {
            activeSlackChannelIds.add(entity.getKey().getName());
        });
    }

    private void loadMinimumConfidence() {

        KeyFactory keyFactory = datastore.newKeyFactory().setKind(KIND_CONFIDENCE);
        Key key = keyFactory.newKey(CONFIDENCE_KEY);
        Entity entity = datastore.get(keyFactory.newKey(CONFIDENCE_KEY));

        if (entity == null) {
            Entity.newBuilder(key)
                    .set("confidence", BotRepository.DEFAULT_MIN_CONFIDENCE)
                    .set("created", Timestamp.now())
                    .build();
            minimumConfidece = BotRepository.DEFAULT_MIN_CONFIDENCE;
        } else {
            minimumConfidece = (byte) entity.getLong("confidence");
        }
    }

}
