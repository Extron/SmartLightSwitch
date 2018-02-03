package com.iot.extron.smartlightswitch.models;

import android.graphics.Color;
import android.util.Pair;

import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnectionType;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeResponseCallback;
import com.philips.lighting.hue.sdk.wrapper.domain.Bridge;
import com.philips.lighting.hue.sdk.wrapper.domain.BridgeState;
import com.philips.lighting.hue.sdk.wrapper.domain.DomainObject;
import com.philips.lighting.hue.sdk.wrapper.domain.DomainType;
import com.philips.lighting.hue.sdk.wrapper.domain.HueError;
import com.philips.lighting.hue.sdk.wrapper.domain.ReturnCode;
import com.philips.lighting.hue.sdk.wrapper.domain.clip.ClipResponse;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightPoint;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightState;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightType;
import com.philips.lighting.hue.sdk.wrapper.domain.resource.Group;
import com.philips.lighting.hue.sdk.wrapper.domain.resource.Scene;
import com.philips.lighting.hue.sdk.wrapper.utilities.HueColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** An object that wraps a heterogeneous list of {@link LightPoint}s and {@link com.philips.lighting.hue.sdk.wrapper.domain.resource.Group}s and aggregates
 * data and actions on the list as if they were a single light object.
 */
public class LightObject
{
    //region Static Fields

    /** The name that is returned by a {@link LightObject} if there are no lights contained within. */
    public static final String NONE = "none";

    /** The name that is returned by a {@link LightObject} if there are multiple lights contained within. */
    public static final String MULTIPLE = "multiple";

    /** A brightness value that indicates that no valid brightness value has been defined for the light object. */
    public static final int BRIGHTNESS_UNDEFINED = -1;

    /** A color value that indicates that no valid color value has been defined for the light object. */
    public static final int COLOR_UNDEFINED = 0;

    //endregion


    //region Fields

    /** The list of light points and groups that are encapsulated in the light object. */
    private List<DomainObject> domainObjects = new ArrayList<>();

    /** The current brightness of the light object, which will be set across all lights contained within. */
    private int brightness = BRIGHTNESS_UNDEFINED;

    /** The current color of the light object, which will be set across all lights contained within. */
    private int color = COLOR_UNDEFINED;

    //endregion


    //region Property Getters/Setters

    /** Gets the name of the light group.  If there is only one {@link LightPoint} or {@link Group} in the object, returns that name.
     * If there are no object, returns <code>LightObject.NONE</code>.  If there are multiple objects, returns <code>LightObject.MULTIPLE</code>.
     * @return The name of the contained object, or <code>LightObject.NONE</code> if none or <code>LightObject.MULTIPLE</code> if multiple.
     */
    public String getName()
    {
        switch (domainObjects.size())
        {
            case 0:
                return NONE;

            case 1:
                DomainObject obj = domainObjects.get(0);

                if (obj instanceof LightPoint)
                    return ((LightPoint)obj).getName();
                else if (obj instanceof Group)
                    return ((Group)obj).getName();
                else
                    return NONE;

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
        return domainObjects.stream().anyMatch(new Predicate<DomainObject>()
        {
            @Override
            public boolean test(DomainObject domainObject)
            {
                if (domainObject instanceof LightPoint)
                    return ((LightPoint)domainObject).getLightState().isOn();
                else if (domainObject instanceof  Group && ((Group)domainObject).getGroupState() != null)
                    return ((Group)domainObject).getGroupState().isAnyOn();
                else
                    return false;
            }
        });
    }

    /** Gets whether the light object supports color, which it does if any contained lights support color.
     * @return Returns <code>true</code> of any contained lights support color, <code>false</code> otherwise.
     */
    public boolean getSupportsColors()
    {
        return domainObjects.stream().anyMatch(new Predicate<DomainObject>()
        {
            @Override
            public boolean test(DomainObject domainObject)
            {
                if (domainObject instanceof LightPoint)
                {
                    LightType type = ((LightPoint)domainObject).getLightType();
                    return (type == LightType.COLOR) || (type == LightType.COLOR_TEMPERATURE) || (type == LightType.EXTENDED_COLOR);
                }
                else if (domainObject instanceof  Group && ((Group)domainObject).getGroupState() != null)
                {
                    // TODO: Need to figure out best way to get light information for a group.
                    return true;
                }
                else
                {
                    return false;
                }
            }
        });
    }

    /** Gets the brightness of the light object.  If there is only a single {@link LightPoint} contained, returns its brightness.  Otherwise, unless the brightness
     * has been explicitly set on this light object, returns <code>LightObject.BRIGHTNESS_UNDEFINED</code>.
     * @return The brightness of the light object if a valid brightness exists, or <code>LightObject.BRIGHTNESS_UNDEFINED</code> of one does not.
     */
    public int getBrightness()
    {
        if (domainObjects.size() == 1 && domainObjects.get(0) instanceof LightPoint)
            return ((LightPoint)domainObjects.get(0)).getLightState().getBrightness();
        else
            return brightness;
    }

    /** Sets the brightness of the light object.  Note that this does not apply the brightness to the contained lights, but merely stores
     * an aggregate brightness that can then be applied to all contained lights.
     * @param brightness The brightness.
     */
    public void setBrightness(int brightness)
    {
        this.brightness = brightness;
    }

    /** Gets the color of the light object.  If there is only a single {@link LightPoint} contained, returns its color.  Otherwise, unless the color
     * has been explicitly set on this light object, returns <code>LightObject.COLOR_UNDEFINED</code>.
     * @return The color of the light object if a valid color exists, or <code>LightObject.COLOR_UNDEFINED</code> of one does not.
     */
    public int getColor()
    {
        if (domainObjects.size() == 1 && domainObjects.get(0) instanceof LightPoint)
        {
            HueColor.RGB rgb = ((LightPoint) domainObjects.get(0)).getLightState().getColor().getRGB();
            return Color.argb(255, rgb.r, rgb.g, rgb.b);
        }
        else
        {
            return color;
        }
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
        domainObjects.add(lightPoint);
    }

    /** Adds a list of {@link LightPoint}s to the light object.
     * @param lightPoints The {@link LightPoint}s to add.
     */
    public void addLightPoints(List<LightPoint> lightPoints)
    {
        domainObjects.addAll(lightPoints);
    }

    /** Adds a {@link Group} to the light object.
     * @param lightGroup The {@link Group} to add.
     */
    public void addLightGroup(Group lightGroup)
    {
        domainObjects.add(lightGroup);
    }

    /** Adds a list of {@link Group}s to the light object.
     * @param lightGroups The {@link Group}s to add.
     */
    public void addLightGroups(List<Group> lightGroups)
    {
        domainObjects.addAll(lightGroups);
    }

    /** Gets all of the {@link LightPoint}s within the light object.
     * @return A list of {@link LightPoint}s.
     */
    public List<LightPoint> getLightPoints()
    {
        return domainObjects.stream().filter(new Predicate<DomainObject>()
        {
            @Override
            public boolean test(DomainObject domainObject)
            {
                return domainObject instanceof LightPoint;
            }
        }).map(new Function<DomainObject, LightPoint>()
        {
            @Override
            public LightPoint apply(DomainObject domainObject)
            {
                return (LightPoint)domainObject;
            }
        }).collect(Collectors.<LightPoint>toList());
    }

    /** Gets all of the {@link Group}s within the light object.
     * @return A list of {@link Group}s.
     */
    public List<Group> getGroups()
    {
        return domainObjects.stream().filter(new Predicate<DomainObject>()
        {
            @Override
            public boolean test(DomainObject domainObject)
            {
                return domainObject instanceof Group;
            }
        }).map(new Function<DomainObject, Group>()
        {
            @Override
            public Group apply(DomainObject domainObject)
            {
                return (Group)domainObject;
            }
        }).collect(Collectors.<Group>toList());
    }

    //endregion



    /** Gets whether this light object contains any lights.
     * @return Returns <code>true</code> if there are lights, <code>false</code> otherwise.
     */
    public boolean hasLights()
    {
        return domainObjects.size() > 0;
    }

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

        domainObjects.clear();

        for (String id : lightIds)
            addLightPoint(bridgeState.getLight(id));

        for (String id : groupIds)
            addLightGroup(bridgeState.getGroup(id));
    }

