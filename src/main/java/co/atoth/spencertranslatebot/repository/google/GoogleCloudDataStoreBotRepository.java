package co.atoth.spencertranslatebot.repository.google;

import co.atoth.spencertranslatebot.repository.BotRepository;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.datastore.Datastore;

import java.util.HashSet;
import java.util.Set;

public class GoogleCloudDataStoreBotRepository implements BotRepository {

    private Datastore datastore;

    /**
     * Currently only works with service account credentials
     * //TODO implement me please
     */
    public GoogleCloudDataStoreBotRepository(ServiceAccountCredentials credentials, String googleProjectName, String slackTeamName){
        /*
        Datastore datastore = DatastoreOptions.newBuilder()
                .setNamespace(slackTeamName)
                .setCredentials(credentials)
                .setProjectId(googleProjectName)
                .build()
                .getService();
        */
    }

    @Override
    public boolean isActiveOnChannel(String channelId) {
        return false;
    }

    @Override
    public boolean activateOnChannel(String channelId) {
//        KeyFactory keyFactory = datastore.newKeyFactory().setKind("activeChannel");
//
//        Entity task = Entity.newBuilder(keyFactory.newKey("test-channel"))
//                .set("channelId", StringValue.newBuilder("test-channel").setExcludeFromIndexes(true).build())
//                .set("created", Timestamp.now())
//                .set("channelName", Timestamp.now())
//                .set("done", false)
//                .build();

        return false;
    }

    @Override
    public boolean deactivateOnChannel(String channelId) {
        return false;
    }

    @Override
    public Set<String> getActiveChannelIds() {
        return new HashSet<>();
    }

    @Override
    public void saveMinimumConfidence(byte confidence) {}

    @Override
    public byte getMinimumConfidence() {
        return BotRepository.DEFAULT_MIN_CONFIDENCE;
    }
}
