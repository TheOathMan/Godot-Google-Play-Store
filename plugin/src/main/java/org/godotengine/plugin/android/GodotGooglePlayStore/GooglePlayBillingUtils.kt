package org.godotengine.plugin.android.GodotGooglePlayStore

/*************************************************************************/
/*  GooglePlayBillingUtils.java                                         */
/*************************************************************************/
/*                       This file is part of:                           */
/*                           GODOT ENGINE                                */
/*                      https://godotengine.org                          */
/*************************************************************************/
/* Copyright (c) 2007-2020 Juan Linietsky, Ariel Manzur.                 */
/* Copyright (c) 2014-2020 Godot Engine contributors (cf. AUTHORS.md).   */
/*                                                                       */
/* Permission is hereby granted, free of charge, to any person obtaining */
/* a copy of this software and associated documentation files (the       */
/* "Software"), to deal in the Software without restriction, including   */
/* without limitation the rights to use, copy, modify, merge, publish,   */
/* distribute, sublicense, and/or sell copies of the Software, and to    */
/* permit persons to whom the Software is furnished to do so, subject to */
/* the following conditions:                                             */
/*                                                                       */
/* The above copyright notice and this permission notice shall be        */
/* included in all copies or substantial portions of the Software.       */
/*                                                                       */
/* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,       */
/* EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF    */
/* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.*/
/* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY  */
/* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,  */
/* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE     */
/* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.                */
/*************************************************************************/

import com.android.billingclient.api.Purchase
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetails.PricingPhase
import com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails
import org.godotengine.godot.Dictionary
import java.util.ArrayList


object GooglePlayBillingUtils {
    fun convertPurchaseToDictionary(purchase: Purchase): Dictionary {
        val dictionary = Dictionary()
        dictionary["original_json"] = purchase.originalJson
        dictionary["order_id"] = purchase.orderId
        dictionary["package_name"] = purchase.packageName
        dictionary["purchase_state"] = purchase.purchaseState
        dictionary["purchase_time"] = purchase.purchaseTime
        dictionary["purchase_token"] = purchase.purchaseToken
        dictionary["quantity"] = purchase.quantity
        dictionary["signature"] = purchase.signature
        // PBL V4 replaced getSku with getSkus to support multi-sku purchases,
        // use the first entry for "sku" and generate an array for "skus"
        val skus: ArrayList<String> = purchase.products as ArrayList<String>
        dictionary["sku"] = if (skus.isNotEmpty()) skus[0] else ""
        val skusArray = skus.toTypedArray<String>()
        dictionary["skus"] = skusArray
        dictionary["is_acknowledged"] = purchase.isAcknowledged
        dictionary["is_auto_renewing"] = purchase.isAutoRenewing
        return dictionary
    }

