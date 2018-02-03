package com.iot.extron.smartlightswitch.lightswitch;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.Fragment;
import android.view.View;

import com.iot.extron.smartlightswitch.R;
import com.iot.extron.smartlightswitch.colorpicker.ColorPickerView;

/**
 * A {@link DialogFragment} that displays a color picker.
 */
public class DFColorPicker extends DialogFragment
{
    //region Fields

    /** The callback used when a color is selected. */
    OnColorSelectedCallback onColorSelectedCallback;

    /** The color to set as the starting color when the dialog is created. */
    int startingColor;

    //endregion


    //region UI Elements

    ColorPickerView colorPickerView;

    //endregion


    //region Constructors

    /** Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon screen orientation changes). */
    public DFColorPicker()
    {
    }

    /** Creates a new dialog fragment to pick colors.
     * @param startingColor The color to display as selected when the dialog first loads.
     */
    public static DFColorPicker newInstance(int startingColor, OnColorSelectedCallback callback)
    {
        DFColorPicker fragment = new DFColorPicker();
        fragment.onColorSelectedCallback = callback;
        fragment.startingColor = startingColor;
        return fragment;
    }

    //endregion


    //region DialogFragment Lifecycle Methods

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        View customView = getActivity().getLayoutInflater().inflate(R.layout.v_colorpicker, null);

        colorPickerView = customView.findViewById(R.id.colorPickerView);
        colorPickerView.setSelectedColor(startingColor);

        builder
                .setTitle(R.string.select_color)
                .setIcon(R.drawable.ic_color_lens_white_24dp)
                .setView(customView)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int id)
                    {
                        if (onColorSelectedCallback != null)
                        {
                            onColorSelectedCallback.colorSelected(colorPickerView.getSelectedColor());
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

    //endregion


    //region Callback Interfaces

    /** A callback for color selected events. */
    public interface OnColorSelectedCallback
    {
        /** Raised when a color has been selected from the dialog.
         * @param color The selected color.
         */
        public void colorSelected(int color);
    }

    //endregion
}
