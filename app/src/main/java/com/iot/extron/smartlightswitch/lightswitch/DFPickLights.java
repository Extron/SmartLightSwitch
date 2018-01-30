package com.iot.extron.smartlightswitch.lightswitch;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightPoint;

import com.iot.extron.smartlightswitch.R;
import com.philips.lighting.hue.sdk.wrapper.domain.resource.Group;

/**
 * Manages a dialog that allows users to pick one or more lights to manage with the light switch.
 */
public class DFPickLights extends DialogFragment
{
    //region Fields

    /** A callback used to notify that lights were selected. */
    OnLightsSelectedCallback onLightsSelectedCallback;

    /** The list of lights being displayed in the dialog. */
    List<LightPoint> lights;

    /** Keeps track of which lights have been selected. */
    List<Integer> selectedLights;

    /** The list of groups being displayed in the dialog. */
    List<Group> groups;

    /** Keeps track of which lights have been selected. */
    List<Integer> selectedGroups;

    //endregion


    //region UI Elements

    RecyclerView recyclerView;
    LightsRVAdapter adapter;

    //endregion

    //region Constructors

    /** Creates a new dialog fragment to pick lights.
     * @param lights The list of lights to display.
     */
    public static DFPickLights newInstance(List<LightPoint> lights, List<Integer> selectedLights, List<Group> groups, List<Integer> selectedGroups, OnLightsSelectedCallback onLightsSelectedCallback)
    {
        DFPickLights fragment = new DFPickLights();
        fragment.onLightsSelectedCallback = onLightsSelectedCallback;
        fragment.lights = lights;
        fragment.selectedLights = selectedLights;
        fragment.groups = groups;
        fragment.selectedGroups = selectedGroups;
        return fragment;
    }

    //endregion


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        View customView = getActivity().getLayoutInflater().inflate(R.layout.v_picklights, null);

        recyclerView = customView.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new LightsRVAdapter();

        List<String> lightItems = lights.stream().map(new Function<LightPoint, String>()
        {
            @Override
            public String apply(LightPoint lightPoint)
            {
                return lightPoint.getName();
            }
        }).collect(Collectors.<String>toList());

        List<String> groupItems = groups.stream().map(new Function<Group, String>()
        {
            @Override
            public String apply(Group group)
            {
                return group.getName();
            }
        }).collect(Collectors.<String>toList());

        adapter.addSection(getResources().getString(R.string.groups), groupItems, selectedGroups);
        adapter.addSection(getResources().getString(R.string.lights), lightItems, selectedLights);

        recyclerView.setAdapter(adapter);

