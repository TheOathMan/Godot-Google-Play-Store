class_name AndroidInAppPurchases
extends Node

var IAP_ID_Consunable:String="consumable_support1"
var IAP_ID_Onetime:String="buy_fullapp"

signal Purchase_Acknowledged_Successfully
signal Purchase_Failed
signal Purchase_Pending

signal ReviewDone
signal ReviewError

var query_purchases_result
var payment
var success_message :String
var failed_message :String

# Matches Purchase.PurchaseState in the Play Billing Library
enum PurchaseState {
	UNSPECIFIED,
	PURCHASED,
	PENDING,
}

# Matches BillingClient.ConnectionState in the Play Billing Library
enum ConnectionState {
	DISCONNECTED, # not yet connected to billing service or was already closed
	CONNECTING, # currently in process of connecting to billing service
	CONNECTED, # currently connected to billing service
	CLOSED, # already closed and shouldn't be used again
}

enum BillingResponseCode {
	OK = 0,  # The operation was successful.
	USER_CANCELED = 1,  # The user canceled the operation.
	SERVICE_UNAVAILABLE = 2,  # The billing service is unavailable.
	BILLING_UNAVAILABLE = 3,  # The feature is not supported by the Play Store.
	ITEM_UNAVAILABLE = 4,  # The requested product is not available for purchase.
	DEVELOPER_ERROR = 5,  # An error occurred due to incorrect implementation.
	ERROR = 6,  # A general error occurred during the operation.
	ITEM_ALREADY_OWNED = 7,  # The user already owns the item.
	ITEM_NOT_OWNED = 8,  # The user does not own the item.
	SERVICE_DISCONNECTED = -1,  # The billing service is disconnected.
	FEATURE_NOT_SUPPORTED = -2,  # The feature is not supported by the Play Store.
	SERVICE_TIMEOUT = -3,  # The billing service timed out.
	NETWORK_ERROR = -4,  # A network error occurred.
	USER_PRE_APPROVED = -5,  # The user pre-approved the purchase.
}

func _ready() -> void:
	#startConnection()
	pass

func startConnection():
	if Engine.has_singleton("GodotGooglePlayStore"):
		payment = Engine.get_singleton("GodotGooglePlayStore")
		# These are all signals supported by the API
		# You can drop some of these based on your needs
		payment.billing_resume.connect(_on_billing_resume) # No params
		payment.connected.connect(_on_connected) # No params
		payment.disconnected.connect(_on_disconnected) # No params
		payment.connect_error.connect(_on_connect_error) # Response ID (int), Debug message (string)
		payment.price_change_acknowledged.connect(_on_price_acknowledged) # Response ID (int)
		payment.purchases_updated.connect(_on_purchases_updated) # Purchases (Dictionary[])
		payment.purchase_error.connect(_on_purchase_error) # Response ID (int), Debug message (string)
		payment.sku_details_query_completed.connect(_on_product_details_query_completed) # Products (Dictionary[])
		payment.sku_details_query_error.connect(_on_product_details_query_error) # Response ID (int), Debug message (string), Queried SKUs (string[])
		payment.purchase_acknowledged.connect(_on_purchase_acknowledged) # Purchase token (string)
		payment.purchase_acknowledgement_error.connect(_on_purchase_acknowledgement_error) # Response ID (int), Debug message (string), Purchase token (string)
		payment.purchase_consumed.connect(_on_purchase_consumed) # Purchase token (string)
		payment.purchase_consumption_error.connect(_on_purchase_consumption_error) # Response ID (int), Debug message (string), Purchase token (string)
		payment.query_purchases_response.connect(_on_query_purchases_response) # Purchases (Dictionary[])
		#store review
		payment.ReviewDone.connect(review_done) # Purchases (Dictionary[])
		payment.ReviewError.connect(review_error) # Purchases (Dictionary[])
		payment.startConnection()
		print("GodotGooglePlayStore plugin is found.")
	else:
		printerr("Couldn't get GodotGooglePlayStore singleton.")
		print(Engine.get_singleton_list())

var purchase_flow_ended_with_success:bool

func purchase():
	print('purchase request sent')
	purchase_flow_ended_with_success=false
	Purchase_Pending.emit()
	if payment!=null:
		payment.purchase(IAP_ID_Consunable)
		#payment.purchase(IAP_ID_Onetime)
	test_Purchase()

func query_purchases():
	print('query_purchases')
	payment.queryPurchases("inapp") # Or "subs" for subscriptions

#func purchase_consumable():
	#Purchase_Pending.emit()
	#test_Purchase()
	#if payment!=null:
		#payment.purchase(IAP_ID_Consunable)

func _on_connected():
	print("_on_connected")
	payment.querySkuDetails([IAP_ID_Consunable], "inapp") # "subs" for subscriptions
	#payment.querySkuDetails([IAP_ID_Onetime], "inapp")

func _on_product_details_query_completed(product_details):
	print("_on_product_details_query_completed")
	query_purchases_result = product_details
	for available_product in product_details:
		print(available_product)

func _on_product_details_query_error(response_id, error_message, products_queried):
	#start_events_check()
	print("on_product_details_query_error id:", response_id, " message: ",
			error_message, " products: ", products_queried)

func _on_query_purchases_response(query_result):
	if query_result.status == OK:
		for purchase in query_result.purchases:
			_process_purchase(purchase)
			print("_on_query_purchases_response  - purchase.purchase_state:",purchase.purchase_state)
			print(purchase)
	else:
		print("queryPurchases failed, response code: ",
				query_result.response_code,
				" debug message: ", query_result.debug_message)

