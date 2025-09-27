@file:Suppress("UNUSED")

package org.godotengine.plugin.android.GodotGooglePlayStore

import android.util.Log
import androidx.collection.ArraySet
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.SubscriptionUpdateParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.PendingPurchasesParams
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import org.godotengine.godot.Dictionary
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot
import org.godotengine.plugin.android.GodotGooglePlayStore.GooglePlayBillingUtils.convertPurchaseListToDictionaryObjectArray
import org.godotengine.plugin.android.GodotGooglePlayStore.GooglePlayBillingUtils.convertProductDetailsListToDictionaryObjectArray

class GodotGooglePlayStore(godot: Godot): GodotPlugin(godot), PurchasesUpdatedListener,
    BillingClientStateListener {

    private var billingClient: BillingClient? = null
    private val productDetailsCache = HashMap<String, ProductDetails>() // productId → ProductDetails

    private var calledStartConnection = false
    private var obfuscatedAccountId: String? = null
    private var obfuscatedProfileId: String? = null

    init {
        val pendingPurchasesParams = PendingPurchasesParams
            .newBuilder()
            .enableOneTimeProducts() // 👈 required in v8 if you want one-time products
            .build()
        billingClient = BillingClient
            .newBuilder(activity!!)
            .enablePendingPurchases(pendingPurchasesParams)
            .setListener(this)
            .build()
        calledStartConnection = false
        obfuscatedAccountId = ""
        obfuscatedProfileId = ""
    }

    @UsedByGodot
    fun startConnection() {
        calledStartConnection = true
        billingClient!!.startConnection(this)
    }

    fun requestAppReview() {
        val manager: ReviewManager = ReviewManagerFactory.create(activity!!)
        val request: Task<ReviewInfo> = manager.requestReviewFlow()

        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow: Task<Void> = manager.launchReviewFlow(activity!!, reviewInfo)
                flow.addOnCompleteListener {
                    emitSignal("ReviewDone")
                }
            } else {
                emitSignal("ReviewError")
            }
        }
    }

    @UsedByGodot
    fun startStoreReview() {
        requestAppReview()
    }

    fun endConnection() {
        billingClient!!.endConnection()
    }

    @UsedByGodot
    fun isReady(): Boolean {
        return billingClient!!.isReady
    }

    @UsedByGodot
    fun getConnectionState(): Int {
        return billingClient!!.connectionState
    }

    @UsedByGodot
    fun queryPurchases(type: String?) {
        Log.v("godot","queryPurchases")
        val productType = when (type) {
            "inapp" -> BillingClient.ProductType.INAPP
            "subs" -> BillingClient.ProductType.SUBS
            else -> BillingClient.ProductType.INAPP
        }

        val queryPurchasesParams = QueryPurchasesParams.newBuilder()
            .setProductType(productType)
            .build()

        billingClient!!.queryPurchasesAsync(
            queryPurchasesParams
        ) { billingResult, purchaseList ->
            Log.v("godot","queryPurchases1")
            val returnValue = Dictionary()
            Log.v("godot","queryPurchases2")
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.v("godot","queryPurchases3")
                returnValue.put("status", 0) // OK = 0
                returnValue.put(
                    "purchases",
                    convertPurchaseListToDictionaryObjectArray(purchaseList)
                )
            } else {
                returnValue.put("status", 1) // FAILED = 1
                returnValue.put("response_code", billingResult.responseCode)
                returnValue.put("debug_message", billingResult.debugMessage)
            }
            emitSignal("query_purchases_response", returnValue as Any)
        }
    }

    @UsedByGodot
    fun querySkuDetails(list: Array<String?>, type: String?) {
        val productList = listOf(*list)
        val productType = when (type) {
            "inapp" -> BillingClient.ProductType.INAPP
            "subs" -> BillingClient.ProductType.SUBS
            else -> BillingClient.ProductType.INAPP
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                productList.map { productId ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId!!)
                        .setProductType(productType)
                        .build()
                }
            )
            .build()

        billingClient!!.queryProductDetailsAsync(params) { billingResult, queryResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                // Get the product details list from the queryResult
                val productDetailsList = queryResult.productDetailsList
                for (productDetails in productDetailsList) {
                    productDetailsCache[productDetails.productId] = productDetails
                }
                emitSignal(
                    "sku_details_query_completed",
                    convertProductDetailsListToDictionaryObjectArray(productDetailsList) as Any
                )
            } else {
                emitSignal(
                    "sku_details_query_error",
                    billingResult.responseCode,
                    billingResult.debugMessage,
                    list
                )
            }
        }
    }

    @UsedByGodot
    fun acknowledgePurchase(purchaseToken: String?) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken!!)
            .build()
        billingClient!!.acknowledgePurchase(
            acknowledgePurchaseParams
        ) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                emitSignal("purchase_acknowledged", purchaseToken)
            } else {
                emitSignal(
                    "purchase_acknowledgement_error",
                    billingResult.responseCode,
                    billingResult.debugMessage,
                    purchaseToken
                )
            }
        }
    }

    @UsedByGodot
    fun consumePurchase(purchaseToken: String?) {
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchaseToken!!)
            .build()
        billingClient!!.consumeAsync(
            consumeParams
        ) { billingResult, purchaseToken ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                emitSignal("purchase_consumed", purchaseToken)
            } else {
                emitSignal(
                    "purchase_consumption_error",
                    billingResult.responseCode,
                    billingResult.debugMessage,
                    purchaseToken
                )
            }
        }
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            emitSignal("connected")
        } else {
            emitSignal("connect_error", billingResult.responseCode, billingResult.debugMessage)
        }
    }

    override fun onBillingServiceDisconnected() {
        emitSignal("disconnected")
    }

    @UsedByGodot
    fun purchase(sku: String): Dictionary {
        return purchaseInternal(
            "", sku,
            SubscriptionUpdateParams.ReplacementMode.DEFERRED
        )
    }

    @UsedByGodot
    fun updateSubscription(oldToken: String, sku: String, prorationMode: Int): Dictionary {
        return purchaseInternal(oldToken, sku, prorationMode)
    }

    private fun purchaseInternal(oldToken: String, productId: String, prorationMode: Int): Dictionary {
        if (!productDetailsCache.containsKey(productId)) {
            val returnValue = Dictionary()
            returnValue["status"] = 1 // FAILED = 1
            returnValue["response_code"] = null
            returnValue["debug_message"] = "You must query the product details and wait for the result before purchasing!"
            return returnValue
        }

        val productDetails = productDetailsCache[productId]
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails!!)
                .build()
        )

        val purchaseParamsBuilder = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)

        if (!obfuscatedAccountId!!.isEmpty()) {
            purchaseParamsBuilder.setObfuscatedAccountId(obfuscatedAccountId!!)
        }
        if (obfuscatedProfileId!!.isNotEmpty()) {
            purchaseParamsBuilder.setObfuscatedProfileId(obfuscatedProfileId!!)
        }

        if (oldToken.isNotEmpty() && prorationMode != SubscriptionUpdateParams.ReplacementMode.DEFERRED) {
            val updateParams = SubscriptionUpdateParams.newBuilder()
                .setOldPurchaseToken(oldToken)
                .setSubscriptionReplacementMode(prorationMode)
                .build()
            purchaseParamsBuilder.setSubscriptionUpdateParams(updateParams)
        }

        val result = billingClient!!.launchBillingFlow(activity!!, purchaseParamsBuilder.build())
        val returnValue = Dictionary()
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            returnValue["status"] = 0 // OK = 0
        } else {
            returnValue["status"] = 1 // FAILED = 1
            returnValue["response_code"] = result.responseCode
            returnValue["debug_message"] = result.debugMessage
        }
        return returnValue
    }

    @UsedByGodot
    fun setObfuscatedAccountId(accountId: String) {
        obfuscatedAccountId = accountId
    }

    @UsedByGodot
    fun setObfuscatedProfileId(profileId: String) {
        obfuscatedProfileId = profileId
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        list: List<Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && list != null) {
            emitSignal("purchases_updated", convertPurchaseListToDictionaryObjectArray(list) as Any)
        } else {
            Log.v("godot","onPurchasesUpdated")
            emitSignal("purchase_error", billingResult.responseCode, billingResult.debugMessage)
        }
    }

    override fun onMainResume() {
        Log.v("godot","onMainResume")
        if (calledStartConnection) {
            emitSignal("billing_resume")
        }
    }

    override fun getPluginName() = "GodotGooglePlayStore"

    @Deprecated("Deprecated in Java")
    override fun getPluginMethods(): List<String> {
        return mutableListOf(
            "startConnection",
            "endConnection",
            "purchase",
            "updateSubscription",
            "querySkuDetails",
            "isReady",
            "getConnectionState",
            "queryPurchases",
            "acknowledgePurchase",
            "consumePurchase",
            "setObfuscatedAccountId",
            "setObfuscatedProfileId"
        )
    }

    override fun getPluginSignals(): Set<SignalInfo> {
        val signals: MutableSet<SignalInfo> = ArraySet()
        signals.add(SignalInfo("ReviewDone"))
        signals.add(SignalInfo("ReviewError"))
        signals.add(SignalInfo("connected"))
        signals.add(SignalInfo("disconnected"))
        signals.add(SignalInfo("billing_resume"))
        signals.add(
            SignalInfo(
                "connect_error",
                Integer::class.java,
                String::class.java
            )
        )
        signals.add(SignalInfo("purchases_updated", Array<Any>::class.java))
        signals.add(SignalInfo("query_purchases_response", Any::class.java))
        signals.add(
            SignalInfo(
                "purchase_error",
                Integer::class.java,
                String::class.java
            )
        )
        signals.add(
            SignalInfo(
                "sku_details_query_completed",
                Array<Any>::class.java
            )
        )
        signals.add(
            SignalInfo(
                "sku_details_query_error",
                Integer::class.java,
                String::class.java,
                Array<String>::class.java
            )
        )
        signals.add(SignalInfo("price_change_acknowledged", Integer::class.java))
        signals.add(SignalInfo("purchase_acknowledged", String::class.java))
        signals.add(
            SignalInfo(
                "purchase_acknowledgement_error",
                Integer::class.java,
                String::class.java,
                String::class.java
            )
        )
        signals.add(SignalInfo("purchase_consumed", String::class.java))
        signals.add(
            SignalInfo(
                "purchase_consumption_error",
                Integer::class.java,
                String::class.java,
                String::class.java
            )
        )
        return signals
    }
}