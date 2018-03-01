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

package com.iot.extron.smartlightswitch.models;

import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnectionType;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeResponseCallback;
import com.philips.lighting.hue.sdk.wrapper.domain.Bridge;
import com.philips.lighting.hue.sdk.wrapper.domain.HueError;
import com.philips.lighting.hue.sdk.wrapper.domain.ReturnCode;
import com.philips.lighting.hue.sdk.wrapper.domain.clip.ClipResponse;
import com.philips.lighting.hue.sdk.wrapper.domain.resource.Scene;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Encapsulates a group of scenes that are all named the same for a set of lights. */
public class SceneGroup
{
    //region Fields

    /** The list of scenes. */
    List<Scene> scenes;

    //endregion


    //region Constructors

    /** Creates a new scene group.
     * @param scenes The list of scenes contained in the group.  They should all have the same name.
     */
    public SceneGroup(List<Scene> scenes)
    {
        this.scenes = scenes;
    }

    //endregion


    //region Getters

    /** Gets the name of the scene group.  Since all scenes in a group have the same name, simply returns the name of the first scene.
     * @return The name of the scene group.
     */
    public String getName()
    {
        if (scenes.size() > 0)
            return scenes.get(0).getName();
        else
            return "";
    }

    //endregion


    //region Scene Recall

    /** Recalls all scenes within the group.
     * @param connectionType The type of the connection to the bridge.
     * @param callback A callback which is invoked when all scenes have responded to the recall.
     */
    public void recallScenes(BridgeConnectionType connectionType, final SceneGroupRecallCallback callback)
    {
        final AtomicInteger latch = new AtomicInteger(scenes.size());
        final SceneGroupRecallResult[] results = new SceneGroupRecallResult[scenes.size()];

        for (int i = 0; i < scenes.size(); i++)
        {
            final Scene scene = scenes.get(i);
            final int index = i;

            scene.recall(connectionType, new BridgeResponseCallback()
            {
                @Override
                public void handleCallback(Bridge bridge, ReturnCode returnCode, List<ClipResponse> responses, List<HueError> errors)
                {
                    results[index] = new SceneGroupRecallResult(scene, returnCode, errors);

                    int current = latch.decrementAndGet();

                    if (current == 0)
                        callback.onRecallCompleted(Arrays.asList(results));
                }
            });
        }
    }

    /** A callback interface for scene recall events. */
    public interface SceneGroupRecallCallback
    {
        /** Raised when all scenes have responded from a recall.
         * @param results A list of results for each scene that was recalled.
         */
        public void onRecallCompleted(List<SceneGroupRecallResult> results);
    }

    /** The result of recalling a single scene within the group. */
    public class SceneGroupRecallResult
    {
        Scene scene;
        ReturnCode returnCode;
        List<HueError> errors;

        private SceneGroupRecallResult(Scene scene, ReturnCode returnCode, List<HueError> errors)
        {
            this.scene = scene;
            this.returnCode = returnCode;
            this.errors = errors;
        }

        public Scene getScene()
        {
            return scene;
        }

        public ReturnCode getReturnCode()
        {
            return returnCode;
        }

        public List<HueError> getErrors()
        {
            return errors;
        }
    }

    //endregion
}
