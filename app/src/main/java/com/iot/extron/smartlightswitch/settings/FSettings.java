package com.iot.extron.smartlightswitch.settings;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Fragment;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.things.device.ScreenManager;
import com.iot.extron.smartlightswitch.FBase;
import com.iot.extron.smartlightswitch.R;
import com.iot.extron.smartlightswitch.SLSApplication;

/**
 * A {@link Fragment} that displays the app settings.
 */
public class FSettings extends FBase
{
    //region Static Fields

    public static final String TAG = "FSettings";

    //endregion


    //region Fields

    /** The callback to raise events with. */
    SettingsEventCallback callback;

    //endregion


    //region UI Elements

    Toolbar toolbar;
    ViewGroup screenOrientationLayout;
    TextView screenOrientationTextView;

    //endregion


    //region Constructors

    /** Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon screen orientation changes). */
    public FSettings() { }

    /** Creates a new fragment to edit app settings.
     * @param callback The {@link SettingsEventCallback} to receive setting events with.
     */
    public static FSettings newInstance(@Nullable SettingsEventCallback callback)
    {
        FSettings fragment = new FSettings();
        fragment.callback = callback;
        return fragment;
    }

    //endregion


    //region Fragment Lifecycle Methods

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.f_settings, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
        {
            if (callback != null)
                callback.onBack();
        });

        screenOrientationLayout = view.findViewById(R.id.screenOrientationLayout);
        screenOrientationLayout.setOnClickListener(v ->
        {
            new AlertDialog.Builder(getContext())
                .setItems(R.array.screen_orientations, (dialog, which) ->
                {
                    SharedPreferences.Editor editor = getApplication().getSettingsPreferences().edit();

                    editor.putInt(SLSApplication.SETTINGS_SCREEN_ORIENTATION, which);
                    editor.commit();

                    try
                    {
                        ScreenManager screenManager = new ScreenManager(Display.DEFAULT_DISPLAY);
                        screenManager.lockRotation(which);
                    }
                    catch (NoClassDefFoundError e)
                    {
                        // Catching this error because ScreenManager only exists on Android Things, but in the case that this app is running on another Android device (e.g. emulator for testing)
                        // we can still proceed without the above code.
                        Log.e(TAG, "ScreenManager class could not be found: " + e.getMessage());
                    }

                    configureUI();
                })
                .show();
        });

        screenOrientationTextView = view.findViewById(R.id.screenOrientationTextView);
    }

    @Override
    public void onStart()
    {
        super.onStart();

        getView().post(() ->
        {
            configureUI();
        });
    }

    //endregion


    //region UI Manipulation Methods

    /** Configures the UI to display the app's current settings. */
    private void configureUI()
    {
        SharedPreferences preferences = getApplication().getSettingsPreferences();

        int orientation = preferences.getInt(SLSApplication.SETTINGS_SCREEN_ORIENTATION, 1);
        screenOrientationTextView.setText(getResources().getStringArray(R.array.screen_orientations)[orientation]);
    }

    //endregion


    //region Callback Interfaces

    /** A callback interface for receiving settings events. */
    public interface SettingsEventCallback
    {
        /** Raised when the back button on the settings page has been pressed. */
        public void onBack();
    }

    //endregion
}
