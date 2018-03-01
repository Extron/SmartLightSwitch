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

import android.content.res.Resources;
import android.util.Pair;

/** A set of utility functions to process UI text. */
public final class TextUtilities
{
    /** Gets an Android string resource that contains substitution markers (e.g. {0}, {1}, ...) and substitutes the specified strings, in order.
     * @param resources A {@link Resources} object to get the string resource from.
     * @param id The ID of the string resource.
     * @param substitutions A list of substitutions to make.  These are made in order, so "{0}" is substituted for the first element in this list.
     * @return A string resource with all substitutions make.
     */
    @SafeVarargs
    public static String getStringWithSub(Resources resources, int id, String... substitutions)
    {
        String str = resources.getString(id);

        for (int i = 0; i < substitutions.length; i++)
        {
            str = str.replace("{" + i + "}", substitutions[i]);
        }

        return str;
    }
}
