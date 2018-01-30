package com.iot.extron.smartlightswitch;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.iot.extron.smartlightswitch.bridgefinder.FBridgeFinder;
import com.iot.extron.smartlightswitch.bridgefinder.FConnectToBridge;
import com.iot.extron.smartlightswitch.lightswitch.FLightswitch;
import com.iot.extron.smartlightswitch.utilities.TextUtilities;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnection;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnectionCallback;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnectionType;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeStateUpdatedCallback;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeStateUpdatedEvent;
import com.philips.lighting.hue.sdk.wrapper.connection.ConnectionEvent;
import com.philips.lighting.hue.sdk.wrapper.domain.Bridge;
import com.philips.lighting.hue.sdk.wrapper.domain.BridgeBuilder;
import com.philips.lighting.hue.sdk.wrapper.domain.HueError;
import com.philips.lighting.hue.sdk.wrapper.knownbridges.KnownBridge;
import com.philips.lighting.hue.sdk.wrapper.knownbridges.KnownBridges;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class AMain extends Activity
{
    //region Nested Enums

    enum UIState
    {
        FindingBridges,
        ConnectingToKnownBridge,
        Lightswitch
    }

    //endregion


    //region Static Fields

    private static final String TAG = "AMain";

    //endregion


    //region Fields

    /** A list of bridge event callbacks to send events to. */
    List<BridgeEventCallback> bridgeEventCallbacks = new ArrayList<>();

    /** The bridge that the app is currently connected to. */
    Bridge bridge;

    /** The IP Address of the bridge the app is currently connected to. */
    String bridgeIp;

    /** Keeps track of the current UI state. */
    UIState uiState;

    /** Indicates that the user initiated a manual disconnect. */
    boolean manualDisconnect;

    /** The most recent errors received from the bridge. */
    List<HueError> latestErrors = new ArrayList<>();

    //endregion


    //region UI Elements

    ViewGroup mainLayout;
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    View navigationHeaderView;
    TextView switchIpTextView;
    TextView bridgeNameTextView;
    TextView bridgeIpTextView;

    //endregion


    //region Activity Lifecycle Methods

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_main);

        mainLayout = (ViewGroup)findViewById(R.id.mainLayout);
        drawerLayout = (DrawerLayout)findViewById(R.id.DrawerLayout);

        navigationView = (NavigationView)findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener()
        {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item)
            {
                switch (item.getItemId())
                {
                    case R.id.disconnectBridge:
                        disconnectFromBridge();
                        goToBridgeFinder();
                        drawerLayout.closeDrawers();
                        return true;

                    case R.id.shutdown:
                        // TODO: Not sure I want to have a power option yet, but for dev purposes keeping it in.
                        try
                        {
                            Runtime.getRuntime().exec("reboot -p");
                        }
                        catch (IOException e)
                        {
                        }
                        return true;

                    default:
                        return false;
                }
            }
        });

        navigationHeaderView = navigationView.getHeaderView(0);

        switchIpTextView = (TextView)navigationHeaderView.findViewById(R.id.switchIpTextView);
        bridgeNameTextView = (TextView)navigationHeaderView.findViewById(R.id.bridgeNameTextView);
        bridgeIpTextView = (TextView)navigationHeaderView.findViewById(R.id.bridgeIpTextView);

        WifiManager wifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
        String switchIp;

        try
        {
            int ipAddress = wifiManager.getDhcpInfo().ipAddress;
            byte[] addressBytes = { (byte)(0xff & ipAddress), (byte)(0xff & (ipAddress >> 8)), (byte)(0xff & (ipAddress >> 16)), (byte)(0xff & (ipAddress >> 24)) };

            switchIp = InetAddress.getByAddress(addressBytes).getHostAddress();
        }
        catch (UnknownHostException e)
        {
            switchIp = getResources().getString(R.string.unknown);
        }

        switchIpTextView.setText(TextUtilities.getStringWithSub(getResources(), R.string.ip_address, switchIp));

        bridgeNameTextView.setText(TextUtilities.getStringWithSub(getResources(), R.string.bridge_name, getResources().getString(R.string.not_connected)));
        bridgeIpTextView.setText(TextUtilities.getStringWithSub(getResources(), R.string.ip_address, getResources().getString(R.string.not_connected)));

        addBridgeEventCallback(new BridgeEventCallback()
        {
            @Override
            public void bridgeInitialized(final Bridge bridge)
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        bridgeNameTextView.setText(TextUtilities.getStringWithSub(getResources(), R.string.bridge_name, bridge.getName()));
                        bridgeIpTextView.setText(TextUtilities.getStringWithSub(getResources(), R.string.ip_address, bridgeIp));
                    }
                });
            }
        });

        KnownBridge lastUsedBridge = getLastUsedBridge();

        if (lastUsedBridge == null)
        {
            goToBridgeFinder();
        }
        else
        {
            goToConnectToBridge(lastUsedBridge);
        }
    }

    //endregion


    /** Gets the layout that a snackbar can be attached to.
     * @return The snackbar layout.
     */
    public ViewGroup getLayoutForSnackbar()
    {
        return mainLayout;
    }


    /** Gets the IP address of the last used bridge, or <code>null</code> if no last bridge was found.
     * @return The IP address of the last bridge, or <code>null</code> if none was found.
     */
    private KnownBridge getLastUsedBridge()
    {
        List<KnownBridge> bridges = KnownBridges.getAll();

        if (bridges.isEmpty())
        {
            return null;
        }

        return Collections.max(bridges, new Comparator<KnownBridge>()
        {
            @Override
            public int compare(KnownBridge a, KnownBridge b)
            {
                return a.getLastConnected().compareTo(b.getLastConnected());
            }
        });
    }

    /** Creates a new bridge and attempts to connect to it.
     * @param bridgeIp The IP address of the bridge to connect to.
     */
    public void connectToBridge(String bridgeIp)
    {
        manualDisconnect = false;
        latestErrors.clear();

        this.bridgeIp = bridgeIp;

        bridge = new BridgeBuilder(getResources().getString(R.string.app_name), Build.MODEL)
                .setIpAddress(bridgeIp)
                .setConnectionType(BridgeConnectionType.LOCAL)
                .setBridgeConnectionCallback(bridgeConnectionCallback)
                .addBridgeStateUpdatedCallback(bridgeStateUpdatedCallback)
                .build();

        bridge.connect();
    }

    /** If the current bridge is not connected, attempts to connect to it. */
    public void reconnectToBridge()
    {
        manualDisconnect = false;
        latestErrors.clear();

        if (bridge != null && !bridge.isConnected())
            bridge.connect();
    }

    /** Disconnects from the current bridge. */
    public void disconnectFromBridge()
    {
        if (bridge != null)
        {
            manualDisconnect = true;
            latestErrors.clear();

            bridge.disconnect();
            bridgeIp = null;
            bridge = null;
        }
    }

    /** Gets the bridge that the app is currently connected to. */
    public Bridge getBridge()
    {
        return bridge;
    }

    /** Adds a {@link BridgeEventCallback} to the list of callbacks receiving events from the current bridge.
     * @param callback The callback to add.
     */
    public void addBridgeEventCallback(BridgeEventCallback callback)
    {
        bridgeEventCallbacks.add(callback);
    }

    /** Removes a {@link BridgeEventCallback} from the list of callbacks receiving events from the current bridge.
     * @param callback The callback to remove.
     */
    public void removeBridgeEventCallback(BridgeEventCallback callback)
    {
        bridgeEventCallbacks.remove(callback);
    }

    /** Displays the lightswitch UI. */
    public void showLightswitch()
    {
        goToLightswitch();
    }


    //region UI State Management

    /** Creates a new {@link FBridgeFinder} and sets it as the current fragment. */
    private void goToBridgeFinder()
    {
        uiState = UIState.FindingBridges;

        Fragment frag = new FBridgeFinder();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        transaction.replace(R.id.mainLayout, frag);
        transaction.commit();
    }

    /** Creates a new {@link FConnectToBridge} and sets it as the current fragment.
     * @param bridge The bridge to pass to the fragment, which will attempt to connect to it.
     */
    private void goToConnectToBridge(KnownBridge bridge)
    {
        uiState = UIState.ConnectingToKnownBridge;

        Fragment frag = FConnectToBridge.newInstance(bridge.getName(), bridge.getIpAddress(), new FConnectToBridge.ConnectToBridgeCallback()
        {
            @Override
            public void connected()
            {
                goToLightswitch();
            }

            @Override
            public void cancelled()
            {
                goToBridgeFinder();
            }

            @Override
            public void failed()
            {
                goToBridgeFinder();
            }
        });

        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        transaction.replace(R.id.mainLayout, frag);
        transaction.commit();
    }

    /** Creates a new {@link FLightswitch} and sets it as the current fragment. */
    private void goToLightswitch()
    {
        uiState = UIState.Lightswitch;

        Fragment frag = new FLightswitch();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        transaction.replace(R.id.mainLayout, frag);
        transaction.commit();
    }

    //endregion


    //region BridgeConnectionCallback Object

    /** The callback that receives bridge connection events. */
    private BridgeConnectionCallback bridgeConnectionCallback = new BridgeConnectionCallback()
    {
        @Override
        public void onConnectionEvent(BridgeConnection bridgeConnection, ConnectionEvent connectionEvent)
        {
            Log.i(TAG, "Received connection event: " + connectionEvent);

            for (BridgeEventCallback callback : bridgeEventCallbacks)
            {
                switch (connectionEvent)
                {
                    case CONNECTED:
                        callback.bridgeConnected(bridge);
                        break;

                    case CONNECTION_LOST:
                        callback.bridgeConnectionLost(bridge);
                        break;

                    case CONNECTION_RESTORED:
                        callback.bridgeConnectionRestored(bridge);
                        break;

                    case COULD_NOT_CONNECT:
                        callback.bridgeCouldNotConnect(bridge);
                        break;

                    case NOT_AUTHENTICATED:
                    case LINK_BUTTON_NOT_PRESSED:
                        callback.bridgePushlinkRequested(bridge);
                        break;

                    case AUTHENTICATED:
                        callback.bridgeAuthenticated(bridge);
                        break;

                    case DISCONNECTED:
                        callback.bridgeDisconnected(bridge, manualDisconnect, latestErrors);
                        break;
                }
            }
        }

        @Override
        public void onConnectionError(BridgeConnection bridgeConnection, List<HueError> list)
        {
            StringBuilder errorMsg = new StringBuilder();

            for (HueError error : list)
                errorMsg.append(error.toString() + "\n");

            Log.e(TAG, "Received connection errors:\n" + errorMsg.toString());

            latestErrors = list;
        }
    };

    //endregion


    //region BridgeStateUpdatedCallback Object

    /** The callback that receives bridge state update events. */
    private BridgeStateUpdatedCallback bridgeStateUpdatedCallback = new BridgeStateUpdatedCallback()
    {
        @Override
        public void onBridgeStateUpdated(Bridge bridge, BridgeStateUpdatedEvent bridgeStateUpdatedEvent)
        {
            Log.i(TAG, "Received bridge state update event: " + bridgeStateUpdatedEvent);

            for (BridgeEventCallback callback : bridgeEventCallbacks)
            {
                switch (bridgeStateUpdatedEvent)
                {
                    case INITIALIZED:
                        callback.bridgeInitialized(bridge);
                        break;

                    case BRIDGE_CONFIG:
                        callback.updatedBridgeConfig(bridge);
                        break;

                    case LIGHTS_AND_GROUPS:
                        callback.updatedLightsAndGroups(bridge);
                        break;

                    case SCENES:
                        callback.updatedScenes(bridge);
                        break;

                    case SENSORS_AND_SWITCHES:
                        callback.updatedSensorsAndSwitches(bridge);
                        break;

                    case RULES:
                        callback.updatedRules(bridge);
                        break;

                    case SCHEDULES_AND_TIMERS:
                        callback.updatedSchedulesAndTimers(bridge);
                        break;
                }
            }
        }
    };

    //endregion
}
