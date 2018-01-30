package com.iot.extron.smartlightswitch.bridgefinder;

import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import com.philips.lighting.hue.sdk.wrapper.domain.Bridge;
import com.philips.lighting.hue.sdk.wrapper.domain.HueError;

import com.iot.extron.smartlightswitch.AMain;
import com.iot.extron.smartlightswitch.BridgeEventCallback;
import com.iot.extron.smartlightswitch.R;



/**
 * A {@link Fragment} that manages connecting to a bridge.
 */
public class FConnectToBridge extends Fragment
{
    //region Static Fields

    private static final String TAG = "FConnectToBridge";

    //endregion


    //region Fields

    /** A callback to listen for bridge connection UI events. */
    ConnectToBridgeCallback connectToBridgeCallback;

    /** The name of the bridge to display to the user. */
    String bridgeName;

    /** The IP address of the bridge to connect to. */
    String bridgeIp;

    //endregion


    //region UI Elements

    ViewGroup progressLayout;
    ViewGroup connectFailedLayout;
    ViewGroup connectErrorLayout;
    TextView connectingTextView;
    TextView statusTextView;
    TextView failedToConnectTextView;
    TextView connectionErrorTextView;
    Button cancelButton;
    Button failedBackButton;
    Button failedTryAgainButton;
    Button errorBackButton;
    Button errorTryAgainButton;

    //endregion


    //region Constructors

    /** Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon screen orientation changes). */
    public FConnectToBridge()
    {
    }


    /** Creates a new fragment to connect to a bridges.
     * @param bridgeName The display name of the bridge to connect to.
     * @param bridgeIp The IP address of the bridge to connect to.
     * @param connectToBridgeCallback The callback to receive UI events from this fragment.
     */
    public static FConnectToBridge newInstance(String bridgeName, String bridgeIp, ConnectToBridgeCallback connectToBridgeCallback)
    {
        FConnectToBridge fragment = new FConnectToBridge();
        fragment.bridgeName = bridgeName;
        fragment.bridgeIp = bridgeIp;
        fragment.connectToBridgeCallback = connectToBridgeCallback;
        return fragment;
    }

    //endregion


    //region Fragment Lifecycle Methods

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.f_connecttobridge, container, false);

        progressLayout = (ViewGroup)view.findViewById(R.id.progressLayout);
        connectFailedLayout = (ViewGroup)view.findViewById(R.id.connectFailedLayout);
        connectErrorLayout = (ViewGroup)view.findViewById(R.id.connectErrorLayout);

        connectingTextView = (TextView)view.findViewById(R.id.connectingTextView);
        connectingTextView.setText(getResources().getString(R.string.connecting_to_bridge).replace("{0}", bridgeName).replace("{1}", bridgeIp));

        statusTextView = (TextView)view.findViewById(R.id.statusTextView);

        failedToConnectTextView = (TextView)view.findViewById(R.id.failedToConnectTextView);
        failedToConnectTextView.setText(getResources().getString(R.string.failed_to_connect).replace("{0}", bridgeName).replace("{1}", bridgeIp));

        connectionErrorTextView = (TextView)view.findViewById(R.id.connectionErrorTextView);
        connectionErrorTextView.setText(getResources().getString(R.string.error_on_connect).replace("{0}", bridgeName).replace("{1}", bridgeIp));

        cancelButton = (Button)view.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                ((AMain)getActivity()).disconnectFromBridge();

                if (connectToBridgeCallback != null)
                {
                    connectToBridgeCallback.cancelled();
                }
            }
        });

        failedBackButton = (Button)view.findViewById(R.id.failedBackButton);
        failedBackButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (connectToBridgeCallback != null)
                {
                    connectToBridgeCallback.failed();
                }
            }
        });

        failedTryAgainButton = (Button)view.findViewById(R.id.failedTryAgainButton);
        failedTryAgainButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                ((AMain)getActivity()).reconnectToBridge();

                progressLayout.setVisibility(View.VISIBLE);
                connectFailedLayout.setVisibility(View.GONE);
            }
        });

        errorBackButton = (Button)view.findViewById(R.id.errorBackButton);
        errorBackButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (connectToBridgeCallback != null)
                {
                    connectToBridgeCallback.failed();
                }
            }
        });

        errorTryAgainButton = (Button)view.findViewById(R.id.errorTryAgainButton);
        errorTryAgainButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                ((AMain)getActivity()).reconnectToBridge();

                progressLayout.setVisibility(View.VISIBLE);
                connectErrorLayout.setVisibility(View.GONE);
            }
        });

        return view;
    }

    @Override
    public void onStart()
    {
        super.onStart();

        connectToBridge();
    }

    @Override
    public void onDetach()
    {
        super.onDetach();

        ((AMain)getActivity()).removeBridgeEventCallback(bridgeEventCallback);
    }

    //endregion


    /**
     * Use the BridgeBuilder to connect to the desired bridge.
     */
    private void connectToBridge()
    {
        AMain main = (AMain)getActivity();

        main.addBridgeEventCallback(bridgeEventCallback);
        main.connectToBridge(bridgeIp);
    }


    //region BridgeEventCallback

    private BridgeEventCallback bridgeEventCallback = new BridgeEventCallback()
    {
        @Override
        public void bridgeConnected(Bridge bridge)
        {
            getActivity().runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    statusTextView.setText(R.string.connected);
                }
            });
        }

        @Override
        public void bridgePushlinkRequested(Bridge bridge)
        {
            getActivity().runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    statusTextView.setText(R.string.press_link);
                }
            });
        }

        @Override
        public void bridgeAuthenticated(Bridge bridge)
        {
            getActivity().runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    statusTextView.setText(R.string.authenticated);
                }
            });
        }

        @Override
        public void bridgeCouldNotConnect(Bridge bridge)
        {
            getActivity().runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    progressLayout.setVisibility(View.GONE);
                    connectFailedLayout.setVisibility(View.VISIBLE);
                }
            });
        }

        @Override
        public void bridgeDisconnected(Bridge bridge, boolean manualDisconnect, List<HueError> errors)
        {
            if (!manualDisconnect)
            {
                final StringBuilder errorMsg = new StringBuilder();

                for (HueError error : errors)
                {
                    Log.e(TAG, "Connection error: " + error.toString());
                    errorMsg.append(error.toString() + "\n");
                }

                getActivity().runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        progressLayout.setVisibility(View.GONE);
                        connectErrorLayout.setVisibility(View.VISIBLE);
                        connectionErrorTextView.setText(getResources().getString(R.string.error_on_connect).replace("{0}", bridgeName).replace("{1}", bridgeIp).replace("{2}", errorMsg.toString()));
                    }
                });
            }
        }

        @Override
        public void bridgeInitialized(Bridge bridge)
        {
            if (connectToBridgeCallback != null)
            {
                connectToBridgeCallback.connected();
            }
        }
    };

    //endregion


    //region Nested Interfaces

    /** A callback interface for events related to bridge searching. */
    public interface ConnectToBridgeCallback
    {
        /** Raised when the bridge has connected successfully. */
        public void connected();

        /** Raised when the user has pressed the "Cancel" button. */
        public void cancelled();

        /** Raised when connecting to the bridge has failed and the user has pressed "OK". */
        public void failed();
    }

    //endregion
}
