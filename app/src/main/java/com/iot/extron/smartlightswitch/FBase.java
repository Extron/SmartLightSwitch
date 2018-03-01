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
