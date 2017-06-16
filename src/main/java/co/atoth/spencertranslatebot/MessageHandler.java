package co.atoth.spencertranslatebot;

import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(SpencerTranslateBotMain.class);

    private static String helpMsg = getHelpMsg();
    private static int minCert = 30;
    private static Set<String> activeSlackChannelIds = new LinkedHashSet<>();

    public boolean activate(SlackChannel channel){
        if(channel.getId() == null || channel.getType() == SlackChannel.SlackChannelType.INSTANT_MESSAGING){
            return false;
        }
        return activeSlackChannelIds.add(channel.getId());
    }

    public boolean deactivate(SlackChannel channel){
        return activeSlackChannelIds.remove(channel.getId());
    }

    public boolean isActive(SlackChannel channel){
        return activeSlackChannelIds.contains(channel.getId());
    }

    public Collection <SlackChannel> getActiveChannels(SlackSession session){
        List<SlackChannel> channels = new ArrayList<>();
        for(String channelId : activeSlackChannelIds){
            SlackChannel channel = session.findChannelById(channelId);
            if(channel != null){
                channels.add(channel);
            }
        }
        return channels;
    }

    public String getTranslation(SlackSession session, SlackUser sender, String messageContent){

        //Replace user ids with usernames
        messageContent = replaceUserIds(messageContent, session);
        messageContent = removeSmileys(messageContent);

        String newMessage = TranslateUtil.translateIfNeeded(messageContent, minCert);

        //Prepend the sender
        if( newMessage != null){
            return "*" +  sender.getUserName() +":* "+ newMessage;
        }

        return null;
    }

    public String parseCommands(SlackSession session, SlackChannel channel, String message){

        String botUserId = session.sessionPersona().getId();
        String target = "<@" + botUserId + ">";

        if(message.contains(target)){
            message = message.replace(target, "").trim();

            try {
                if(matchCmd(message, "on") != null){
                    if(activate(channel)) {
                        return "> *on for: " + channel.getName() + "*";
                    } else {
                        return "> *won't work here*";
                    }
                }

                if(matchCmd(message, "off") != null){
                    if(isActive(channel)) {
                        deactivate(channel);
                        return "> *off for: " + channel.getName() + "*";
                    } else {
                        return "> *was not active on this channel*";
                    }
                }

                if(matchCmd(message, "status") != null){

                    String activeChannels =
                                    getActiveChannels(session)
                                    .stream()
                                    .map(SlackChannel::getName)
                                    .collect(Collectors.joining(", "));

                    if(activeChannels.isEmpty()){
                        activeChannels = "*none*";
                    }

                    String languages = Arrays.stream(TranslateUtil.Lang.values())
                            .map(Enum::name)
                            .collect(Collectors.joining(", "));

                    return "> *status*\n" +
                            "supported languages: " + languages  + "\n" +
                            "minimum certainty: " + minCert + "%\n" +
                            "active channels: " + activeChannels;
                }

                if(matchCmd(message, "help") != null){
                    return "> *help* \n" + helpMsg;
                }

                if(matchCmd(message, "meta") != null){
                    return "> *meta* \n" + getMetaFromManifest();
                }

                String[] minCertWild = matchCmd(message, "setMinCert", "*");
                if(minCertWild != null){
                    int minCert = Integer.parseInt(minCertWild[0]);;
                    return "> *setMinCert*\n New minimum certainty " + minCert;
                }
            } catch (Exception ex) {
                //Go away
            }
        }
        return null;
    }

    private static String[] matchCmd(String message, String... inputCmds){
        String[] parsedCmds = message.split("\\s+");

        List<String> wildcards = new ArrayList<>();

        if(parsedCmds.length != inputCmds.length){
            return null;
        }

        for(int i = 0; i < parsedCmds.length; i++){

            String cmd = inputCmds[i].trim().toLowerCase();
            String parsedCmd = parsedCmds[i].trim().toLowerCase();

            // wildcards
            if("*".equals(cmd)){
                wildcards.add(parsedCmd);
                continue;
            }

            if(!parsedCmds[i].equals(inputCmds[i])){
                return null;
            }
        }
        return wildcards.toArray(new String[]{});
    }

    private static String getHelpMsg(){
        StringBuilder helpMsg = new StringBuilder();
        helpMsg.append("status - print status\n");
        helpMsg.append("on - activate on current channel \n");
        helpMsg.append("off - deactivate on current channel\n");
        helpMsg.append("meta - print bot metadata\n");
        helpMsg.append("setMinCert (INT range (0,100) - set minimum certainty for language detection\n");
        return helpMsg.toString();
    }

    private static final Pattern matchUserIdPattern = Pattern.compile("(<)(.*?)(>)");
    private static final String matchSmileyPattern = "(:)(.*?)(:)";

    public String replaceUserIds(String message, SlackSession session){

        Matcher m = matchUserIdPattern.matcher(message);
        while (m.find()) {
            String match = m.group();
            String originalMatch = match;
            match = match.replace("<", "");
            match = match.replace(">", "");
            match = match.replace("@", "");

            //slack get username by id

            SlackUser user = session.findUserById(match);
            if(user != null){
                String username = user.getUserName();
                message = message.replaceFirst(originalMatch, "@" + username);
            }
        }
        return message;
    }

    public String removeSmileys(String message){
        return message.replaceAll(matchSmileyPattern, "");
    }

    public String getMetaFromManifest(){

        Class clazz = MessageHandler.class;
        String className = clazz.getSimpleName() + ".class";
        String classPath = clazz.getResource(className).toString();
        if (!classPath.startsWith("jar")) {
            // Class not from JAR
            return null;
        }

        String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
        Manifest manifest = null;
        try {
            manifest = new Manifest(new URL(manifestPath).openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        Attributes attr = manifest.getMainAttributes();

        StringBuilder metaBuilder = new StringBuilder();

        attr.entrySet().stream().forEach(objectObjectEntry -> {
            metaBuilder.append(objectObjectEntry.getKey() + ": " + objectObjectEntry.getValue() + "\n");
        });

        return metaBuilder.toString();
    }

}