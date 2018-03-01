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

import android.graphics.Color;

import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnectionType;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeResponseCallback;
import com.philips.lighting.hue.sdk.wrapper.domain.Bridge;
import com.philips.lighting.hue.sdk.wrapper.domain.BridgeState;
import com.philips.lighting.hue.sdk.wrapper.domain.HueError;
import com.philips.lighting.hue.sdk.wrapper.domain.ReturnCode;
import com.philips.lighting.hue.sdk.wrapper.domain.clip.ClipResponse;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightPoint;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightState;
import com.philips.lighting.hue.sdk.wrapper.domain.resource.Group;
import com.philips.lighting.hue.sdk.wrapper.domain.resource.Scene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Stores a list of {@link Light}s and aggregates data and actions on the list as if they were a single light object. */
public class AggregateLight
{
    //region Static Fields

    /** The name that is returned by a {@link AggregateLight} if there are no lights contained within. */
    public static final String NONE = "none";

    /** The name that is returned by a {@link AggregateLight} if there are multiple lights contained within. */
    public static final String MULTIPLE = "multiple";

    /** A brightness value that indicates that no valid brightness value has been defined for the light object. */
    public static final int BRIGHTNESS_UNDEFINED = -1;

    /** A color value that indicates that no valid color value has been defined for the light object. */
    public static final int COLOR_UNDEFINED = 0;

    //endregion


    //region Fields

    /** The list of light points and groups that are encapsulated in the light object. */
    private List<Light> lights = new ArrayList<>();

    /** The current brightness of the light object, which will be set across all lights contained within. */
    private int brightness = BRIGHTNESS_UNDEFINED;

    /** The current color of the light object, which will be set across all lights contained within. */
    private int color = COLOR_UNDEFINED;

    //endregion


    //region Property Getters/Setters

    /** Gets the name of the light group.  If there is only one {@link Light} in the object, returns that name.
     * If there are no object, returns <code>AggregateLight.NONE</code>.  If there are multiple objects, returns <code>AggregateLight.MULTIPLE</code>.
     * @return The name of the contained object, or <code>AggregateLight.NONE</code> if none or <code>AggregateLight.MULTIPLE</code> if multiple.
     */
    public String getName()
    {
        switch (lights.size())
        {
            case 0:
                return NONE;

            case 1:
                return lights.get(0).getName();

            default:
                return MULTIPLE;
        }
    }

    /** Gets whether the light object is on.  If any contained lights are on, returns <code>true</code>, and returns <code>false</code> if all lights are off
     * or there are no contained lights.
     * @return Returns <code>true</code> if any contained lights are on, <code>false</code> otherwise.
     */
    public boolean getOn()
    {
        return lights.stream().anyMatch(new Predicate<Light>()
        {
            @Override
            public boolean test(Light light)
            {
                return light.getOn();
            }
        });
    }

    /** Gets whether the light object supports color, which it does if any contained lights support color.
     * @return Returns <code>true</code> of any contained lights support color, <code>false</code> otherwise.
     */
    public boolean getSupportsColors()
    {
        return lights.stream().anyMatch(new Predicate<Light>()
        {
            @Override
            public boolean test(Light light)
            {
                return light.getSupportsColors();
            }
        });
    }

    /** Gets the brightness of the light object.  If there is only a single {@link Light} contained that has a single brightness, returns its brightness.  Otherwise, unless the brightness
     * has been explicitly set on this light object, returns {@link AggregateLight#BRIGHTNESS_UNDEFINED}.
     * @return The brightness of the light object if a valid brightness exists, or {@link AggregateLight#BRIGHTNESS_UNDEFINED} of one does not.
     */
    public int getBrightness()
    {
        switch (lights.size())
        {
            case 0:
                return brightness;

            case 1:
                int lightBrightness = lights.get(0).getBrightness();
                if (lightBrightness != Light.BRIGHTNESS_MULTIPLE)
                    return lightBrightness;
                else
                    return brightness;

            default:
                List<Integer> uniqueBrightnesses = new ArrayList<>();

                for (Light light : lights)
                {
                    int brightness = light.getBrightness();

                    if (!uniqueBrightnesses.contains(brightness))
                        uniqueBrightnesses.add(brightness);
                }

                if (uniqueBrightnesses.size() == 1 && uniqueBrightnesses.get(0) != Light.BRIGHTNESS_MULTIPLE)
                    return uniqueBrightnesses.get(0);
                else
                    return brightness;
        }
    }

    /** Sets the brightness of the light object.  Note that this does not apply the brightness to the contained lights, but merely stores
     * an aggregate brightness that can then be applied to all contained lights.
     * @param brightness The brightness.
     */
    public void setBrightness(int brightness)
    {
        this.brightness = brightness;
    }

