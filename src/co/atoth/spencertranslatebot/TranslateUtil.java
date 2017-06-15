package co.atoth.spencertranslatebot;

import com.google.cloud.translate.Detection;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.Translate.TranslateOption;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TranslateUtil {

    private static final Logger logger = LoggerFactory.getLogger(TranslateUtil.class);

    public static String translateIfNeeded(String message, int minimumConfidence){

        Translate translate = createTranslateService();

        //Check romanian or hebrew
        List<Detection> detections = translate.detect(ImmutableList.of(message));

        logger.debug("Detecting lang for message: " + message);
        for (Detection detection : detections) {
            String lang = detection.getLanguage();
            float conf = detection.getConfidence();

            logger.debug("Detected lang " + lang + " with confidence " + conf);

            if("ro".equals(lang) && conf >= (minimumConfidence / 100f)){
                return translateText(message,"ro", "en");
            } else if ("iw".equals(lang) && conf >= (minimumConfidence / 100f)){
                return translateText(message,"iw", "en");
            }
        }

        return message;
    }

    public static String translateText(
            String sourceText,
            String sourceLang,
            String targetLang){

        Translate translate = createTranslateService();

        TranslateOption srcLang = TranslateOption.sourceLanguage(sourceLang);
        TranslateOption tgtLang = TranslateOption.targetLanguage(targetLang);

        Translation translation = translate.translate(sourceText, srcLang, tgtLang);

        return translation.getTranslatedText();
    }

    public static Translate createTranslateService() {
        return TranslateOptions
                .newBuilder()
                .setApiKey(SpencerTranslateBot.GOOGLE_API_KEY)
                .build()
                .getService();
    }

}