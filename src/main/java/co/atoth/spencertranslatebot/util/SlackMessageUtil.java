package co.atoth.spencertranslatebot.util;

import co.atoth.spencertranslatebot.translation.Lang;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SlackMessageUtil {

    private static final Pattern matchUserIdPattern = Pattern.compile("(<)(.*?)(>)");
    private static final String matchSmileyPattern = "(:)(.*?)(:)";
    public static final String SLACK_LINE_SEPARATOR = "\n";

    public static String getBold(String string){
        return "*" + string + "*";
    }

    public static String replaceUserIds(String message, SlackSession session){

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

    public static String getFlagForLang(Lang lang){
        return ":flag-"+lang.country+":";
    }

    public static String removeSmileys(String message){
        return message.replaceAll(matchSmileyPattern, "");
    }

}