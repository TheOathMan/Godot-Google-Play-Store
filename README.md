# Godot Android PlayStore Plugin
This plugin combines in-app purchases, subscriptions, and in-app store reviews in one plugin. The billing version used is 8.0.0.

## How to setup
* Make sure the addons folder plugin is at `res://addons`.
* Go to Project -> Project settings -> Plugins.
* Enable `GodotGooglePlayStore`.
* Now add `GodotGooglePlayStore` node using the plus add-node button in the scene tab.

<!-- ## How to use
Once the `GodotGooglePlayStore` node has been added to the scene. You can: -->

## Building the plugin
To build the plugin from source, you either download Android Studio from [here](https://developer.android.com/studio), then open the project in Android Studio and build it, or you can use the command line. To build from the command line, you need to have the [Android SDK](https://developer.android.com/studio) and JDK 17 installed.

- Open the terminal in the project's root directory, and run this command:
```
./gradlew assemble
```
- If everything goes as expected, static debug and release libraries will be compiled into the `plugin/addons/Android/GodotGooglePlayStore/bin` folder. After that, copy `addons` folder into your Godot project folder then continue with the [steps.](#How-to-setup).

