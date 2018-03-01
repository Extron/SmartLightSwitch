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

package com.iot.extron.smartlightswitch;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.Display;

import com.google.android.things.device.DeviceManager;
import com.google.android.things.device.ScreenManager;
import com.philips.lighting.hue.sdk.wrapper.HueLog;
import com.philips.lighting.hue.sdk.wrapper.Persistence;
import com.philips.lighting.hue.sdk.wrapper.domain.ReturnCode;

import java.io.IOException;

/**
 * The SLS Application object, which manages talking to the Hue SDK for the entire app.
 */

public class SLSApplication extends Application
{
    private static final String TAG = "SLSApplication";

    static
    {
        // Load the huesdk native library before calling any SDK method
        System.loadLibrary("huesdk");
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        Log.i(TAG, "Initializing application");

        // Configure the storage location and log level for the Hue SDK
        ReturnCode result = Persistence.setStorageLocation(getFilesDir().getAbsolutePath(), getResources().getString(R.string.app_name));
        HueLog.setConsoleLogLevel(HueLog.LogLevel.INFO);

        Log.i(TAG, "setStorageLocation returned with a " + result);

        // In order to rotate the screen, need to have appropriate settings.  However, as per https://stackoverflow.com/questions/46435014/android-things-permission-com-google-android-things-permission-manage-input-driv
        // these settings are marked as dangerous.  Normal Android asks users permission to turn on, but can't on Android Things.  The only way to get permissions set is
        // to reboot the device.  So, if we can't find the screen settings, reboot.
        if (checkSelfPermission("com.google.android.things.permission.MODIFY_SCREEN_SETTINGS") == PackageManager.PERMISSION_DENIED)
        {
            try
            {
                Runtime.getRuntime().exec("reboot");
            }
            catch (IOException e)
            {
            }
        }

        try
        {
            SharedPreferences preferences = getSettingsPreferences();
            int orientation = preferences.getInt(SETTINGS_SCREEN_ORIENTATION, ScreenManager.ROTATION_90);

            ScreenManager screenManager = new ScreenManager(Display.DEFAULT_DISPLAY);
            screenManager.lockRotation(orientation);
        }
        catch (NoClassDefFoundError e)
        {
            // Catching this error because ScreenManager only exists on Android Things, but in the case that this app is running on another Android device (e.g. emulator for testing)
            // we can still proceed without the above code.
            Log.e(TAG, "ScreenManager class could not be found: " + e.getMessage());
        }
    }


    //region Storage

    private static final String SETTINGS_PREFERENCES = "settings";
    public static final String SETTINGS_SCREEN_ORIENTATION = "screen_orientation";

    /** Gets the {@link SharedPreferences} that app settings can be stored in.
     * @return The app settings {@link SharedPreferences}.
     */
    public SharedPreferences getSettingsPreferences()
    {
        return getSharedPreferences(SETTINGS_PREFERENCES, Context.MODE_PRIVATE);
    }

    //endregion
}
