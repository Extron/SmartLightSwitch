package com.iot.extron.smartlightswitch.bridgefinder;


import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.iot.extron.smartlightswitch.R;

/**
 * A {@link Fragment} that displays the status if an active bridge search.
 */
public class FSearchForBridges extends Fragment
{
    //region Fields

    OnBridgeSearchListener onBridgeSearchListener;

    //endregion


    //region UI Elements

    ViewGroup findBridgesLayout;
    ViewGroup progressLayout;
    Button findBridgesButton;
    Button cancelButton;

    //endregion


    //region Constructors

    /** Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon screen orientation changes). */
    public FSearchForBridges()
    {
    }

    /** Creates a new fragment to search for bridges.
     * @param onBridgeSearchListener The listener to receive UI events with.
     */
    public static FSearchForBridges newInstance(OnBridgeSearchListener onBridgeSearchListener)
    {
        FSearchForBridges fragment = new FSearchForBridges();
        fragment.onBridgeSearchListener = onBridgeSearchListener;
        return fragment;
    }

    //endregion


    //region Fragment Lifecycle Methods

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.f_searchforbridges, container, false);

        findBridgesLayout = (ViewGroup)view.findViewById(R.id.findBridgesLayout);
        progressLayout = (ViewGroup)view.findViewById(R.id.progressLayout);
        findBridgesButton = (Button)view.findViewById(R.id.findBridgesButton);
        cancelButton = (Button)view.findViewById(R.id.cancelButton);

        findBridgesButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                onBridgeSearchListener.startSearch();

                findBridgesLayout.setVisibility(View.GONE);
                progressLayout.setVisibility(View.VISIBLE);
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                onBridgeSearchListener.cancelSearch();

                findBridgesLayout.setVisibility(View.VISIBLE);
                progressLayout.setVisibility(View.GONE);
            }
        });

        return view;
    }

    //endregion


    //region Nested Interfaces

    /** A callback interface for events related to bridge searching. */
    public interface OnBridgeSearchListener
    {
        /** Raised when the user has started the search. */
        public void startSearch();

        /** Raised when the user has cancelled the search. */
        public void cancelSearch();
    }

    //endregion
}