        builder
            .setTitle(R.string.lights)
            .setIcon(R.drawable.ic_lightbulb_outline_white_24dp)
            .setView(customView)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int id)
                {
                    if (onLightsSelectedCallback != null)
                    {
                        List<LightPoint> selectedLights = new ArrayList<>();
                        List<Group> selectedGroups = new ArrayList<>();

                        List<List<Integer>> selectedItems = adapter.getSelectedItems();

                        for (Integer item : selectedItems.get(0))
                            selectedGroups.add(groups.get(item));

                        for (Integer item : selectedItems.get(1))
                            selectedLights.add(lights.get(item));

                        onLightsSelectedCallback.onLightsSelected(selectedLights, selectedGroups);
                    }
                }
            })
            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int id)
                {

                }
            });

        return builder.create();
    }

    //region RecyclerView Adapter

    /** {@link RecyclerView.Adapter} that can display a a sectioned list of light and group names. */
    class LightsRVAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
    {
        //region Static Fields

        /** The type integer used to identify header types. */
        private static final int HEADER_TYPE = 0;

        /** The type integer used to identify header types. */
        private static final int ITEM_TYPE = 1;

        //endregion


        //region Fields

        /** A list of sections, each of which contains a list of items to display under that section. */
        List<Section> sections;

        /** The index of the selected item. */
        List<Integer> selectedItems = new ArrayList<>();

        //endregion


        //region Constructors

        public LightsRVAdapter()
        {
            this.sections = new ArrayList<>();
        }

        //endregion


        //region RecyclerView.Adapter Methods

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            switch (viewType)
            {
                case HEADER_TYPE:
                    View headerView = LayoutInflater.from(parent.getContext()).inflate(R.layout.v_lightitemheader, parent, false);
                    return new HeaderViewHolder(headerView);

                case ITEM_TYPE:
                default:
                    View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.v_selectablelightitem, parent, false);
                    return new ItemViewHolder(itemView);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position)
        {
            String rawItem = getItem(position);
            int itemType = getItemViewType(position);

            switch (itemType)
            {
                case HEADER_TYPE:
                    ((HeaderViewHolder)holder).bind(rawItem);
                    break;

                case ITEM_TYPE:
                default:
                    ((ItemViewHolder)holder).bind(rawItem);
                    break;
            }
        }

        @Override
        public int getItemCount()
        {
            // The total number of items in the list is the number of headers plus the sum of the sizes of all of the items in all collections.
            return sections.size() + sections.stream().map(new Function<Section, Integer>()
            {
                @Override
                public Integer apply(Section section)
                {
                    return section.items.size();
                }
            }).reduce(0, new BinaryOperator<Integer>()
            {
                @Override
                public Integer apply(Integer current, Integer element)
                {
                    return current + element;
                }
            });
        }

        @Override
        public int getItemViewType(int position)
        {
            // Flatten the sections into a single list, adding the headers in with the items.
            List<Integer> types = new ArrayList<>();

            for (Section section : sections)
            {
                types.add(HEADER_TYPE);

                for (String item : section.items)
                    types.add(ITEM_TYPE);
            }

            return types.get(position);
        }

        //endregion

        /** Adds a new section to the adapter.
         * @param header The header text to display for the section.
         * @param items The items within the section.
         * @param selectedItemsInSection A list of indices of the selected items in the section.
         */
        public void addSection(String header, List<String> items, List<Integer> selectedItemsInSection)
        {
            int offset = getItemCount() + 1;
            sections.add(new Section(header, items));

            for (Integer item : selectedItemsInSection)
                selectedItems.add(item + offset);
        }

        /** Gets the raw item at the specified position, which can either be a header or an item.
         * @param position The position.
         * @return The raw item, either a header or actual item.
         */
        private String getItem(int position)
        {
            // Flatten the sections into a single list, adding the headers in with the items.
            List<String> rawItems = new ArrayList<>();

            for (Section section : sections)
            {
                rawItems.add(section.name);

                for (String item : section.items)
                    rawItems.add(item);
            }

            return rawItems.get(position);
        }

        //region Item Selection

        /** Gets the currently selected item in the recycler view, by section.
         * @return A list of selected indices per section.
         */
        public List<List<Integer>> getSelectedItems()
        {
            List<List<Integer>> sectionSelections = new ArrayList<>();
            int position = 0;

            for (Section section : sections)
            {
                position++;

                List<Integer> selections = new ArrayList<>();

                for (String item : section.items)
                {
                    if (selectedItems.contains(position))
                        selections.add(section.items.indexOf(item));

                    position++;
                }

                sectionSelections.add(selections);
            }

            return sectionSelections;
        }

        /** Adds an item to the list of selected item.
         * @param position The position of the item.
         */
        void selectItem(int position)
        {
            if (!selectedItems.contains(position))
                selectedItems.add(position);
        }

        /** Removes an item from the list of selected item.
         * @param position The position of the item.
         */
        void deselectItem(int position)
        {
            if (selectedItems.contains(position))
                selectedItems.remove(Integer.valueOf(position));
        }

        /** Determines if the item at the specified position is selected. */
        private boolean isItemSelected(int position)
        {
            return selectedItems.contains(position);
        }

        //endregion


        //region ViewHolders

        /** Manages the view of a section header item within the {@link RecyclerView} */
        class HeaderViewHolder extends RecyclerView.ViewHolder
        {
            String header;

            final TextView headerTextView;

            public HeaderViewHolder(View view)
            {
                super(view);

                headerTextView = (TextView)itemView.findViewById(R.id.headerTextView);
            }

            /** Binds the view holder to a list item.
             * @param header The header to bind to.
             */
            public void bind(String header)
            {
                this.header = header;

                headerTextView.setText(header);
            }
        }

        /** Manages the view of a list item within the {@link RecyclerView} */
        class ItemViewHolder extends RecyclerView.ViewHolder
        {
            String item;

            final TextView itemTextView;
            final CheckBox itemCheckbox;

            public ItemViewHolder(View view)
            {
                super(view);

                itemTextView = (TextView)itemView.findViewById(R.id.itemTextView);
                itemCheckbox = (CheckBox)itemView.findViewById(R.id.itemCheckbox);

                itemView.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        itemCheckbox.setChecked(!itemCheckbox.isChecked());
                    }
                });

                itemCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
                {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean checked)
                    {
                        if (checked)
                            selectItem(getAdapterPosition());
                        else
                            deselectItem(getAdapterPosition());
                    }
                });
            }

            /** Binds the view holder to a list item.
             * @param item The item to bind to.
             */
            public void bind(String item)
            {
                this.item = item;

                itemTextView.setText(item);
                itemCheckbox.setChecked(isItemSelected(getAdapterPosition()));
            }
        }

        //endregion


        //region Sections

        class Section
        {
            String name;
            List<String> items;

            public Section(String name, List<String> items)
            {
                this.name = name;
                this.items = items;
            }

            public String getName()
            {
                return name;
            }

            public List<String> getItems()
            {
                return items;
            }
        }

        //endregion
    }

    //endregion


    public interface OnLightsSelectedCallback
    {
        public void onLightsSelected(List<LightPoint> selectedLights, List<Group> selectedGroups);
    }
}
