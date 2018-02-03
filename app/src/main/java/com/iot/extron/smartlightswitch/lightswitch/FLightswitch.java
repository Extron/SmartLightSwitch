package com.iot.extron.smartlightswitch.lightswitch;

import android.animation.ObjectAnimator;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.app.Fragment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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
import java.util.function.Predicate;

import com.iot.extron.smartlightswitch.AMain;
import com.iot.extron.smartlightswitch.BridgeEventCallback;
import com.iot.extron.smartlightswitch.models.LightObject;
import com.iot.extron.smartlightswitch.models.SceneGroup;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnectionType;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeStateCacheType;
import com.philips.lighting.hue.sdk.wrapper.domain.Bridge;
import com.philips.lighting.hue.sdk.wrapper.domain.HueError;
import com.philips.lighting.hue.sdk.wrapper.domain.ReturnCode;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightPoint;

import com.iot.extron.smartlightswitch.utilities.ColorUtilities;
import com.iot.extron.smartlightswitch.R;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightState;
import com.philips.lighting.hue.sdk.wrapper.domain.resource.Group;
import com.philips.lighting.hue.sdk.wrapper.domain.resource.Scene;
import com.philips.lighting.hue.sdk.wrapper.utilities.HueColor;


public class FLightswitch extends Fragment
{
    //region Static Fields

    private static final String TAG = "FLightswitch";
    private static final String LightPickerTag = "DFLightPicker";
    private static final String ColorPickerTag = "DFColorPicker";
    private static final String ScenePickerTag = "DFScenePicker";
    private static final int DEFAULT_COLOR = 0xFFFFFFDD;
    private static final int HEARTBEAT_INTERVAL = 15 * 1000;

    //endregion


    //region Fields

    /** The light object that the switch is controlling. */
    LightObject lightObject = new LightObject();

    /** Indicates whether the current light/lightgroup is on or off. */
    boolean on = false;

    /** The current brightness of the light/lightgroup. */
    int brightness = 100;

    /** The current color of the light/lightgroup. */
    Color color = Color.valueOf(DEFAULT_COLOR);

    //endregion


    //region UI Elements

    ProgressBar responseProgressBar;
    Button onButton;
    Button offButton;
    Button lightButton;
    FloatingActionButton colorFab;
    FloatingActionButton sceneFab;
    ViewGroup brightnessLayout;
    TextView brightnessTextView;
    SeekBar brightnessSeekBar;

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

        responseProgressBar = (ProgressBar)view.findViewById(R.id.responseProgressBar);
        brightnessLayout = (ViewGroup)view.findViewById(R.id.brightnessLayout);
        brightnessTextView = view.findViewById(R.id.brightnessTextView);

