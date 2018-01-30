package com.iot.extron.smartlightswitch.utilities;

import android.graphics.Color;

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
}
