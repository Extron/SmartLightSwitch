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

package com.iot.extron.smartlightswitch.models;

import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnectionType;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeResponseCallback;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightState;
import com.philips.lighting.hue.sdk.wrapper.domain.resource.Scene;

import java.util.List;

/** An abstraction for {@link LightPoint} and {@link Group}. */
public interface Light
{
    //region Constants

    /** The color value that indicates the light has multiple colors. */
    public static final int COLOR_MULTIPLE = 0;

    /** The brightness value that indicates the light has multiple brightnesses. */
    public static final int BRIGHTNESS_MULTIPLE = -1;

    //endregion


    //region Interface Methods

    /** Gets the identifier of the light.
     * @return The light's identifier.
     */
    public String getIdentifier();

    /** Gets the name of the light.
     * @return The light's name.
     */
    public String getName();

    /** Gets the color of the light.
     * @return The light's color.  Will return <code>COLOR_MULTIPLE</code> if the light has multiple colors.
     */
    public int getColor();

    /** Gets a list of all unique colors of the light.
     * @return A list of unique colors of the light.
     */
    public List<Integer> getColors();

    /** Gets the brightness of the light.
     * @return The light's brightness.  Will return <code>BRIGHTNESS_MULTIPLE</code> if the light has multiple brightnesses.
     */
    public int getBrightness();

    /** Gets whether the light is on.
     * @return Returns <code>true</code> if the light is on, otherwise <code>false</code>.
     */
    public boolean getOn();

    /** Gets whether the light supports colors.
     * @return Returns <code>true</code> if the light supports colors, otherwise <code>false</code>.
     */
    public boolean getSupportsColors();

    /** Gets whether the scene is valid for this {@link Light}.
     * @param scene The scene to validate.
     * @return Returns <code>true</code> if the scene can be applied to the light, otherwise <code>false</code>.
     */
    public boolean isSceneValid(Scene scene);

    /** Applies the specified {@link LightState} to the light
     * @param state The state to apply.
     * @param connectionType The {@link BridgeConnectionType} to the Hue bridge to apply the state over.
     * @param callback A {@link BridgeResponseCallback} to receive response events.
     */
    public void applyLightState(LightState state, BridgeConnectionType connectionType, BridgeResponseCallback callback);

    //endregion
}
