package co.atoth.spencertranslatebot;

import com.google.cloud.translate.Detection;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.Translate.TranslateOption;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import com.google.common.collect.ImmutableList;

import java.io.PrintStream;
import java.util.List;

public class TranslateUtil {

    public static float isRomanian(String text, float minConfidence){
        Translate translate = createTranslateService();
        List<Detection> detections = translate.detect(ImmutableList.of(text));
        for (Detection detection : detections) {
            if(detection.getLanguage().equals("ro") && detection.getConfidence() > minConfidence){
                return detection.getConfidence();
            }
        }
        return -1f;
    }

    /**
     * Translate the source text from source to target language.
     * Make sure that your project is whitelisted.
     *
     * @param sourceText source text to be translated
     * @param sourceLang source language of the text
     * @param targetLang target language of translated text
     * @param out print stream
     */
    public static void translateTextWithOptionsAndModel(
            String sourceText,
            String sourceLang,
            String targetLang,
            PrintStream out) {

        Translate translate = createTranslateService();
        TranslateOption srcLang = TranslateOption.sourceLanguage(sourceLang);
        TranslateOption tgtLang = TranslateOption.targetLanguage(targetLang);

        // Use translate `model` parameter with `base` and `nmt` options.
        TranslateOption model = TranslateOption.model("nmt");

        Translation translation = translate.translate(sourceText, srcLang, tgtLang, model);
        out.printf("Source Text:\n\tLang: %s, Text: %s\n", sourceLang, sourceText);
        out.printf("TranslatedText:\n\tLang: %s, Text: %s\n", targetLang,
                translation.getTranslatedText());
    }

    public static String translateRomanian(String text) {

        Translate translate = createTranslateService();
        TranslateOption srcLang = TranslateOption.sourceLanguage("ro");
        TranslateOption tgtLang = TranslateOption.targetLanguage("en");

        Translation translation = translate.translate(text, srcLang, tgtLang);
        return translation.getTranslatedText();
    }

    /**
     * Create Google Translate API Service.
     *
     * @return Google Translate Service
     */
    public static Translate createTranslateService() {
        return TranslateOptions.newBuilder().setApiKey(SpencerTranslateBot.GOOGLE_API_KEY).build().getService();
    }

}