package com.iot.extron.smartlightswitch;

import com.philips.lighting.hue.sdk.wrapper.domain.Bridge;
import com.philips.lighting.hue.sdk.wrapper.domain.HueError;

import java.util.List;

/** An interface that wraps all Hue bridge events into a single callback.
 *  The bridge's connection lifecycle is Connected -> Pushlink -> Authenticated -> Initialized -> Disconnected
 */
public abstract class BridgeEventCallback
{
    //region Connection

    /** Raised when the bridge is connected.
     * @param bridge The bridge.
     */
    public void bridgeConnected(Bridge bridge) { };

    /** Raised when the bridge connection is lost.
     * @param bridge The bridge.
     */
    public void bridgeConnectionLost(Bridge bridge) { };

    /** Raised when the bridge connection is restored.
     * @param bridge The bridge.
     */
    public void bridgeConnectionRestored(Bridge bridge) { };

    /** Raised when the bridge could not connect.
     * @param bridge The bridge.
     */
    public void bridgeCouldNotConnect(Bridge bridge) { };

    /** Raised when the bridge requests that the link button be pushed by the user.
     * @param bridge The bridge.
     */
    public void bridgePushlinkRequested(Bridge bridge) { };

    /** Raised when the bridge has authenticated the connection.
     * @param bridge The bridge.
     */
    public void bridgeAuthenticated(Bridge bridge) { };

    /** Raised when the bridge has been fully initialized, and is ready to be used.
     * @param bridge The bridge.
     */
    public void bridgeInitialized(Bridge bridge) { };

    /** Raised when the bridge is disconnected.
     * @param bridge The bridge.
     * @param manualDisconnect Indicates whether the disconnect was initiated by the user.
     * @param errors A list of errors that caused the disconnect.  Will be empty in the case of a manual disconnect.
     */
    public void bridgeDisconnected(Bridge bridge, boolean manualDisconnect, List<HueError> errors) { };

    //endregion


    //region State Update Events

    /** Raised when the bridge has updated its bridge configuration.
     * @param bridge The bridge.
     */
    public void updatedBridgeConfig(Bridge bridge) { }

    /** Raised when the bridge has updated lights and groups.
     * @param bridge The bridge.
     */
    public void updatedLightsAndGroups(Bridge bridge) { }

    /** Raised when the bridge has updated scenes.
     * @param bridge The bridge.
     */
    public void updatedScenes(Bridge bridge) { }

    /** Raised when the bridge has updated sensors and switches.
     * @param bridge The bridge.
     */
    public void updatedSensorsAndSwitches(Bridge bridge) { }

    /** Raised when the bridge has updated rules.
     * @param bridge The bridge.
     */
    public void updatedRules(Bridge bridge) { }

    /** Raised when the bridge has updated schedules and timers.
     * @param bridge The bridge.
     */
    public void updatedSchedulesAndTimers(Bridge bridge) { }

    //endregion
}