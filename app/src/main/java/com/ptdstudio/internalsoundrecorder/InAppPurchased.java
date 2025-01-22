package com.ptdstudio.internalsoundrecorder;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.dimowner.audiorecorder.ARApplication;
import com.ptdstudio.internalsoundrecorder.util.ProVersionManager;

import java.util.Collections;
import java.util.List;

public class InAppPurchased implements PurchasesUpdatedListener {

    public BillingClient mBillingClient;
    private Handler handler = null;

    public static final String ITEM_NO_ADS = "internal_noads";
    public static final String ITEM_ALL_FEATURES = "internal_features";
    public static final String ITEM_PREMIUM = "internal_premium";

    public InAppPurchased(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            if (purchases != null && !purchases.isEmpty())
                for (int i = 0; i < purchases.size(); i++) {
                    if (!purchases.get(i).isAcknowledged()) {
                        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchases.get(i).getPurchaseToken()).build();
                        AcknowledgePurchaseResponseListener listener =
                                billingResult1 -> {

                                };
                        mBillingClient.acknowledgePurchase(params, listener);
                    }
                    //update purchased data here
                    //purchase.getOrderId();
                    //purchase.getSkus().get(0);
                    if (purchases.get(i).getPurchaseState() == Purchase.PurchaseState.PURCHASED)
                        enableProFeatures(purchases.get(i).getProducts().get(0), handler);
                }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            mBillingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                    new PurchasesResponseListener() {
                        @Override
                        public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> purchaseList) {
                            if (purchaseList != null && !purchaseList.isEmpty()) {
                                for (int i = 0; i < purchaseList.size(); i++) {
                                    Purchase purchase = purchaseList.get(i);
                                    if (!purchase.isAcknowledged()) {
                                        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                                                .setPurchaseToken(purchase.getPurchaseToken()).build();
                                        AcknowledgePurchaseResponseListener listener =
                                                billingResult12 -> {
                                                };
                                        mBillingClient.acknowledgePurchase(params, listener);
                                    }
                                    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED)
                                        enableProFeatures(purchase.getProducts().get(0)/*skus[0]*/, handler);
                                }
                                //update purchased data here
                            }
                            //mBillingClient.endConnection();
                        }

                // check purchases list


        });

    }

}

    public void startGetBilling(Activity activity) {
        if (mBillingClient == null)
            mBillingClient = BillingClient.newBuilder(activity.getApplicationContext()).enablePendingPurchases().setListener(this).build();
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingServiceDisconnected() {

            }

            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    mBillingClient.queryPurchasesAsync(
                            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                            new PurchasesResponseListener() {
                                @Override
                                public void onQueryPurchasesResponse(@NonNull BillingResult _billingResult, @NonNull List<Purchase> purchaseList) {
                                    //list is null, no product was purchased
                                    if (purchaseList.isEmpty()) {
                                        //disable all pro features first
                                        ARApplication.Companion.getInstance().getProVersionManager().setAllFeatures(false);
                                        ARApplication.Companion.getInstance().getProVersionManager().setNoAds(false);
                                        return;
                                    }
                                    for (Purchase purchase : purchaseList){
                                        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                            //save result here
                                            enableProFeatures(purchase.getProducts().get(0)/*skus[0]*/, handler);
                                        }
                                        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged()) {
                                            AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                                                    .setPurchaseToken(purchase.getPurchaseToken()).build();
                                            AcknowledgePurchaseResponseListener listener = billingResult1 -> {
                                            };
                                            mBillingClient.acknowledgePurchase(params, listener);
                                        }
                                    }
                                }
                            });

                }
            }
        });
    }

    public void purchased(Activity activity, String productId) {
        if (mBillingClient == null)
            mBillingClient = BillingClient.newBuilder(activity.getApplicationContext()).enablePendingPurchases().setListener(this).build();
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResponse) {
                if (billingResponse.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    QueryProductDetailsParams.Product queryProductDetailsParams = QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(productId)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build();
                    QueryProductDetailsParams productParams = QueryProductDetailsParams.newBuilder().setProductList(Collections.singletonList(queryProductDetailsParams)).build();

                    mBillingClient.queryProductDetailsAsync(productParams, new ProductDetailsResponseListener() {
                        @Override
                        public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull List<ProductDetails> purchases) {
                            if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK){
                                activity.runOnUiThread(() -> {
                                    for (ProductDetails productDetail : purchases) {
                                        String sku = productDetail.getProductId();
                                        //long price = productDetail.getOneTimePurchaseOfferDetails().getPriceAmountMicros();
                                        if (productId.equals(sku)) {
                                            List<BillingFlowParams.ProductDetailsParams> prductDetailParamsList = Collections.singletonList(
                                                    BillingFlowParams.ProductDetailsParams.newBuilder()
                                                            .setProductDetails(productDetail)
                                                            .build()
                                            );
                                            BillingFlowParams flowParams =
                                                    BillingFlowParams.newBuilder()
                                                            .setProductDetailsParamsList(prductDetailParamsList)
                                                            .build();
                                            BillingResult responseCode123 =
                                                    mBillingClient.launchBillingFlow(activity, flowParams);
                                        }
                                    }
                                });
                            }
                        }
                    });
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
            }
        });
    }

    private void enableProFeatures(String sku_bought, Handler handler) {
        ProVersionManager proVersionManager = ARApplication.Companion.getInstance().getProVersionManager();
        Message message = new Message();
        if (sku_bought.equals(ITEM_PREMIUM)) {
            message.what = 1;
            proVersionManager.setAllFeatures(true);
            proVersionManager.setNoAds(true);
            handler.sendMessage(message);
        } else if (sku_bought.equals(ITEM_ALL_FEATURES)) {
            message.what = 1;
            proVersionManager.setAllFeatures(true);
            handler.sendMessage(message);
        } else if (sku_bought.equals(ITEM_NO_ADS)) {
            message.what = 0;
            proVersionManager.setNoAds(true);
        }
    }
}
