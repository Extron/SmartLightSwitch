Hue SmartSwitch
===
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

A smart lightswitch Android Things app designed for [Philips Hues smart lights](https://www2.meethue.com/en-us).

Overview
---
This app was designed with [Android Things](https://developer.android.com/things/index.html) in mind, but generally works on any Android OS.  The intent of the app is to provide a simple interface for controlling Philips Hue lights from a wall-mountable touchscreen device.  The app allows you to control the state of any number of Hue lights from a single screen.

Setup
---
1. Install the [Android Things OS](https://developer.android.com/things/get-started/kits.html) on a desired kit, and connect the device to your computer
2. Download [Android Studio](https://developer.android.com/studio/index.html) and open this project in it
3. Connect your device to the [adb tool](https://developer.android.com/studio/command-line/adb.html) with the command `adb connect <ip-address>`
4. Build the app and select the device as the deployment target in Android Studio

The app should then be installed on the device, and will automatically launch whenever the device is turned on.

Usage
---
The first time the app is run, it will need to connect to a Philips Hue bridge.  This app can find and connect to any Hue bridge that is on the same local network as the device the app is running on.  To start the search, press **Find Bridges** to start the search.

Once the search is complete, a list of bridges that can be connected to will be shown.  Select one and the app will attempt to connect.  If this is the first time the app is connecting to the bridge, it will need to be authenticated by pressing the **Link** button on the Hue bridge.  The app will prompt you when it is time to press the **Link** button.

Once the app is successfully connected to a Hue bridge, it will go to the _Lightswitch_ screen.  To select a list of lights to control, press the button at the bottom of the screen.  This will bring up a list of lights and light groups that are connected to the bridge.

Select any number of lights and light groups.  The controls onscreen will affect all lights selected.  Pressing the **On** and **Off** buttons will turn all selected lights on and off, respectively.

When the selected lights are on, the brightness can be controlled using the brightness slider.  The round button on the right will display a list of scenes that can be set on any of the selected lights.  If any selected lights support color, a second round button will be displayed on top of the first one, and will bring up the hue wheel to select a color to set the selected lights to.

Any of the selected lights can be controlled individually by pressing the bar at the bottom of the screen and dragging up to bring up the bottom sheet.  Each individual light can be turned on and off, and the brightness and color changed, should the light support it.

Sliding from left to right on the left edge of the screen opens the app's navigation drawer.  From here, you can manually disconnect from the Hue bridge, and view the app's settings.  The navigation drawer also displays the IP address of the device the app is running on, as well as the name and IP address of the Hue bridge that it is currently connected to.

License
---
Hue SmartSwitch is provided under the [GNU GPLv3 License](https://www.gnu.org/licenses/gpl-3.0.en.html). See [LICENSE](LICENSE.txt) for more details.