    /** Applies a new {@link LightState} to all lights contained in this light object.
     * @param newState The {@link LightState} to apply.
     * @param connectionType The connection type to apply the update over.
     * @param callback A callback to receive results with.  Will be called once all lights have responded to the update.
     */
    public void applyLightState(LightState newState, BridgeConnectionType connectionType, final LightObjectApplyStateCallback callback)
    {
        final AtomicInteger latch = new AtomicInteger(domainObjects.size());
        final LightObjectApplyResults[] results = new LightObjectApplyResults[domainObjects.size()];

        for (int i = 0; i < domainObjects.size(); i++)
        {
            final DomainObject obj = domainObjects.get(i);
            final int index = i;

            if (obj instanceof LightPoint)
            {
                ((LightPoint)obj).updateState(newState, connectionType, new BridgeResponseCallback()
                {
                    @Override
                    public void handleCallback(Bridge bridge, ReturnCode returnCode, List<ClipResponse> responses, List<HueError> errors)
                    {
                        results[index] = new LightObjectApplyResults(obj, returnCode, errors);

                        int current = latch.decrementAndGet();

                        if (current == 0)
                            callback.onApplyCompleted(Arrays.asList(results));
                    }
                });
            }
            else if (obj instanceof Group)
            {
                ((Group)obj).apply(newState, connectionType, new BridgeResponseCallback()
                {
                    @Override
                    public void handleCallback(Bridge bridge, ReturnCode returnCode, List<ClipResponse> responses, List<HueError> errors)
                    {
                        results[index] = new LightObjectApplyResults(obj, returnCode, errors);

                        int current = latch.decrementAndGet();

                        if (current == 0)
                            callback.onApplyCompleted(Arrays.asList(results));
                    }
                });
            }
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
                return domainObjects.stream().anyMatch(new Predicate<DomainObject>()
                {
                    @Override
                    public boolean test(DomainObject domainObject)
                    {
                        if (domainObject instanceof LightPoint)
                        {
                            return scene.getLightIds().contains(((LightPoint) domainObject).getIdentifier());
                        }
                        else if (domainObject instanceof Group)
                        {
                            for (String id : ((Group)domainObject).getLightIds())
                            {
                                if (scene.getLightIds().contains(id))
                                    return true;
                            }
                        }
                        return false;
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

    public interface LightObjectApplyStateCallback
    {
        public void onApplyCompleted(List<LightObjectApplyResults> results);
    }

    public class LightObjectApplyResults
    {
        DomainObject domainObject;
        ReturnCode returnCode;
        List<HueError> errors;

        private LightObjectApplyResults(DomainObject domainObject, ReturnCode returnCode, List<HueError> errors)
        {
            this.domainObject = domainObject;
            this.returnCode = returnCode;
            this.errors = errors;
        }

        public DomainObject getObject()
        {
            return domainObject;
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
}
