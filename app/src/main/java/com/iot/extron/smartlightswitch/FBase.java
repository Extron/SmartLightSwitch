package com.iot.extron.smartlightswitch;

import android.app.Fragment;

import com.philips.lighting.hue.sdk.wrapper.domain.Bridge;

/** A base fragment that contains general functionality useful to all app fragments. */
public abstract class FBase extends Fragment
{
    /** Gets the {@link Bridge} that the app is currently connected to.
     * @return The connected {@link Bridge}.
     */
    protected Bridge getBridge()
    {
        return ((AMain)getActivity()).getBridge();
    }

    /** Gets the fragment's owning activity and casts it to {@link AMain}.
     * @return The owning {@link AMain} activity.
     */
    protected AMain getMainActivity()
    {
        return (AMain)getActivity();
    }

    /** Gets the {@link android.app.Application} object of the app.
     * @return The app's {@link android.app.Application}.
     */
    protected SLSApplication getApplication()
    {
        return (SLSApplication)getActivity().getApplication();
    }
}
