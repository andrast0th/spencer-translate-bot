package co.atoth.spencertranslatebot;

import co.atoth.spencertranslatebot.repository.BotRepository;
import co.atoth.spencertranslatebot.translation.Lang;
import co.atoth.spencertranslatebot.translation.TranslationService;
import co.atoth.spencertranslatebot.util.BotInfo;
import co.atoth.spencertranslatebot.util.SlackMessageUtil;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackPreparedMessage;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class MessageHandler implements SlackMessagePostedListener{

    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);
    private static String helpMsg = getHelpMsg();

    private static final String MASTER_USERNAME = "slbruce";
    private static final String MASTER_GREET = "*:crown: Long live Master Spencer! :crown: :bow: I exist to serve you. :bow:*";

    private BotRepository botRepository;
    private TranslationService translationService;
    private SlackSession slackSession;

    MessageHandler(BotRepository repository, TranslationService translationService){
        this.botRepository = repository;
        this.translationService = translationService;
    }

    @Override
    public void onEvent(SlackMessagePosted event, SlackSession session) {
        this.slackSession = session;
        SlackChannel channel = event.getChannel();
        SlackUser sender = event.getSender();
        String message  = event.getMessageContent();

        //Self filter
        if(slackSession.sessionPersona().getId().equals(event.getSender().getId())){
            return;
        }

        //Check if command parsing needed
        String cmdReply = parseCommands(channel, message);
        if(cmdReply != null){
            if(sender.getUserName().equals(MASTER_USERNAME)){
                cmdReply = MASTER_GREET + "\n" + cmdReply;
            }
            slackSession.sendMessage(channel, cmdReply);
        } else {

            //Check if active on this channel
            if(!botRepository.isActiveOnChannel(channel.getId())){
                return;
            }

            //Check if translation need
            String reply = getTranslation(sender, message);
            if(reply != null){
                sendReplyMessage(reply, event);
            }
        }
    }

    private String getTranslation(SlackUser sender, String messageContent){

        //Replace user ids with usernames
        messageContent = SlackMessageUtil.replaceUserIds(messageContent, slackSession);
        messageContent = SlackMessageUtil.removeSmileys(messageContent);

        String newMessage = translationService.translateIfNeeded(messageContent, botRepository.getMinimumConfidence());

        //Prepend the sender
        if( newMessage != null){
            return "*" +  sender.getUserName() +":* "+ newMessage;
        }

        return null;
    }

    private String parseCommands(SlackChannel channel, String message){

        String botUserId = slackSession.sessionPersona().getId();
        String target = "<@" + botUserId + ">";

        if(message.contains(target)){
            message = message.replace(target, "").trim();

            try {
                if(matchCmd(message, "on") != null){
                    if(botRepository.activateOnChannel(channel.getId())) {
                        return "> *on for: " + channel.getName() + "*";
                    } else {
                        return "> *won't work here*";
                    }
                }

                if(matchCmd(message, "off") != null){
                    if(botRepository.isActiveOnChannel(channel.getId())) {
                        botRepository.deactivateOnChannel(channel.getId());
                        return "> *off for: " + channel.getName() + "*";
                    } else {
                        return "> *was not active on this channel*";
                    }
                }

                if(matchCmd(message, "status") != null){

                    String activeChannels =
                                    getActiveChannels()
                                    .stream()
                                    .map(SlackChannel::getName)
                                    .collect(Collectors.joining(", "));

                    if(activeChannels.isEmpty()){
                        activeChannels = "*none*";
                    }

                    String languages = Arrays.stream(Lang.values())
                            .map(Enum::name)
                            .collect(Collectors.joining(", "));

                    return "> *status*\n" +
                            "supported languages: " + languages  + "\n" +
                            "minimum confidence: " + botRepository.getMinimumConfidence() + "%\n" +
                            "active channels: " + activeChannels;
                }

                if(matchCmd(message, "help") != null){
                    return "> *help* \n" + helpMsg;
                }

                if(matchCmd(message, "botInfo") != null){
                    String buildInfo = BotInfo.getInfo("\n");
                    return "> *botInfo* \n " + buildInfo;
                }

                String[] minConfWild = matchCmd(message, "setMinConf", "*");
                if(minConfWild != null){
                    try{
                        byte minConf = Byte.parseByte(minConfWild[0]);

                        if(minConf < 0 || minConf > 100){
                            return "> *setMinConfidece* \n Invalid minimum confidence, must be a number between 0 and 100";
                        }

                        botRepository.saveMinimumConfidence(minConf);
                        return "> *setMinConf*\n New minimum confidence " + minConf;

                    } catch (NumberFormatException ex){
                        return "> *setMinConfidece* \n Invalid minimum confidence, must be a number between 0 and 100";
                    }
                }
            } catch (Exception ex) {
                //Go away
                logger.error("Failed to parse command: " + ex);
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
        return "status - print status\n" +
               "on - activate on current channel \n" +
               "off - deactivate on current channel\n" +
               "botInfo - print bot build information and current host\n" +
               "setMinConf (INT range (0,100) - set minimum confidence for language detection\n";
    }

    private Collection<SlackChannel> getActiveChannels(){
        List<SlackChannel> channels = new ArrayList<>();
        for(String channelId : botRepository.getActiveChannelIds()){
            SlackChannel channel = slackSession.findChannelById(channelId);
            if(channel != null){
                channels.add(channel);
            }
        }
        return channels;
    }

    private void sendReplyMessage(String message, SlackMessagePosted event){

        SlackChannel channel = event.getChannel();

        String replyTimeStamp;
        if(event.getThreadTimestamp() != null){
            replyTimeStamp = event.getThreadTimestamp();
        } else {
            replyTimeStamp = event.getTimeStamp();
        }

        SlackPreparedMessage msg =
                new SlackPreparedMessage.Builder()
                        .withThreadTimestamp(replyTimeStamp)
                        .withMessage(message)
                        .build();

        slackSession.sendMessage(channel, msg);
    }

}