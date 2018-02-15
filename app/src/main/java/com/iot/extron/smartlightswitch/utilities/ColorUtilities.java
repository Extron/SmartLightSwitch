package com.iot.extron.smartlightswitch.utilities;

import android.graphics.Color;

import java.util.List;

/**
 * Utility functions for color related operations.
 */
public class ColorUtilities
{
    /**
     * Determines if a bright or dark contrasting color is best for the specified color based on its YIQ brightness.
     * @param color The color to find the contrast color for.
     * @return <code>1</code> if <code>color</code> is dark and a bright color should be used, and
     * <code>-1</code> if <code>color</code> is bright and a dark color should be used.
     */
    public static int getContrastColor(Color color)
    {
        float brightness = 0.299f * color.red() + 0.587f * color.green() + 0.114f * color.blue();

        return brightness >= 0.5 ? -1 : 1;
    }

    /** Computes the average color of a list of colors.arithmetically.
     * @param colors The list of colors to average.
     * @return Returns the average color.
     */
    public static Color averageColors(List<Color> colors)
    {
        float r = 0f, g = 0f, b = 0f;

        for (Color color : colors)
        {
            r += color.red();
            g +=color.green();
            b += color.blue();
        }

        r /= colors.size();
        g /= colors.size();
        b /= colors.size();

        return Color.valueOf(r, g, b);
    }
}