    /** Gets the color of the light object.  If there is only a single {@link Light} contained and it does not have multiple colors, returns its color.  Otherwise, unless the color
     * has been explicitly set on this light object, returns {@link AggregateLight#COLOR_UNDEFINED}.
     * @return The color of the light object if a valid color exists, or {@link AggregateLight#COLOR_UNDEFINED} if one does not.
     */
    public int getColor()
    {
        switch (lights.size())
        {
            case 0:
                return color;

            case 1:
                int lightColor = lights.get(0).getColor();

                if (color != Light.COLOR_MULTIPLE)
                    return lightColor;
                else
                    return color;

            default:
                List<Integer> uniqueColors = new ArrayList<>();

                for (Light light : lights)
                {
                    int color = light.getColor();

                    if (!uniqueColors.contains(color))
                        uniqueColors.add(color);
                }

                if (uniqueColors.size() == 1 && uniqueColors.get(0) != Light.COLOR_MULTIPLE)
                    return uniqueColors.get(0);
                else
                    return color;
        }
    }

    /** Gets a list of all unique colors of the lights contained within.  Colors are sorted by their HSV value.
     * @return A list of unique colors of the contained lights.
     */
    public List<Integer> getColors()
    {
        List<Integer> colors = new ArrayList<>();

        for (Light light : lights)
        {
            List<Integer> lightColors = light.getColors();

            for (int lightColor : lightColors)
            {
                if (!colors.contains(lightColor))
                    colors.add(lightColor);
            }
        }

        colors.sort((c1, c2) ->
        {
            float[] c1HSV = new float[3];
            float[] c2HSV = new float[3];
            Color.colorToHSV(c1, c1HSV);
            Color.colorToHSV(c2, c2HSV);

            if (c1HSV[0] != c2HSV[0])
                return c1HSV[0] < c2HSV[0] ? -1 : 1;

            if (c1HSV[1] != c2HSV[1])
                return c1HSV[1] < c2HSV[1] ? -1 : 1;

            if (c1HSV[2] != c2HSV[2])
                return c1HSV[2] < c2HSV[2] ? -1 : 1;

            return 0;
        });

        return colors;
    }

    /** Sets the color of the light object.  Note that this does not apply the color to the contained lights, but merely stores
     * an aggregate color that can then be applied to all contained lights.
     * @param color The brightness.
     */
    public void setColor(int color)
    {
        this.color = color;
    }

    //endregion


    //region Light Adders/Getters

    /** Adds a {@link LightPoint} to the light object.
     * @param lightPoint The {@link LightPoint} to add.
     */
    public void addLightPoint(LightPoint lightPoint)
    {
        lights.add(new SingleLight(lightPoint));
    }

    /** Adds a list of {@link LightPoint}s to the light object.
     * @param lightPoints The {@link LightPoint}s to add.
     */
    public void addLightPoints(List<LightPoint> lightPoints)
    {
        for (LightPoint lightPoint : lightPoints)
            lights.add(new SingleLight(lightPoint));
    }

    /** Adds a {@link Group} to the light object.
     * @param lightGroup The {@link Group} to add.
     * @param bridgeState The bridge state that the group's contained lights are in.
     */
    public void addLightGroup(Group lightGroup, BridgeState bridgeState)
    {
        lights.add(new GroupLight(lightGroup, bridgeState));
    }

    /** Adds a list of {@link Group}s to the light object.
     * @param lightGroups The {@link Group}s to add.
     * @param bridgeState The bridge state that the groups' contained lights are in.
     */
    public void addLightGroups(List<Group> lightGroups, BridgeState bridgeState)
    {
        for (Group lightGroup : lightGroups)
            lights.add(new GroupLight(lightGroup, bridgeState));
    }

    /** Gets all of the {@link Light}s in the aggregate light.
     * @return The list of {@link Light}s.
     */
    public List<Light> getLights()
    {
        return lights;
    }

    /** Gets all of the {@link LightPoint}s within the light object.
     * @return A list of {@link LightPoint}s.
     */
    public List<LightPoint> getLightPoints()
    {
        return lights.stream().filter(new Predicate<Light>()
        {
            @Override
            public boolean test(Light light)
            {
                return light instanceof SingleLight;
            }
        }).map(new Function<Light, LightPoint>()
        {
            @Override
            public LightPoint apply(Light light)
            {
                return ((SingleLight)light).getLightPoint();
            }
        }).collect(Collectors.<LightPoint>toList());
    }

