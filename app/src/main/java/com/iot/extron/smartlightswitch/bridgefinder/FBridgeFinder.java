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

package com.iot.extron.smartlightswitch.bridgefinder;


import android.app.FragmentTransaction;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.iot.extron.smartlightswitch.AMain;
import com.iot.extron.smartlightswitch.R;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscovery;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscoveryCallback;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscoveryResult;
import com.philips.lighting.hue.sdk.wrapper.domain.ReturnCode;

import java.util.List;

/**
 * A {@link Fragment} used to discover Hue bridges and allow the user to pick one to connect to.  This fragment does not have its own UI, but instead maintains its UI state through child fragments.
 */
public class FBridgeFinder extends Fragment
{
    //region Static Fields

    private static final String TAG = "FBridgeFinder";

    //endregion


    //region Fields

    /** The bridge finder used to find bridges on the same local network. */
    BridgeDiscovery bridgeFinder;

    /** The latest results from the bridge discovery. */
    List<BridgeDiscoveryResult> results;

    //endregion


    //region Constructors

    /** Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon screen orientation changes). */
    public FBridgeFinder()
    {
    }

    //endregion


    //region Fragment Lifecycle Methods

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.f_bridgefinder, container, false);

        goToSearchForBridges();

        return view;
    }

    //endregion


    /** Configures the UI to search for bridges. */
    private void goToSearchForBridges()
    {
        Fragment searchFrag = FSearchForBridges.newInstance(new FSearchForBridges.OnBridgeSearchListener()
        {
            @Override
            public void startSearch()
            {
                bridgeFinder = new BridgeDiscovery();

                bridgeFinder.search(BridgeDiscovery.BridgeDiscoveryOption.ALL, new BridgeDiscoveryCallback()
                {
                    @Override
                    public void onFinished(final List<BridgeDiscoveryResult> list, final ReturnCode returnCode)
                    {
                        bridgeFinder = null;

                        getActivity().runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                if (returnCode == ReturnCode.SUCCESS)
                                {
                                    Log.i(TAG, "Bridge discovery finished successfully and found " + list.size() + " bridges.");
                                    goToFoundBridges(list);
                                }
                                else if (returnCode == ReturnCode.STOPPED)
                                {
                                    Log.i(TAG, "Bridge discovery stopped.");
                                }
                                else
                                {
                                    Log.i(TAG, "Bridge discovery failed with return code: " + returnCode);
                                }
                            }
                        });
                    }
                });
            }

            @Override
            public void cancelSearch()
            {
                if (bridgeFinder != null)
                {
                    bridgeFinder.stop();
                    bridgeFinder = null;
                }
            }
        });

        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.mainLayout, searchFrag);
        transaction.commit();
    }

    /** Configures the UI to display the found bridges. */
    private void goToFoundBridges(List<BridgeDiscoveryResult> list)
    {
        results = list;
        FFoundBridges foundBridgesFrag = FFoundBridges.newInstance(list, new FFoundBridges.OnBridgesFoundListener()
        {
            @Override
            public void bridgeSelected(BridgeDiscoveryResult bridge)
            {
                goToConnectToBridge(bridge);
            }

            @Override
            public void cancelled()
            {
                goToSearchForBridges();
            }
        });

        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.mainLayout, foundBridgesFrag);
        transaction.commit();
    }

    /** Configures the UI to display the "Connecting to Bridge" UI. */
    private void goToConnectToBridge(BridgeDiscoveryResult selectedBridge)
    {
        FConnectToBridge connectFrag = FConnectToBridge.newInstance(selectedBridge.getUniqueID(), selectedBridge.getIP(), new FConnectToBridge.ConnectToBridgeCallback()
        {
            @Override
            public void connected()
            {
                ((AMain)getActivity()).showLightswitch();
            }

            @Override
            public void cancelled()
            {
                goToFoundBridges(results);
            }

            @Override
            public void failed()
            {
                goToFoundBridges(results);
            }
        });

        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.mainLayout, connectFrag);
        transaction.commit();
    }
}
