package co.atoth.spencertranslatebot.translation;

import com.google.cloud.translate.Detection;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.Translate.TranslateOption;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import com.google.common.collect.ImmutableList;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.List;

import static co.atoth.spencertranslatebot.util.SlackMessageUtil.getBold;
import static co.atoth.spencertranslatebot.util.SlackMessageUtil.getFlagForLang;

public class TranslationService {

    private static final Logger logger = LoggerFactory.getLogger(TranslationService.class);
    private static final DecimalFormat df = new DecimalFormat("#");

    private static final Lang[] translatedLanguages = new Lang[]{Lang.IW, Lang.RO};
    private static final String TRANSLATE_TO_LANG = "en";

    private Translate translate;

    public TranslationService(String googleTranslateApiKey){
        this.translate =
                TranslateOptions
                    .newBuilder()
                    .setApiKey(googleTranslateApiKey)
                    .build()
                    .getService();
    }

    public String translateIfNeeded(String message, byte minimumConfidence){

        //Check romanian or hebrew
        List<Detection> detections = translate.detect(ImmutableList.of(message));

        logger.debug("Detecting lang for message: " + message);
        for (Detection detection : detections) {
            String lang = detection.getLanguage();
            float conf = detection.getConfidence() * 100;
            String confStr = df.format(conf) + "%";

            logger.debug("Detected lang " + lang + " with confidence " + conf);
            for(Lang supportedLang : translatedLanguages){
                if(supportedLang.toString().equals(lang) && conf >= minimumConfidence) {
                    String translation = translateText(message, supportedLang.toString(), TRANSLATE_TO_LANG);
                    //Get rid of html char codes
                    translation = Jsoup.parse(translation).text();
                    return getBold(getFlagForLang(supportedLang) + " " + confStr) + "\n" + translation;
                }
            }
        }
        return null;
    }

    private String translateText(
            String sourceText,
            String sourceLang,
            String targetLang){

        TranslateOption srcLang = TranslateOption.sourceLanguage(sourceLang);
        TranslateOption tgtLang = TranslateOption.targetLanguage(targetLang);

        Translation translation = translate.translate(sourceText, srcLang, tgtLang);

        return translation.getTranslatedText();
    }

}