package com.iot.extron.smartlightswitch.models;

import android.graphics.Color;

import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnectionType;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeResponseCallback;
import com.philips.lighting.hue.sdk.wrapper.domain.BridgeState;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightPoint;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightState;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightType;
import com.philips.lighting.hue.sdk.wrapper.domain.resource.Group;
import com.philips.lighting.hue.sdk.wrapper.domain.resource.Scene;
import com.philips.lighting.hue.sdk.wrapper.utilities.HueColor;

import java.util.ArrayList;
import java.util.List;

/** Abstracts a {@link Group}. */
public class GroupLight implements Light
{
    //region Fields

    /** The group of lights being abstracted. */
    Group group;

    /** A list of lights that are contained in the group. */
    List<LightPoint> lights = new ArrayList<>();

    //endregion


    //region Constructors

    /** Creates a new {@link GroupLight} that wraps the specified {@link Group}.
     * @param group The {@link Group} to wrap.
     * @param bridgeState The {@link BridgeState} that contains the lights in {@code group}.
     */
    public GroupLight(Group group, BridgeState bridgeState)
    {
        this.group = group;

        for (String lightId : group.getLightIds())
        {
            LightPoint lightPoint = bridgeState.getLight(lightId);

            if (lightPoint != null)
                lights.add(lightPoint);
        }
    }

    //endregion


    //region Light Interface Methods

    @Override
    public String getIdentifier()
    {
        return group.getIdentifier();
    }

    @Override
    public String getName()
    {
        return group.getName();
    }

    @Override
    public int getColor()
    {
        List<Integer> uniqueColors = getColors();

        if (uniqueColors.size() > 1)
            return COLOR_MULTIPLE;
        else
            return uniqueColors.get(0);
    }

    @Override
    public List<Integer> getColors()
    {
        List<Integer> uniqueColors = new ArrayList<>();

        for (LightPoint light : lights)
        {
            HueColor.RGB rgb = light.getLightState().getColor().getRGB();
            int color = Color.argb(255, rgb.r, rgb.g, rgb.b);
            if (!uniqueColors.contains(color))
                uniqueColors.add(color);
        }

        return uniqueColors;
    }

    @Override
    public int getBrightness()
    {
        List<Integer> uniqueBrightnesses = new ArrayList<>();

        for (LightPoint light : lights)
        {
            int brightness = light.getLightState().getBrightness();
            if (!uniqueBrightnesses.contains(brightness))
                uniqueBrightnesses.add(brightness);
        }

        if (uniqueBrightnesses.size() > 1)
            return BRIGHTNESS_MULTIPLE;
        else
            return uniqueBrightnesses.get(0);
    }

    @Override
    public boolean getOn()
    {
        return group.getGroupState().isAnyOn();
    }

    @Override
    public boolean getSupportsColors()
    {
        // A group light supports color if any of its contained lights support color.
        boolean supportsColor = false;

        for (LightPoint light : lights)
        {
            LightType type = light.getLightType();

            if ((type == LightType.COLOR) || (type == LightType.COLOR_TEMPERATURE) || (type == LightType.EXTENDED_COLOR))
            {
                supportsColor = true;
                break;
            }
        }

        return supportsColor;
    }

    @Override
    public boolean isSceneValid(Scene scene)
    {
        for (String id : group.getLightIds())
        {
            if (scene.getLightIds().contains(id))
                return true;
        }

        return false;
    }

    @Override
    public void applyLightState(LightState state, BridgeConnectionType connectionType, BridgeResponseCallback callback)
    {
        group.apply(state, connectionType, callback);
    }

    //endregion


    //region Getters/Setters

    /** Gets the encapsulated {@link Group}.
     * @return The encapsulated {@link Group}
     */
    public Group getGroup()
    {
        return group;
    }

    //endregion
}