        onButton = view.findViewById(R.id.onTextView);
        onButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                turnOn();
            }
        });

        offButton = view.findViewById(R.id.offTextView);
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
                List<LightPoint> allLights = ((AMain)getActivity()).getBridge().getBridgeState().getLights();
                List<Group> allGroups = ((AMain)getActivity()).getBridge().getBridgeState().getGroups();
                List<LightPoint> currentLights = lightObject.getLightPoints();
                List<Group> currentGroups = lightObject.getGroups();

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
                        lightObject = new LightObject();
                        lightObject.addLightPoints(selectedLights);
                        lightObject.addLightGroups(selectedGroups);

                        bindData();
                        configureUI(false);

                        ((AMain)getActivity()).getBridge().getBridgeState().refresh(BridgeStateCacheType.LIGHTS_AND_GROUPS, BridgeConnectionType.LOCAL);
                    }
                });

                lightPicker.show(getFragmentManager(), LightPickerTag);
            }
        });

        colorFab = view.findViewById(R.id.colorFab);
        colorFab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                DFColorPicker colorPicker = DFColorPicker.newInstance(color.toArgb(), new DFColorPicker.OnColorSelectedCallback()
                {
                    @Override
                    public void colorSelected(int color)
                    {
                        setColor(color);
                    }
                });

                colorPicker.show(getFragmentManager(), ColorPickerTag);
            }
        });

        sceneFab = view.findViewById(R.id.sceneFab);
        sceneFab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                // TODO: Each light has its own list of scenes, which will look like duplicates in the list if multiple lights are selected.
                // Need to aggregate these duplicates into a single object.
                List<Scene> scenes = ((AMain)getActivity()).getBridge().getBridgeState().getScenes();

                DFScenePicker scenePicker = DFScenePicker.newInstance(lightObject.filterValidScenes(scenes), new DFScenePicker.OnSceneSelectedCallback()
                {
                    @Override
                    public void sceneSelected(SceneGroup scene)
                    {
                        recallScene(scene);
                    }
                });

                scenePicker.show(getFragmentManager(), ScenePickerTag);
            }
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

        ((AMain)getActivity()).addBridgeEventCallback(bridgeEventCallback);
        ((AMain)getActivity()).getBridge().getBridgeConnection(BridgeConnectionType.LOCAL).getHeartbeatManager().startHeartbeat(BridgeStateCacheType.LIGHTS_AND_GROUPS, HEARTBEAT_INTERVAL);

        getView().post(new Runnable()
        {
            @Override
            public void run()
            {

                configureUI(false);
            }
        });
    }

    @Override
    public void onDetach()
    {
        super.onDetach();

        ((AMain)getActivity()).removeBridgeEventCallback(bridgeEventCallback);
        ((AMain)getActivity()).getBridge().getBridgeConnection(BridgeConnectionType.LOCAL).getHeartbeatManager().stopHeartbeat(BridgeStateCacheType.LIGHTS_AND_GROUPS);
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
        lightObject.setBrightness(scaledBrightness);

        LightState newState = new LightState();
        newState.setBrightness(scaledBrightness);
        updateLightState(newState);
    }

    /** Sets the color of the selected lights.
     * @param color The color to set.
     */
    public void setColor(int color)
    {
        lightObject.setColor(color);

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
        lightObject.applyLightState(newState, BridgeConnectionType.LOCAL, new LightObject.LightObjectApplyStateCallback()
        {
            @Override
            public void onApplyCompleted(final List<LightObject.LightObjectApplyResults> results)
            {
                Log.i(TAG, "Light Object finished applying new state.");
                getActivity().runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        responseProgressBar.setVisibility(View.GONE);

                        // Force the bridge to send a refreshed state as soon as possible to make sure switch is responsive as possible.
                        ((AMain)getActivity()).getBridge().getBridgeState().refresh(BridgeStateCacheType.LIGHTS_AND_GROUPS, BridgeConnectionType.LOCAL);

                        boolean anyErrors = results.stream().anyMatch(new Predicate<LightObject.LightObjectApplyResults>()
                        {
                            @Override
                            public boolean test(LightObject.LightObjectApplyResults lightObjectApplyResults)
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
                        lightObject.setColor(LightObject.COLOR_UNDEFINED);

                        // Force the bridge to send a refreshed state as soon as possible to make sure switch is responsive as possible.
                        ((AMain)getActivity()).getBridge().getBridgeState().refresh(BridgeStateCacheType.LIGHTS_AND_GROUPS, BridgeConnectionType.LOCAL);

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
        on = lightObject.getOn();

        int lightBrightness = lightObject.getBrightness();

        if (lightBrightness == LightObject.BRIGHTNESS_UNDEFINED)
            brightness = 100;
        else
            brightness = (int)(100 * (float)lightBrightness / 254f);

        int lightColor = lightObject.getColor();

        if (lightColor == LightObject.COLOR_UNDEFINED)
            color = Color.valueOf(DEFAULT_COLOR);
        else
            color = Color.valueOf(lightColor);
    }

    /** Configures the UI based on the state of the currently selected lights.
     * @param animate Indicates that UI animations should play.
     */
    private void configureUI(boolean animate)
    {
        onButton.setEnabled(lightObject.hasLights());
        onButton.setActivated(on);
        onButton.setBackgroundTintList(ColorStateList.valueOf(lightObject.hasLights() ? color.toArgb() : Color.TRANSPARENT));

        int onColor = ColorUtilities.getContrastColor(color) > 0 ? R.color.colorTextOnLight : R.color.colorTextOnDark;
        onButton.setTextColor(getResources().getColor(on ? onColor : R.color.colorTextOff, null));

        offButton.setEnabled(lightObject.hasLights());
        offButton.setTextColor(getResources().getColor(on ? R.color.colorTextOff : R.color.colorTextOnLight, null));

        brightnessSeekBar.setEnabled(on);
        brightnessSeekBar.setProgress(brightness);

        // Configure the light selection button.  It should display the name of the current light.
        String name = lightObject.getName();

        switch (name)
        {
            case LightObject.NONE:
                lightButton.setText(R.string.no_lights_selected);
                break;

            case LightObject.MULTIPLE:
                lightButton.setText(R.string.multiple_lights_selected);
                break;

            default:
                lightButton.setText(name);
                break;
        }

        colorFab.setEnabled(on);
        colorFab.setVisibility(lightObject.getSupportsColors() ? View.VISIBLE : View.GONE);

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

    /** Animates the Color FAB.
     * @param animationDuration The length of time of the animation.
     */
    private void animateColorFab(int animationDuration)
    {
        int[] location = new int[2];
        colorFab.getLocationOnScreen(location);

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        ObjectAnimator animator = ObjectAnimator.ofFloat(colorFab, "translationX", on ? 0f : metrics.widthPixels - location[0]);
        animator.setDuration(animationDuration);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
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

        ObjectAnimator animator = ObjectAnimator.ofFloat(sceneFab, "translationX", on ? 0f : metrics.widthPixels - location[0]);
        animator.setDuration(animationDuration);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }

    /** Animates the brightness seekbar.
     * @param animationDuration The length of time of the animation.
     */
    private void animateBrightnessSeekBar(int animationDuration)
    {
        int[] location = new int[2];
        brightnessLayout.getLocationOnScreen(location);

        ObjectAnimator animator = ObjectAnimator.ofFloat(brightnessLayout, "translationX", on ? 0f : -(location[0] + brightnessLayout.getWidth()));
        animator.setDuration(animationDuration);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }

    //endregion


    //region BridgeEventCallback

    private BridgeEventCallback bridgeEventCallback = new BridgeEventCallback()
    {
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
                    Bridge bridge = ((AMain)getActivity()).getBridge();

                    lightObject.updateLightState(bridge.getBridgeState());

                    bindData();
                    configureUI(true);
                }
            });
        }
    };

    //endregion
}
