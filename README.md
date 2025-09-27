# Godot Android PlayStore Plugin
This plugin is based on this [implementation](https://github.com/godot-sdk-integrations/godot-google-play-billing), I combines in-app purchases, subscriptions, and in-app store reviews in one plugin. The upgrade to billing version 8.0.0 was made independently, so keep in mind that this version is not thoroughly tested. Because of that, I recommend using the original plugin for in-app purchases and subscriptions if you don't need the review feature. 

## How to setup
* Make sure the addons folder plugin is at `res://addons`.
* Go to Project -> Project settings -> Plugins.
* Enable `GodotGooglePlayStore`.
* Now add `GodotGooglePlayStore` node using the plus add-node button in the scene tab.

<!-- ## How to use
Once the `GodotGooglePlayStore` node has been added to the scene. You can: -->

## How to use
I streamed line the process into 5 signals and two functions.

### functions:
1.- Function `purchases()`, call this function to get all the purchases and subscriptions.
2.- Function `start_store_review()`, call this function to start the in-app review flow.

### Signals:
1. Signal `Purchase_Acknowledged_Successfully`, call `purchases()` function to get all the purchases and subscriptions.
2. Signal `Purchase_Failed`, called when purchase failed.
3. Signal `Purchase_Pending`, called when purchase is pending.
4. Signal `ReviewError`, called when review flow failed.
5. Signal `ReviewDone`, called when review flow is done.

## Building the plugin
To build the plugin from source, you either download Android Studio from [here](https://developer.android.com/studio), then open the project in Android Studio and build it, or you can use the command line. To build from the command line, you need to have the [Android SDK](https://developer.android.com/studio) and JDK 17 installed.

- Open the terminal in the project's root directory, and run this command:
```
./gradlew assemble
```
- If everything goes as expected, static debug and release libraries will be compiled into the `plugin/addons/Android/GodotGooglePlayStore/bin` folder. After that, copy `addons` folder into your Godot project folder then continue with the [steps.](#How-to-setup).