func _on_billing_resume():
	print('_on_billing_resume')
	start_events_check()
	if payment.getConnectionState() == ConnectionState.CONNECTED:
		query_purchases()

func _on_purchases_updated(purchases):
	for purchase in purchases:
		_process_purchase(purchase)
		print("purchases_updated")

func _on_purchase_error(response_id:BillingResponseCode, error_message):
	failed_message = "Purchase_error id:%d"%response_id
	if response_id == BillingResponseCode.USER_CANCELED:
		failed_message=tr('KEY_CANCLED')
		get_store_response()
	if response_id == BillingResponseCode.BILLING_UNAVAILABLE:
		failed_message="BILLING UNAVAILABLE"
		get_store_response()
	if response_id == BillingResponseCode.SERVICE_TIMEOUT:
		failed_message="SERVICE TIMEOUT"
		get_store_response()
		pass
	print("purchase_error id:", response_id, " message: ", get_billing_response_message(response_id))

func _process_purchase(purchase):
	start_events_check()
	if IAP_ID_Consunable in purchase.skus and purchase.purchase_state == PurchaseState.PURCHASED:
		payment.consumePurchase(purchase.purchase_token)
	if IAP_ID_Onetime in purchase.skus:
		if purchase.is_acknowledged==true:
			purchase_flow_ended_with_success=true
		if  purchase.purchase_state == PurchaseState.PURCHASED:
			payment.acknowledgePurchase(purchase.purchase_token)

#-=---------------------Onetime purchase---------------

func _on_purchase_acknowledged(purchase_token):
	_handle_purchase_token(purchase_token, true)
	print("_on_purchase_acknowledged: ",purchase_token)

func _on_purchase_acknowledgement_error(response_id, error_message, purchase_token):
	_handle_purchase_token(purchase_token, false)
	print("_on_purchase_acknowledgement_error id: ", response_id,
			" message: ", error_message)

#-=---------------------consumption purchase---------------

func _on_purchase_consumed(purchase_token):
	_handle_purchase_token(purchase_token, true)
	print("_on_purchase_consumed")

func _on_purchase_consumption_error(response_id, error_message, purchase_token):
	print("_on_purchase_consumption_error id:", response_id,
			" message: ", error_message)
	_handle_purchase_token(purchase_token, false)


func _on_disconnected():
	failed_message = "disconnected"
	pass

func _on_connect_error(Response_ID:int,Debug_message:String):
	failed_message = "connection error: %d"% Response_ID
	pass


func _on_price_acknowledged(Response_ID:int):
	print("_on_price_acknowledged")
	pass


func test_Purchase():
	if(OS.get_name() == "macOS" || OS.get_name() == "Windows"): #for testing paid stuff on pc
		commons.app_data.AppPaid=commons.app_data.AppPaid+1
		commons.save_game()
		Purchase_Acknowledged_Successfully.emit()

var timer_4_event_check : Timer
func start_events_check(time_wait:float=120.0):
	if timer_4_event_check == null:
		timer_4_event_check=Timer.new()
		add_child(timer_4_event_check)
		timer_4_event_check.one_shot=true
		timer_4_event_check.timeout.connect(func(): 
			failed_message = "Timeout" 
			get_store_response())
	timer_4_event_check.set_wait_time(time_wait)
	timer_4_event_check.start()
	print("start_events_check..")


# Find the sku associated with the purchase token and award the
# product if successful
func _handle_purchase_token(purchase_token, purchase_successful):
	if purchase_successful == true:
		print("purchase acknowledged successfully")
		purchase_flow_ended_with_success=true
		success_message = tr('KEY_PURCHASED')
	else:
		failed_message = tr('KEY_PPURCHASE_FAILED')
	get_store_response()


func get_store_response():
	if purchase_flow_ended_with_success:
		Purchase_Acknowledged_Successfully.emit()
	else:
		Purchase_Failed.emit()
	if timer_4_event_check!=null:
		timer_4_event_check.stop()

func get_billing_response_message(response_code: int) -> String:
	match response_code:
		BillingResponseCode.OK:
			return "The operation was successful."
		BillingResponseCode.USER_CANCELED:
			return "The user canceled the operation."
		BillingResponseCode.SERVICE_UNAVAILABLE:
			return "The billing service is unavailable."
		BillingResponseCode.BILLING_UNAVAILABLE:
			return "The feature is not supported by the Play Store."
		BillingResponseCode.ITEM_UNAVAILABLE:
			return "The requested product is not available for purchase."
		BillingResponseCode.DEVELOPER_ERROR:
			return "An error occurred due to incorrect implementation."
		BillingResponseCode.ERROR:
			return "A general error occurred during the operation."
		BillingResponseCode.ITEM_ALREADY_OWNED:
			return "The user already owns the item."
		BillingResponseCode.ITEM_NOT_OWNED:
			return "The user does not own the item."
		BillingResponseCode.SERVICE_DISCONNECTED:
			return "The billing service is disconnected."
		BillingResponseCode.FEATURE_NOT_SUPPORTED:
			return "The feature is not supported by the Play Store."
		BillingResponseCode.SERVICE_TIMEOUT:
			return "The billing service timed out."
		BillingResponseCode.NETWORK_ERROR:
			return "A network error occurred."
		BillingResponseCode.USER_PRE_APPROVED:
			return "The user pre-approved the purchase."
		_:
			return "Unknown billing response code."

# Store Review Stuff..
func review_done():
	ReviewDone.emit()
	pass
func review_error():
	ReviewError.emit()
	pass
func start_store_review():
	payment.startStoreReview()
