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

package com.iot.extron.smartlightswitch.lightswitch;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import com.iot.extron.smartlightswitch.R;
import com.iot.extron.smartlightswitch.models.SceneGroup;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A {@link DialogFragment} that displays a list of scenes.
 */
public class DFScenePicker extends DialogFragment
{
    //region Fields

    /** The callback used when a scene is selected. */
    OnSceneSelectedCallback onSceneSelectedCallback;

    /** The list of scenes to display. */
    List<SceneGroup> scenes;

    /** The selected scene. */
    int selectedScene;

    //endregion


    //region Constructors

    /** Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon screen orientation changes). */
    public DFScenePicker()
    {
    }

    /** Creates a new dialog fragment to pick a scene.
     * @param scenes The list of scenes to display.
     */
    public static DFScenePicker newInstance(List<SceneGroup> scenes, OnSceneSelectedCallback callback)
    {
        DFScenePicker fragment = new DFScenePicker();
        fragment.onSceneSelectedCallback = callback;
        fragment.scenes = scenes.stream().sorted((s1, s2) -> s1.getName().compareTo(s2.getName())).collect(Collectors.toList());
        return fragment;
    }

    //endregion


    //region DialogFragment Lifecycle Methods

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        List<String> names = scenes.stream().map(new Function<SceneGroup, String>()
        {
            @Override
            public String apply(SceneGroup scene)
            {
                return scene.getName();
            }
        }).collect(Collectors.<String>toList());


        builder
            .setTitle(R.string.select_scene)
            .setIcon(R.drawable.ic_photo_white_24dp)
            .setSingleChoiceItems(new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_single_choice, names), -1, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialogInterface, int index)
                {
                    selectedScene = index;
                }
            })
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int id)
                {
                    if (onSceneSelectedCallback != null)
                    {
                        if (selectedScene > -1)
                            onSceneSelectedCallback.sceneSelected(scenes.get(selectedScene));
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

    /** A callback for scene selected events. */
    public interface OnSceneSelectedCallback
    {
        /** Raised when a scene has been selected from the dialog.
         * @param scene The selected color.
         */
        public void sceneSelected(SceneGroup scene);
    }

    //endregion
}
