package com.example.smartchatbot.common;

import android.content.Context;
import android.util.Log;

import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.HashMap;
import java.util.Map;

public class LanguageManager {

    private static final String TAG = "LanguageManager";

    // Detect language (Hindi = hi, Punjabi = pa, English = en)
    public static void detectLanguage(String text, OnLanguageDetected listener) {
        LanguageIdentifier identifier = LanguageIdentification.getClient();
        identifier.identifyLanguage(text)
                .addOnSuccessListener(lang -> {
                    if (lang.equals("und")) {
                        listener.onDetected("en"); // default to English
                    } else {
                        listener.onDetected(lang);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Language detection failed", e);
                    listener.onDetected("en");
                });
    }

    // Translate text from sourceLang → targetLang
    public static void translateText(String text, String sourceLang, String targetLang, OnTranslationComplete listener) {
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(getMLKitLangCode(sourceLang))
                .setTargetLanguage(getMLKitLangCode(targetLang))
                .build();

        Translator translator = Translation.getClient(options);
        translator.downloadModelIfNeeded()
                .addOnSuccessListener(unused -> translator.translate(text)
                        .addOnSuccessListener(listener::onTranslated)
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Translation failed", e);
                            listener.onTranslated(text); // fallback
                        }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Model download failed", e);
                    listener.onTranslated(text); // fallback
                });
    }

    // Map ISO codes to MLKit
    private static String getMLKitLangCode(String code) {
        Map<String, String> map = new HashMap<>();
        map.put("en", TranslateLanguage.ENGLISH);
        map.put("hi", TranslateLanguage.HINDI);
        map.put("pa", "pa");

        return map.getOrDefault(code, TranslateLanguage.ENGLISH);
    }

    // Interfaces
    public interface OnLanguageDetected {
        void onDetected(String langCode);
    }

    public interface OnTranslationComplete {
        void onTranslated(String translatedText);
    }
}
