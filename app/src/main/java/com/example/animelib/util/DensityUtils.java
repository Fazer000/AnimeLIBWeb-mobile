package com.example.animelib.util;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public class DensityUtils {

    /**
     * Преобразует dp в пиксели
     */
    public static int dpToPx(Context context, float dp) {
        if (context == null) return (int) dp;

        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return (int) (dp * (metrics.densityDpi / 160f));
    }

    /**
     * Преобразует пиксели в dp
     */
    public static float pxToDp(Context context, int px) {
        if (context == null) return px;

        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return px / (metrics.densityDpi / 160f);
    }

    /**
     * Преобразует sp в пиксели (для текста)
     */
    public static int spToPx(Context context, float sp) {
        if (context == null) return (int) sp;

        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                sp,
                metrics
        );
    }

    /**
     * Преобразует пиксели в sp (для текста)
     */
    public static float pxToSp(Context context, int px) {
        if (context == null) return px;

        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return px / metrics.scaledDensity;
    }

    /**
     * Преобразует dp в пиксели (статический метод без контекста)
     */
    public static int dpToPx(float dp, DisplayMetrics metrics) {
        if (metrics == null) return (int) dp;
        return (int) (dp * (metrics.densityDpi / 160f));
    }

    /**
     * Преобразует пиксели в dp (статический метод без контекста)
     */
    public static float pxToDp(int px, DisplayMetrics metrics) {
        if (metrics == null) return px;
        return px / (metrics.densityDpi / 160f);
    }

    /**
     * Получает DisplayMetrics из контекста
     */
    public static DisplayMetrics getDisplayMetrics(Context context) {
        if (context == null) return null;
        return context.getResources().getDisplayMetrics();
    }

    /**
     * Преобразует значение по TypedValue
     */
    public static float applyDimension(int unit, float value, Context context) {
        if (context == null) return value;

        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return TypedValue.applyDimension(unit, value, metrics);
    }
}