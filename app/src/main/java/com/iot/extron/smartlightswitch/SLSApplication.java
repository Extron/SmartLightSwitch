package com.iot.extron.smartlightswitch;

import android.app.Application;
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
            ScreenManager screenManager = new ScreenManager(Display.DEFAULT_DISPLAY);
            screenManager.lockRotation(ScreenManager.ROTATION_90);
        }
        catch (NoClassDefFoundError e)
        {
            // Catching this error because ScreenManager only exists on Android Things, but in the case that this app is running on another Android device (e.g. emulator for testing)
            // we can still proceed without the above code.
            Log.e(TAG, "ScreenManager class could not be found: " + e.getMessage());
        }
    }
}