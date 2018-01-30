package com.iot.extron.smartlightswitch.bridgefinder;

import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscoveryResult;

import com.iot.extron.smartlightswitch.R;

import java.util.List;

/**
 * A fragment representing a list of {@link BridgeDiscoveryResult}s.
 */
public class FFoundBridges extends Fragment
{
    //region Fields

    /** The list of bridges found to display to the user. */
    List<BridgeDiscoveryResult> foundBridges;

    /** The callback listener to send events to. */
    OnBridgesFoundListener onBridgesFoundListener;

    //endregion


    //region UI Elements

    RecyclerView bridgesRecyclerView;
    Button okButton;
    Button cancelButton;

    //endregion


    //region Constructors

    /** Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon screen orientation changes). */
    public FFoundBridges()
    {
    }

    /** Creates a new fragment to display the specified found bridges.
     * @param foundBridges The bridges to display.
     * @param onBridgesFoundListener The listener to receive UI events with.
     */
    public static FFoundBridges newInstance(List<BridgeDiscoveryResult> foundBridges, OnBridgesFoundListener onBridgesFoundListener)
    {
        FFoundBridges fragment = new FFoundBridges();
        fragment.foundBridges = foundBridges;
        fragment.onBridgesFoundListener = onBridgesFoundListener;
        return fragment;
    }

    //endregion


    //region Lifecycle Methods

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.f_foundbridges, container, false);

        bridgesRecyclerView = (RecyclerView)view.findViewById(R.id.BridgesRecyclerView);
        bridgesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        bridgesRecyclerView.setAdapter(new FoundBridgesRVAdapter(foundBridges));

        okButton = (Button)view.findViewById(R.id.okButton);
        cancelButton = (Button)view.findViewById(R.id.cancelButton);

        okButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                onBridgesFoundListener.bridgeSelected(((FoundBridgesRVAdapter)bridgesRecyclerView.getAdapter()).getSelectedItem());
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                onBridgesFoundListener.cancelled();
            }
        });
        return view;
    }

    //endregion


    //region RecyclerView Adapter

    /** {@link RecyclerView.Adapter} that can display a {@link BridgeDiscoveryResult}. */
    class FoundBridgesRVAdapter extends RecyclerView.Adapter<FoundBridgesRVAdapter.ViewHolder>
    {
        //region Fields

        /** The list of items managed by this adapter. */
        final List<BridgeDiscoveryResult> items;

        /** The index of the selected item. */
        int selectedItem;

        //endregion


        //region Constructors

        public FoundBridgesRVAdapter(List<BridgeDiscoveryResult> items)
        {
            this.items = items;
            selectedItem = -1;
        }

        //endregion


        //region RecyclerView.Adapter Methods

        @Override
        public FoundBridgesRVAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.v_knownbridge, parent, false);
            return new FoundBridgesRVAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(FoundBridgesRVAdapter.ViewHolder holder, int position)
        {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount()
        {
            return items.size();
        }

        //endregion


        //region Item Selection

        /** Gets the currently selected item in the recycler view.
         * @return The selected item, or <code>null</code> if no item is selected.
         */
        public BridgeDiscoveryResult getSelectedItem()
        {
            if (selectedItem > -1)
                return items.get(selectedItem);
            else
                return null;
        }

        /** Changes the currently selected item to be the specified item.
         * @param position The position of the item to select.
         */
        void itemSelected(int position)
        {
            int oldSelection = selectedItem;
            selectedItem = position;
            notifyItemChanged(oldSelection);
            notifyItemChanged(selectedItem);

            okButton.setEnabled(true);
        }

        //endregion


        //region ViewHolder

        /** Manages the view of a list item within the {@link RecyclerView} */
        class ViewHolder extends RecyclerView.ViewHolder
        {
            BridgeDiscoveryResult item;

            final TextView bridgeNameTextView;
            final TextView bridgeIpTextView;

            public ViewHolder(View view)
            {
                super(view);

                bridgeNameTextView = (TextView)itemView.findViewById(R.id.bridgeNameTextView);
                bridgeIpTextView = (TextView)itemView.findViewById(R.id.BridgeIpTextView);

                itemView.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        itemSelected(getAdapterPosition());
                    }
                });
            }

            /** Binds the view holder to a {@link BridgeDiscoveryResult} list item.
             * @param item The {@link BridgeDiscoveryResult} item to bind to.
             */
            public void bind(BridgeDiscoveryResult item)
            {
                this.item = item;

                bridgeNameTextView.setText(item.getUniqueID());
                bridgeIpTextView.setText(item.getIP());

                itemView.setActivated(getAdapterPosition() == selectedItem);
            }
        }

        //endregion
    }

    //endregion


    //region Nested Callback Interfaces

    /** A callback interface for events related to selecting from found bridges. */
    public interface OnBridgesFoundListener
    {
        /** Raised when the user has selected a bridge to connect to. */
        public void bridgeSelected(BridgeDiscoveryResult bridge);

        /** Raised when the user has cancelled. */
        public void cancelled();
    }

    //endregion
}