    fun convertProductDetailsToDictionary(details: ProductDetails): Dictionary {
        val dictionary = Dictionary()
        dictionary["product_id"] = details.productId
        dictionary["title"] = details.title
        dictionary["description"] = details.description
        dictionary["product_type"] = details.productType
        dictionary["name"] = details.name

        // Handle one-time purchase offers
        val oneTimePurchaseOfferDetails = details.oneTimePurchaseOfferDetails
        if (oneTimePurchaseOfferDetails != null) {
            dictionary["price"] = oneTimePurchaseOfferDetails.formattedPrice
            dictionary["price_currency_code"] = oneTimePurchaseOfferDetails.priceCurrencyCode
            dictionary["price_amount_micros"] = oneTimePurchaseOfferDetails.priceAmountMicros
//            dictionary["original_price"] = oneTimePurchaseOfferDetails.originalPrice
//            dictionary["original_price_amount_micros"] = oneTimePurchaseOfferDetails.originalPriceAmountMicros
        }

        // Handle subscription offers
        val subscriptionOfferDetails = details.subscriptionOfferDetails
        if (subscriptionOfferDetails != null && subscriptionOfferDetails.isNotEmpty()) {
            // Use the first offer as the primary one
            val primaryOffer = subscriptionOfferDetails[0]
            val pricingPhases = primaryOffer.pricingPhases.pricingPhaseList

            if (pricingPhases.isNotEmpty()) {
                val firstPricingPhase = pricingPhases[0]
                dictionary["price"] = firstPricingPhase.formattedPrice
                dictionary["price_currency_code"] = firstPricingPhase.priceCurrencyCode
                dictionary["price_amount_micros"] = firstPricingPhase.priceAmountMicros

                // Add subscription period information
                dictionary["billing_period"] = firstPricingPhase.billingPeriod
                dictionary["billing_cycle_count"] = firstPricingPhase.billingCycleCount

                // Look for free trial or introductory pricing
                for (phase in pricingPhases) {
                    if (phase.priceAmountMicros == 0L) {
                        dictionary["free_trial_period"] = phase.billingPeriod
                        break
                    }
                }
            }

            // Add all offer details as an array
            val offerDetailsArray = arrayOfNulls<Any>(subscriptionOfferDetails.size)
            for (i in subscriptionOfferDetails.indices) {
                offerDetailsArray[i] = convertSubscriptionOfferDetailsToDictionary(subscriptionOfferDetails[i])
            }
            dictionary["subscription_offers"] = offerDetailsArray
        }

        return dictionary
    }

    private fun convertSubscriptionOfferDetailsToDictionary(offerDetails: SubscriptionOfferDetails): Dictionary {
        val dictionary = Dictionary()
        dictionary["offer_id"] = offerDetails.basePlanId
        dictionary["offer_token"] = offerDetails.offerToken

        val pricingPhases = offerDetails.pricingPhases.pricingPhaseList
        val pricingPhasesArray = arrayOfNulls<Any>(pricingPhases.size)
        for (i in pricingPhases.indices) {
            pricingPhasesArray[i] = convertPricingPhaseToDictionary(pricingPhases[i])
        }
        dictionary["pricing_phases"] = pricingPhasesArray

        return dictionary
    }

    private fun convertPricingPhaseToDictionary(pricingPhase: PricingPhase): Dictionary {
        val dictionary = Dictionary()
        dictionary["formatted_price"] = pricingPhase.formattedPrice
        dictionary["price_currency_code"] = pricingPhase.priceCurrencyCode
        dictionary["price_amount_micros"] = pricingPhase.priceAmountMicros
        dictionary["billing_period"] = pricingPhase.billingPeriod
        dictionary["billing_cycle_count"] = pricingPhase.billingCycleCount
        dictionary["recurrence_mode"] = pricingPhase.recurrenceMode
        return dictionary
    }

    fun convertPurchaseListToDictionaryObjectArray(purchases: List<Purchase>): Array<Any?> {
        val purchaseDictionaries = arrayOfNulls<Any>(purchases.size)
        for (i in purchases.indices) {
            purchaseDictionaries[i] = convertPurchaseToDictionary(purchases[i])
        }
        return purchaseDictionaries
    }

    fun convertProductDetailsListToDictionaryObjectArray(productDetails: List<ProductDetails>): Array<Any?> {
        val productDetailsDictionaries = arrayOfNulls<Any>(productDetails.size)
        for (i in productDetails.indices) {
            productDetailsDictionaries[i] = convertProductDetailsToDictionary(productDetails[i])
        }
        return productDetailsDictionaries
    }

    // Keep the old method for backward compatibility (you can remove it later)
    @Deprecated("Use convertProductDetailsToDictionary instead")
    fun convertSkuDetailsToDictionary(details: ProductDetails): Dictionary {
        return convertProductDetailsToDictionary(details)
    }

    @Deprecated("Use convertProductDetailsListToDictionaryObjectArray instead")
    fun convertSkuDetailsListToDictionaryObjectArray(productDetails: List<ProductDetails>): Array<Any?> {
        return convertProductDetailsListToDictionaryObjectArray(productDetails)
    }
}