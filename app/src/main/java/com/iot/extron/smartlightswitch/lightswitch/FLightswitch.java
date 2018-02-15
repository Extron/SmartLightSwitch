package com.iot.extron.smartlightswitch.lightswitch;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.ArraySet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.iot.extron.smartlightswitch.models.AggregateLight;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnectionType;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeStateCacheType;
import com.philips.lighting.hue.sdk.wrapper.domain.Bridge;
import com.philips.lighting.hue.sdk.wrapper.domain.BridgeState;
import com.philips.lighting.hue.sdk.wrapper.domain.HueError;
import com.philips.lighting.hue.sdk.wrapper.domain.ReturnCode;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightPoint;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightState;
import com.philips.lighting.hue.sdk.wrapper.domain.resource.Group;
import com.philips.lighting.hue.sdk.wrapper.domain.resource.Scene;
import com.philips.lighting.hue.sdk.wrapper.utilities.HueColor;

import com.iot.extron.smartlightswitch.AMain;
import com.iot.extron.smartlightswitch.BridgeEventCallback;
import com.iot.extron.smartlightswitch.FBase;
import com.iot.extron.smartlightswitch.models.SceneGroup;
import com.iot.extron.smartlightswitch.utilities.ColorUtilities;
import com.iot.extron.smartlightswitch.R;


public class FLightswitch extends FBase
{
    //region Static Fields

    private static final String TAG = "FLightswitch";
    private static final String TAG_LIGHT_PICKER = "DFLightPicker";
    private static final String TAG_COLOR_PICKER = "DFColorPicker";
    private static final String TAG_SCENE_PICKER = "DFScenePicker";
    private static final int DEFAULT_COLOR = 0xFFFFFFDD;
    private static final int HEARTBEAT_INTERVAL = 15 * 1000;

    //endregion


    //region Fields

    /** The light object that the switch is controlling. */
    AggregateLight aggregateLight = new AggregateLight();

    /** Indicates whether the current light/lightgroup is on or off. */
    boolean on = false;

    /** The current brightness of the light/lightgroup. */
    int brightness = 100;

    /** The current colors of the aggregate light. */
    List<Integer> colors = new ArrayList<>();

    //endregion


    //region UI Elements

    ProgressBar responseProgressBar;
    View lightBackgroundView;
    Button onButton;
    Button offButton;
    Button lightButton;
    FloatingActionButton colorFab;
    FloatingActionButton sceneFab;
    ViewGroup brightnessLayout;
    TextView brightnessTextView;
    SeekBar brightnessSeekBar;

    float sceneFabOffscreenTranslation;
    float colorFabOffscreenTranslation;
    float brightnessSeekBarOffscreenTranslation;

    //endregion


    //region Constructors

    /** Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon screen orientation changes). */
    public FLightswitch()
    {
    }

    //endregion


