/*
 * This file is part of Hue SmartSwitch
 *
 * Hue SmartSwitch is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

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
