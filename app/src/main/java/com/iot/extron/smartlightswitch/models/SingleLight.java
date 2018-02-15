package com.iot.extron.smartlightswitch.models;

import android.graphics.Color;

import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnectionType;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeResponseCallback;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightPoint;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightState;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightType;
import com.philips.lighting.hue.sdk.wrapper.domain.resource.Scene;
import com.philips.lighting.hue.sdk.wrapper.utilities.HueColor;

import java.util.ArrayList;
import java.util.List;

/** Abstracts a single {@link LightPoint}. */
public class SingleLight implements Light
{
    //region Fields

    LightPoint lightPoint;

    //endregion


    //region Constructors

    /** Creates a new {@link SingleLight} that wraps the specified {@link LightPoint}.
     * @param lightPoint The {@link LightPoint} to wrap.
     */
    public SingleLight(LightPoint lightPoint)
    {
        this.lightPoint = lightPoint;
    }

    //endregion


    //region Light Interface Methods

    @Override
    public String getIdentifier()
    {
        return lightPoint.getIdentifier();
    }

    @Override
    public String getName()
    {
        return lightPoint.getName();
    }

    @Override
    public int getColor()
    {
        HueColor.RGB rgb = lightPoint.getLightState().getColor().getRGB();
        return Color.argb(255, rgb.r, rgb.g, rgb.b);
    }

    @Override
    public List<Integer> getColors()
    {
        List<Integer> colors = new ArrayList<>();
        colors.add(getColor());
        return colors;
    }

    @Override
    public int getBrightness()
    {
        return lightPoint.getLightState().getBrightness();
    }

    @Override
    public boolean getOn()
    {
        return lightPoint.getLightState().isOn();
    }

    @Override
    public boolean getSupportsColors()
    {
        LightType type = lightPoint.getLightType();
        return (type == LightType.COLOR) || (type == LightType.COLOR_TEMPERATURE) || (type == LightType.EXTENDED_COLOR);
    }

    @Override
    public boolean isSceneValid(Scene scene)
    {
        return scene.getLightIds().contains(lightPoint.getIdentifier());
    }

    @Override
    public void applyLightState(LightState state, BridgeConnectionType connectionType, BridgeResponseCallback callback)
    {
        lightPoint.updateState(state, connectionType, callback);
    }

    //endregion


    //region Getters/Setters

    /** Gets the encapsulated {@link LightPoint}.
     * @return The encapsulated {@link LightPoint}
     */
    public LightPoint getLightPoint()
    {
        return lightPoint;
    }

    //endregion
}
