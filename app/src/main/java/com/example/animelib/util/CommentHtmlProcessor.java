package com.example.animelib.util;

import android.content.Context;
import android.graphics.Color;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.animelib.R;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * Утилита для обработки HTML комментариев с поддержкой спойлеров, форматирования и цитат
 */
public class CommentHtmlProcessor {
    
    private final Context context;
    private final int primaryTextColor;
    private final int accentTextColor;
    private final int backgroundColor;
    
    public CommentHtmlProcessor(Context context) {
        this.context = context;
        this.primaryTextColor = ContextCompat.getColor(context, R.color.white_color);
        this.accentTextColor = ContextCompat.getColor(context, R.color.dt_accent_text_color);
        this.backgroundColor = ContextCompat.getColor(context, R.color.dt_primary_color);
    }
    
    /**
     * Обработать HTML комментарий и создать View элементы
     * @param htmlContent HTML содержимое комментария
     * @param container Контейнер для добавления элементов
     */
    public void processCommentHtml(String htmlContent, LinearLayout container) {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            return;
        }
        
        Log.d("CommentHtmlProcessor", "Processing HTML: " + htmlContent);
        
        // Очищаем контейнер
        container.removeAllViews();
        
        try {
            // Очищаем HTML от экранированных кавычек
            String cleanedHtml = htmlContent.replace("\\\"", "\"");
            Log.d("CommentHtmlProcessor", "Cleaned HTML: " + cleanedHtml);
            
            // Парсим HTML с помощью Jsoup
            Document doc = Jsoup.parse(cleanedHtml);
            
            Log.d("CommentHtmlProcessor", "Parsed document body children: " + doc.body().children().size());
            
            // Обрабатываем каждый элемент
            for (Element element : doc.body().children()) {
                Log.d("CommentHtmlProcessor", "Processing element: " + element.tagName() + " with classes: " + element.className());
                processElement(element, container);
            }
            
        } catch (Exception e) {
            Log.e("CommentHtmlProcessor", "Error parsing HTML", e);
            // Если парсинг не удался, показываем как обычный текст
            TextView fallbackView = createTextView();
            Spanned spanned = Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_LEGACY);
            fallbackView.setText(spanned);
            container.addView(fallbackView);
        }
    }
    
    /**
     * Обработать HTML элемент
     */
    private void processElement(Element element, LinearLayout container) {
        String tagName = element.tagName().toLowerCase();
        
        Log.d("CommentHtmlProcessor", "Processing element: " + tagName + ", classes: " + element.className());
        Log.d("CommentHtmlProcessor", "Element HTML: " + element.html());
        
        switch (tagName) {
            case "p":
                processParagraph(element, container);
                break;
            case "blockquote":
                processBlockquote(element, container);
                break;
            case "div":
                if (element.hasClass("spoiler-node")) {
                    Log.d("CommentHtmlProcessor", "Found div with spoiler-node class");
                    processSpoiler(element, container);
                } else {
                    processDiv(element, container);
                }
                break;
            case "span":
                if (element.hasClass("spoiler-node")) {
                    Log.d("CommentHtmlProcessor", "Found span with spoiler-node class");
                    processSpoiler(element, container);
                } else {
                    processSpan(element, container);
                }
                break;
            default:
                Log.d("CommentHtmlProcessor", "Processing default element: " + tagName);
                // Для остальных элементов создаем обычный TextView
                TextView textView = createTextView();
                Spanned spanned = Html.fromHtml(element.html(), Html.FROM_HTML_MODE_LEGACY);
                textView.setText(spanned);
                container.addView(textView);
                break;
        }
    }
    
    /**
     * Обработать параграф
     */
    private void processParagraph(Element element, LinearLayout container) {
        if (element.text().trim().isEmpty()) {
            // Пустой параграф - добавляем отступ
            View spacer = new View(context);
            spacer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                dpToPx(8)
            ));
            container.addView(spacer);
            return;
        }
        
        // Проверяем есть ли в параграфе спойлеры
        Elements spoilerElements = element.select(".spoiler-node");
        if (!spoilerElements.isEmpty()) {
            Log.d("CommentHtmlProcessor", "Found spoiler elements in paragraph: " + spoilerElements.size());
            // Обрабатываем каждый спойлер отдельно
            for (Element spoilerElement : spoilerElements) {
                processSpoiler(spoilerElement, container);
            }
            return;
        }
        
        TextView textView = createTextView();
        Spanned spanned = processInlineElements(element);
        textView.setText(spanned);
        container.addView(textView);
    }
    
    /**
     * Обработать цитату
     */
    private void processBlockquote(Element element, LinearLayout container) {
        // Создаем контейнер для цитаты
        LinearLayout quoteContainer = new LinearLayout(context);
        quoteContainer.setOrientation(LinearLayout.HORIZONTAL);
        quoteContainer.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        
        // Устанавливаем фон с скругленными углами
        quoteContainer.setBackgroundResource(R.drawable.quote_background_light);
        
        // Добавляем скругленные углы
        quoteContainer.setElevation(dpToPx(2));

        // Создаем TextView для содержимого цитаты
        TextView textView = createTextView();
        textView.setPadding(4, 0, 0, 0); // Убираем стандартные отступы
        
        // Создаем SpannableStringBuilder для цитаты
        SpannableStringBuilder builder = new SpannableStringBuilder();
        
        // Добавляем символ цитаты
        builder.append("❝ ");
        
        // Обрабатываем содержимое цитаты
        Spanned content = processInlineElements(element);
        builder.append(content);
        
        // Применяем стили цитаты
        builder.setSpan(new ForegroundColorSpan(primaryTextColor), 4, builder.length(),
                      Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // Текст цитаты
        builder.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), 2, builder.length(), 
                      Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        textView.setText(builder);
        
        // Добавляем TextView в контейнер
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        textView.setLayoutParams(textParams);
        quoteContainer.addView(textView);
        
        // Добавляем отступы для контейнера
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        quoteContainer.setLayoutParams(containerParams);
        
        container.addView(quoteContainer);
    }
    
    /**
     * Обработать спойлер
     */
    private void processSpoiler(Element element, LinearLayout container) {
        String spoilerText = element.attr("data-spoiler-text");
        String spoilerType = element.attr("data-spoiler-type");
        
        Log.d("CommentHtmlProcessor", "Processing spoiler - text: '" + spoilerText + "', type: '" + spoilerType + "'");
        Log.d("CommentHtmlProcessor", "Element HTML: " + element.html());
        
        // Извлекаем текст спойлера
        Elements spoilerTextElements = element.select(".spoiler-node__text");
        String content = "";
        if (!spoilerTextElements.isEmpty()) {
            // Если есть элементы с классом spoiler-node__text, берем их содержимое
            StringBuilder contentBuilder = new StringBuilder();
            for (Element textElement : spoilerTextElements) {
                if (contentBuilder.length() > 0) {
                    contentBuilder.append("\n");
                }
                contentBuilder.append(textElement.text());
            }
            content = contentBuilder.toString();
            Log.d("CommentHtmlProcessor", "Found spoiler text elements, content: '" + content + "'");
        } else {
            // Если нет специальных элементов, берем весь текст элемента
            content = element.text();
            Log.d("CommentHtmlProcessor", "No spoiler text elements found, using element text: '" + content + "'");
        }
        
        // Создаем SpoilerView
        com.example.animelib.ui.SpoilerView spoilerView = 
            new com.example.animelib.ui.SpoilerView(context);
        
        String title = !spoilerText.isEmpty() ? spoilerText : "Спойлер";
        
        Log.d("CommentHtmlProcessor", "Creating spoiler with title: '" + title + "', content: '" + content + "'");
        spoilerView.setSpoilerData(title, content);
        container.addView(spoilerView);
    }
    
    /**
     * Обработать div
     */
    private void processDiv(Element element, LinearLayout container) {
        TextView textView = createTextView();
        Spanned spanned = processInlineElements(element);
        textView.setText(spanned);
        container.addView(textView);
    }
    
    /**
     * Обработать span
     */
    private void processSpan(Element element, LinearLayout container) {
        TextView textView = createTextView();
        Spanned spanned = processInlineElements(element);
        textView.setText(spanned);
        container.addView(textView);
    }
    
    /**
     * Обработать inline элементы (strong, em, u, strike и т.д.)
     */
    private Spanned processInlineElements(Element element) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        
        for (Node node : element.childNodes()) {
            if (node instanceof TextNode) {
                // Обычный текст
                TextNode textNode = (TextNode) node;
                builder.append(textNode.text());
            } else if (node instanceof Element) {
                Element childElement = (Element) node;
                String tagName = childElement.tagName().toLowerCase();
                
                int start = builder.length();
                
                switch (tagName) {
                    case "strong":
                    case "b":
                        builder.append(childElement.text());
                        builder.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 
                                      start, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case "em":
                    case "i":
                        builder.append(childElement.text());
                        builder.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), 
                                      start, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case "u":
                        builder.append(childElement.text());
                        builder.setSpan(new UnderlineSpan(), 
                                      start, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case "strike":
                    case "s":
                        builder.append(childElement.text());
                        builder.setSpan(new StrikethroughSpan(), 
                                      start, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case "br":
                        builder.append("\n");
                        break;
                    default:
                        // Рекурсивно обрабатываем вложенные элементы
                        Spanned nested = processInlineElements(childElement);
                        builder.append(nested);
                        break;
                }
            }
        }
        
        return builder;
    }
    
    /**
     * Создать TextView с базовыми настройками
     */
    private TextView createTextView() {
        TextView textView = new TextView(context);
        textView.setTextColor(primaryTextColor);
        textView.setTextSize(13);
        textView.setLineSpacing(dpToPx(2), 1.0f);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dpToPx(4), 0, dpToPx(4));
        textView.setLayoutParams(params);
        
        return textView;
    }
    
    /**
     * Преобразовать dp в px
     */
    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
}