    //region Fragment Lifecycle Methods

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.f_lightswitch, container, false);

        responseProgressBar = view.findViewById(R.id.responseProgressBar);
        brightnessLayout = view.findViewById(R.id.brightnessLayout);
        brightnessTextView = view.findViewById(R.id.brightnessTextView);
        lightBackgroundView = view.findViewById(R.id.lightBackgroundView);

        onButton = view.findViewById(R.id.onButton);
        onButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                turnOn();
            }
        });

        offButton = view.findViewById(R.id.offButton);
        offButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                turnOff();
            }
        });

        lightButton = view.findViewById(R.id.lightButton);
        lightButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                final BridgeState bridgeState = getBridge().getBridgeState();
                List<LightPoint> allLights = bridgeState.getLights();
                List<Group> allGroups = bridgeState.getGroups();
                List<LightPoint> currentLights = aggregateLight.getLightPoints();
                List<Group> currentGroups = aggregateLight.getGroups();

                List<Integer> selectedLights = new ArrayList<>();
                List<Integer> selectedGroups = new ArrayList<>();

                for (LightPoint light : currentLights)
                    selectedLights.add(allLights.indexOf(light));

                for (Group group : currentGroups)
                    selectedGroups.add(allGroups.indexOf(group));

                DFLightPicker lightPicker = DFLightPicker.newInstance(allLights, selectedLights, allGroups, selectedGroups, new DFLightPicker.OnLightsSelectedCallback()
                {
                    @Override
                    public void onLightsSelected(List<LightPoint> selectedLights, List<Group> selectedGroups)
                    {
                        aggregateLight = new AggregateLight();
                        aggregateLight.addLightPoints(selectedLights);
                        aggregateLight.addLightGroups(selectedGroups, bridgeState);

                        saveSelectedLights();
                        bindData();
                        configureUI(false);

                        getBridge().getBridgeState().refresh(BridgeStateCacheType.LIGHTS_AND_GROUPS, BridgeConnectionType.LOCAL);
                    }
                });

                lightPicker.show(getFragmentManager(), TAG_LIGHT_PICKER);
            }
        });

        colorFab = view.findViewById(R.id.colorFab);
        colorFab.setOnClickListener(v ->
        {
            int currentColor = Color.TRANSPARENT;

            if (colors.size() == 1)
                currentColor = colors.get(0);

            DFColorPicker colorPicker = DFColorPicker.newInstance(currentColor, selectedColor -> setColor(selectedColor));

            colorPicker.show(getFragmentManager(), TAG_COLOR_PICKER);
        });

        sceneFab = view.findViewById(R.id.sceneFab);
        sceneFab.setOnClickListener(v ->
        {
            List<Scene> scenes = getBridge().getBridgeState().getScenes();

            DFScenePicker scenePicker = DFScenePicker.newInstance(aggregateLight.filterValidScenes(scenes), scene -> recallScene(scene));

            scenePicker.show(getFragmentManager(), TAG_SCENE_PICKER);
        });

        brightnessSeekBar = view.findViewById(R.id.brightnessSeekBar);
        brightnessSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                brightnessTextView.setText(getResources().getString(R.string.brightness).replace("{0}", Integer.toString(progress)));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                setBrightness(seekBar.getProgress());
            }
        });

        return view;
    }

    @Override
    public void onStart()
    {
        super.onStart();

        getMainActivity().addBridgeEventCallback(bridgeEventCallback);
        getBridge().getBridgeConnection(BridgeConnectionType.LOCAL).getHeartbeatManager().startHeartbeat(BridgeStateCacheType.LIGHTS_AND_GROUPS, HEARTBEAT_INTERVAL);

        loadSelectedLights();
        bindData();

        getView().post(() ->
        {
            colorFabOffscreenTranslation = computeColorFabOffscreenTranslation();
            sceneFabOffscreenTranslation = computeSceneFabOffscreenTranslation();
            brightnessSeekBarOffscreenTranslation = computeBrightnessSeekBarOffscreenTranslation();

            configureUI(false);
        });
    }

    @Override
    public void onDetach()
    {
        super.onDetach();

        getMainActivity().removeBridgeEventCallback(bridgeEventCallback);
    }

    //endregion


    //region Light Manipulation Methods

    /** Turns on the selected lights. */
    public void turnOn()
    {
        LightState newState = new LightState();
        newState.setOn(true);

        updateLightState(newState);
    }

    /** Turns off the selected lights. */
    public void turnOff()
    {
        LightState newState = new LightState();
        newState.setOn(false);

        updateLightState(newState);
    }

    /** Sets the brightness of the selected lights.
     * @param brightness The brightness to set, between 0 and 100.
     */
    public void setBrightness(int brightness)
    {
        int scaledBrightness = (int)(254f * (float)brightness / 100f);
        aggregateLight.setBrightness(scaledBrightness);

        LightState newState = new LightState();
        newState.setBrightness(scaledBrightness);
        updateLightState(newState);
    }

    /** Sets the color of the selected lights.
     * @param color The color to set.
     */
    public void setColor(int color)
    {
        aggregateLight.setColor(color);

        HueColor hueColor = new HueColor(new HueColor.RGB(Color.red(color), Color.green(color), Color.blue(color)), null, null);

        LightState newState = new LightState();
        newState.setXYWithColor(hueColor);
        updateLightState(newState);
    }

    /** Updates the state of the currently selected lights.
     * @param newState The new light state.
     */
    private void updateLightState(LightState newState)
    {
        responseProgressBar.setVisibility(View.VISIBLE);
        aggregateLight.applyLightState(newState, BridgeConnectionType.LOCAL, new AggregateLight.LightObjectApplyStateCallback()
        {
            @Override
            public void onApplyCompleted(final List<AggregateLight.LightObjectApplyResults> results)
            {
                Log.i(TAG, "Light Object finished applying new state.");
                getActivity().runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        responseProgressBar.setVisibility(View.GONE);

                        // Force the bridge to send a refreshed state as soon as possible to make sure switch is responsive as possible.
                        getBridge().getBridgeState().refresh(BridgeStateCacheType.LIGHTS_AND_GROUPS, BridgeConnectionType.LOCAL);

                        boolean anyErrors = results.stream().anyMatch(new Predicate<AggregateLight.LightObjectApplyResults>()
                        {
                            @Override
                            public boolean test(AggregateLight.LightObjectApplyResults lightObjectApplyResults)
                            {
                                return lightObjectApplyResults.getReturnCode() != ReturnCode.SUCCESS;
                            }
                        });

                        if (anyErrors)
                        {
                            Snackbar.make(((AMain)getActivity()).getLayoutForSnackbar(), R.string.could_not_connect_light, Snackbar.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }

    /** Recalls a scene.
     * @param scene The scene to recall.
     */
    private void recallScene(SceneGroup scene)
    {
        responseProgressBar.setVisibility(View.VISIBLE);
        scene.recallScenes(BridgeConnectionType.LOCAL, new SceneGroup.SceneGroupRecallCallback()
        {
            @Override
            public void onRecallCompleted(final List<SceneGroup.SceneGroupRecallResult> results)
            {
                StringBuilder codesStr = new StringBuilder().append("\n");

                for (SceneGroup.SceneGroupRecallResult result : results)
                    codesStr.append(result.getScene().getName()).append("(").append(result.getScene().getIdentifier()).append("): ").append(result.getReturnCode()).append("\n");

                Log.i(TAG, "Scene group finished recalling with return codes: " + codesStr.toString());

                getActivity().runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        responseProgressBar.setVisibility(View.GONE);
                        aggregateLight.setColor(AggregateLight.COLOR_UNDEFINED);

                        // Force the bridge to send a refreshed state as soon as possible to make sure switch is responsive as possible.
                        getBridge().getBridgeState().refresh(BridgeStateCacheType.LIGHTS_AND_GROUPS, BridgeConnectionType.LOCAL);

                        boolean anyErrors = results.stream().anyMatch(new Predicate<SceneGroup.SceneGroupRecallResult>()
                        {
                            @Override
                            public boolean test(SceneGroup.SceneGroupRecallResult result)
                            {
                                return result.getReturnCode() != ReturnCode.SUCCESS;
                            }
                        });

                        if (anyErrors)
                        {
                            Snackbar.make(((AMain) getActivity()).getLayoutForSnackbar(), R.string.could_not_recall_scene, Snackbar.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }

    //endregion


    //region UI Methods

    /** Sets the UI's model data with the values from the currently selected lights. */
    private void bindData()
    {
        on = aggregateLight.getOn();

        int lightBrightness = aggregateLight.getBrightness();

        if (lightBrightness == AggregateLight.BRIGHTNESS_UNDEFINED)
            brightness = 100;
        else
            brightness = (int)(100 * (float)lightBrightness / 254f);

        colors = aggregateLight.getColors();
    }

    /** Configures the UI based on the state of the currently selected lights.
     * @param animate Indicates that UI animations should play.
     */
    private void configureUI(boolean animate)
    {
        lightBackgroundView.setBackground(createLightBackgroundDrawable());

        onButton.setEnabled(aggregateLight.hasLights());
        onButton.setActivated(on);

        Color averageColor = ColorUtilities.averageColors(colors.stream().map(c -> Color.valueOf(c)).collect(Collectors.toList()));

        int onColor = ColorUtilities.getContrastColor(averageColor) > 0 ? R.color.colorTextOnLight : R.color.colorTextOnDark;
        onButton.setTextColor(getResources().getColor(on ? onColor : R.color.colorTextOff, null));

        offButton.setEnabled(aggregateLight.hasLights());
        offButton.setTextColor(getResources().getColor(on ? R.color.colorTextOff : R.color.colorTextOnLight, null));

        brightnessSeekBar.setEnabled(on);
        brightnessSeekBar.setProgress(brightness);

        // Configure the light selection button.  It should display the name of the current light.
        String name = aggregateLight.getName();

        switch (name)
        {
            case AggregateLight.NONE:
                lightButton.setText(R.string.no_lights_selected);
                break;

            case AggregateLight.MULTIPLE:
                lightButton.setText(R.string.multiple_lights_selected);
                break;

            default:
                lightButton.setText(name);
                break;
        }

        colorFab.setEnabled(on);
        colorFab.setVisibility(aggregateLight.getSupportsColors() ? View.VISIBLE : View.GONE);

        sceneFab.setEnabled(on);

        if (animate)
        {
            animateColorFab(getResources().getInteger(android.R.integer.config_shortAnimTime));
            animateSceneFab(getResources().getInteger(android.R.integer.config_shortAnimTime));
            animateBrightnessSeekBar(getResources().getInteger(android.R.integer.config_shortAnimTime));
        }
        else
        {
            // If we are not animating the UI elements, we need to move them to the proper locations based on the state of the backing data.
            // We can simply run the same animation code, but set the duration to 0, making the change instant.
            animateColorFab(0);
            animateSceneFab(0);
            animateBrightnessSeekBar(0);
        }
    }

    private Drawable createLightBackgroundDrawable()
    {
        Drawable drawable;

        if (colors.size() > 1)
        {
            drawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors.stream().mapToInt(i -> i).toArray());
        }
        else
        {
            drawable = new ColorDrawable(colors.size() > 0 ? colors.get(0) : DEFAULT_COLOR);
        }

        return drawable;
    }

    /** Computes the translation needed for the color FAB to be just offscreen for animation purposes. */
    private float computeColorFabOffscreenTranslation()
    {
        int[] location = new int[2];
        colorFab.getLocationOnScreen(location);

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        return metrics.widthPixels - location[0];
    }

    /** Animates the Color FAB.
     * @param animationDuration The length of time of the animation.
     */
    private void animateColorFab(int animationDuration)
    {
        ObjectAnimator animator = ObjectAnimator.ofFloat(colorFab, "translationX", on ? 0f : colorFabOffscreenTranslation);
        animator.setDuration(animationDuration);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }

    /** Computes the translation needed for the scene FAB to be just offscreen for animation purposes. */
    private float computeSceneFabOffscreenTranslation()
    {
        int[] location = new int[2];
        sceneFab.getLocationOnScreen(location);

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        return metrics.widthPixels - location[0];
    }

    /** Animates the Scene FAB.
     * @param animationDuration The length of time of the animation.
     */
    private void animateSceneFab(int animationDuration)
    {
        int[] location = new int[2];
        sceneFab.getLocationOnScreen(location);

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        ObjectAnimator animator = ObjectAnimator.ofFloat(sceneFab, "translationX", on ? 0f : sceneFabOffscreenTranslation);
        animator.setDuration(animationDuration);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }

    /** Computes the translation needed for the brightness seekbar to be just offscreen for animation purposes. */
    private float computeBrightnessSeekBarOffscreenTranslation()
    {
        int[] location = new int[2];
        brightnessLayout.getLocationOnScreen(location);

        return -(location[0] + brightnessLayout.getWidth());
    }

    /** Animates the brightness seekbar.
     * @param animationDuration The length of time of the animation.
     */
    private void animateBrightnessSeekBar(int animationDuration)
    {
        int[] location = new int[2];
        brightnessLayout.getLocationOnScreen(location);

        ObjectAnimator animator = ObjectAnimator.ofFloat(brightnessLayout, "translationX", on ? 0f : brightnessSeekBarOffscreenTranslation);
        animator.setDuration(animationDuration);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }

    //endregion


    //region Storage

    /** The string key that the currently selected {@link LightPoint}s are saved under. */
    private static final String SK_SELECTED_LIGHTPOINTS = "_selected_lights";

    /** The string key that the currently selected {@link Group}s are saved under. */
    private static final String SK_SELECTED_GROUPS = "_selected_groups";


    /** Saves the currently selected lights to the app's preferences storage. */
    private void saveSelectedLights()
    {
        SharedPreferences preferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        Set<String> lightIds = new ArraySet<>();
        Set<String> groupIds = new ArraySet<>();

        for (LightPoint lightPoint : aggregateLight.getLightPoints())
            lightIds.add(lightPoint.getIdentifier());

        for (Group group : aggregateLight.getGroups())
            groupIds.add(group.getIdentifier());

        Bridge bridge = getBridge();
        editor.putStringSet(bridge.getIdentifier() + SK_SELECTED_LIGHTPOINTS, lightIds);
        editor.putStringSet(bridge.getIdentifier() + SK_SELECTED_GROUPS, groupIds);
        editor.apply();
    }

    /** Loads a set of lights from the app's preferences and sets them as the currently selected lights. */
    private void loadSelectedLights()
    {
        SharedPreferences preferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        Bridge bridge = getBridge();
        BridgeState bridgeState = bridge.getBridgeState();

        Set<String> lightIds = preferences.getStringSet(bridge.getIdentifier() + SK_SELECTED_LIGHTPOINTS, new ArraySet<String>());
        Set<String> groupIds = preferences.getStringSet(bridge.getIdentifier() + SK_SELECTED_GROUPS, new ArraySet<String>());

        aggregateLight = new AggregateLight();

        for (String lightId : lightIds)
        {
            LightPoint lightPoint = bridgeState.getLightPoint(lightId);

            if (lightPoint != null)
                aggregateLight.addLightPoint(lightPoint);
        }

        for (String groupId : groupIds)
        {
            Group group = bridgeState.getGroup(groupId);

            if (group != null)
                aggregateLight.addLightGroup(group, bridgeState);
        }
    }

    //endregion


    //region BridgeEventCallback

    private BridgeEventCallback bridgeEventCallback = new BridgeEventCallback()
    {
        @Override
        public void bridgeDisconnecting(Bridge bridge)
        {
            getBridge().getBridgeConnection(BridgeConnectionType.LOCAL).getHeartbeatManager().stopHeartbeat(BridgeStateCacheType.LIGHTS_AND_GROUPS);
            saveSelectedLights();
        }

        @Override
        public void bridgeDisconnected(Bridge bridge, boolean manualDisconnect, List<HueError> errors)
        {
            if (!manualDisconnect)
            {
                Snackbar.make(((AMain)getActivity()).getLayoutForSnackbar(), R.string.unexpected_disconnect_snack, Snackbar.LENGTH_LONG).show();
            }
        }

        @Override
        public void updatedLightsAndGroups(Bridge bridge)
        {
            getActivity().runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    Bridge bridge = getBridge();

                    aggregateLight.updateLightState(bridge.getBridgeState());

                    bindData();
                    configureUI(true);
                }
            });
        }
    };

    //endregion
}
