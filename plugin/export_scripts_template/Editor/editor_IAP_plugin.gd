@tool
extends EditorPlugin

const PLUGIN_NAME: String = "GodotGooglePlayStore"

var exportPlugin : AndroidExportPlugin

func _enter_tree() -> void:
	add_custom_type(PLUGIN_NAME, "Node", preload("../GooglePlayStore.gd"), preload("shop.svg"))
	exportPlugin = AndroidExportPlugin.new()
	add_export_plugin(exportPlugin)

func _exit_tree() -> void:
	remove_custom_type(PLUGIN_NAME)
	remove_export_plugin(exportPlugin)
	exportPlugin=null

class AndroidExportPlugin extends EditorExportPlugin:
	
	func _supports_platform(platform: EditorExportPlatform) -> bool:
		if platform is EditorExportPlatformAndroid:
			return true
		return false
		
	func _get_android_libraries(platform: EditorExportPlatform, debug: bool) -> PackedStringArray:
		if debug:
			return PackedStringArray(["res://addons/Android/GodotGooglePlayStore/bin/GodotGooglePlayStore-debug.aar"])
		else:
			return PackedStringArray(["res://addons/Android/GodotGooglePlayStore/bin/GodotGooglePlayStore-release.aar"])
		
	func _get_android_dependencies(platform: EditorExportPlatform, debug: bool) -> PackedStringArray:
		return PackedStringArray(["com.android.billingclient:billing-ktx:8.0.0",
								  "com.google.android.play:review-ktx:2.0.2"])
	
	func _get_name() -> String:
		return PLUGIN_NAME
	