    /** Gets all of the {@link Group}s within the light object.
     * @return A list of {@link Group}s.
     */
    public List<Group> getGroups()
    {
        return lights.stream().filter(new Predicate<Light>()
        {
            @Override
            public boolean test(Light light)
            {
                return light instanceof GroupLight;
            }
        }).map(new Function<Light, Group>()
        {
            @Override
            public Group apply(Light light)
            {
                return ((GroupLight)light).getGroup();
            }
        }).collect(Collectors.<Group>toList());
    }

    //endregion



    //region Light States

    /** Gets whether this light object contains any lights.
     * @return Returns <code>true</code> if there are lights, <code>false</code> otherwise.
     */
    public boolean hasLights()
    {
        return lights.size() > 0;
    }

    /** Updates the light states of all contained lights.
     * @param bridgeState The bridge state that contains the updated light states.
     */
    public void updateLightState(BridgeState bridgeState)
    {
        List<String> lightIds = getLightPoints().stream().map(new Function<LightPoint, String>()
        {
            @Override
            public String apply(LightPoint lightPoint)
            {
                return lightPoint.getIdentifier();
            }
        }).collect(Collectors.<String>toList());

        List<String> groupIds = getGroups().stream().map(new Function<Group, String>()
        {
            @Override
            public String apply(Group group)
            {
                return group.getIdentifier();
            }
        }).collect(Collectors.<String>toList());

        lights.clear();

        for (String id : lightIds)
            addLightPoint(bridgeState.getLight(id));

        for (String id : groupIds)
            addLightGroup(bridgeState.getGroup(id), bridgeState);
    }

    /** Applies a new {@link LightState} to all lights contained in this light object.
     * @param newState The {@link LightState} to apply.
     * @param connectionType The connection type to apply the update over.
     * @param callback A callback to receive results with.  Will be called once all lights have responded to the update.
     */
    public void applyLightState(LightState newState, BridgeConnectionType connectionType, final LightObjectApplyStateCallback callback)
    {
        final AtomicInteger latch = new AtomicInteger(lights.size());
        final LightObjectApplyResults[] results = new LightObjectApplyResults[lights.size()];

        for (int i = 0; i < lights.size(); i++)
        {
            final Light light = lights.get(i);
            final int index = i;

            light.applyLightState(newState, connectionType, new BridgeResponseCallback()
            {
                @Override
                public void handleCallback(Bridge bridge, ReturnCode returnCode, List<ClipResponse> responses, List<HueError> errors)
                {
                    results[index] = new LightObjectApplyResults(light, returnCode, errors);

                    int current = latch.decrementAndGet();

                    if (current == 0)
                        callback.onApplyCompleted(Arrays.asList(results));
                }
            });
        }
    }

    /** Filters and groups a list of scenes to those that apply to the lights contained in this light object.
     * @param scenes The list of scenes to filter and group.
     * @return A filtered list of scene groups.
     */
    public List<SceneGroup> filterValidScenes(List<Scene> scenes)
    {
        List<Scene> filteredScenes = scenes.stream().filter(new Predicate<Scene>()
        {
            @Override
            public boolean test(final Scene scene)
            {
                return lights.stream().anyMatch(new Predicate<Light>()
                {
                    @Override
                    public boolean test(Light light)
                    {
                        return light.isSceneValid(scene);
                    }
                });
            }
        }).collect(Collectors.<Scene>toList());

        Map<String, List<Scene>> groups = filteredScenes.stream().collect(Collectors.groupingBy(new Function<Scene, String>()
        {
            @Override
            public String apply(Scene scene)
            {
                return scene.getName();
            }
        }));

        List<SceneGroup> sceneGroups = new ArrayList<>();

        for (Map.Entry<String, List<Scene>> entry : groups.entrySet())
            sceneGroups.add(new SceneGroup(entry.getValue()));

        return sceneGroups;
    }

    /** A callback that receives an event when all lights have responded to applying a new light state. */
    public interface LightObjectApplyStateCallback
    {
        /** Raised when all lights have responded to applying the new light state.
         * @param results A list of responses from each light.
         */
        public void onApplyCompleted(List<LightObjectApplyResults> results);
    }

    /** Stores the response of a light to applying a new light state. */
    public class LightObjectApplyResults
    {
        Light light;
        ReturnCode returnCode;
        List<HueError> errors;

        private LightObjectApplyResults(Light light, ReturnCode returnCode, List<HueError> errors)
        {
            this.light = light;
            this.returnCode = returnCode;
            this.errors = errors;
        }

        public Light getLight()
        {
            return light;
        }

        public ReturnCode getReturnCode()
        {
            return returnCode;
        }

        public List<HueError> getErrors()
        {
            return errors;
        }
    }

    //endregion
}
