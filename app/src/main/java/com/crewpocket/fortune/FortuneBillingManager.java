package com.crewpocket.fortune;

import android.app.Activity;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryProductDetailsResult;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.Collections;
import java.util.List;

/**
 * Google Play one-time consumable purchase for one full fortune reading.
 *
 * Product configured in Play Console:
 *   fortune_full_reading_credit
 *
 * The product is consumed only after the AI report has been successfully persisted.
 */
final class FortuneBillingManager implements PurchasesUpdatedListener {
    static final String PRODUCT_FULL_READING =
            "fortune_full_reading_credit";

    interface Listener {
        void onBillingReady(String formattedPrice);
        void onPurchaseReady(String purchaseToken);
        void onPurchasePending();
        void onBillingError(String message);
        void onPurchaseConsumed(String purchaseToken);
    }

    private final Activity activity;
    private final Listener listener;
    private BillingClient billingClient;
    private ProductDetails productDetails;
    private String formattedPrice = "";

    FortuneBillingManager(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    void start() {
        if (billingClient != null) return;

        billingClient = BillingClient.newBuilder(activity)
                .setListener(this)
                .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder()
                                .enableOneTimeProducts()
                                .build())
                .enableAutoServiceReconnection()
                .build();

        billingClient.startConnection(
                new BillingClientStateListener() {
                    @Override public void onBillingSetupFinished(
                            BillingResult result) {
                        if (result.getResponseCode()
                                != BillingClient.BillingResponseCode.OK) {
                            error("Google Play 付款初始化失敗："
                                    + result.getDebugMessage());
                            return;
                        }
                        queryProduct();
                        queryOwnedPurchases();
                    }

                    @Override public void onBillingServiceDisconnected() {
                        // Auto reconnection is enabled in Play Billing 9.
                    }
                });
    }

    void close() {
        BillingClient client = billingClient;
        billingClient = null;
        productDetails = null;
        if (client != null) {
            try { client.endConnection(); } catch (RuntimeException ignored) {}
        }
    }

    String formattedPrice() {
        return formattedPrice;
    }

    boolean canPurchase() {
        return billingClient != null
                && billingClient.isReady()
                && productDetails != null;
    }

    void launchPurchase() {
        BillingClient client = billingClient;
        ProductDetails details = productDetails;
        if (client == null || !client.isReady() || details == null) {
            error("Google Play 商品尚未準備好，請稍後再試");
            return;
        }

        BillingFlowParams.ProductDetailsParams.Builder item =
                BillingFlowParams.ProductDetailsParams
                        .newBuilder()
                        .setProductDetails(details);

        ProductDetails.OneTimePurchaseOfferDetails offer =
                firstOffer(details);
        if (offer != null
                && offer.getOfferToken() != null
                && !offer.getOfferToken().isEmpty()) {
            item.setOfferToken(offer.getOfferToken());
        }

        BillingFlowParams params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                        Collections.singletonList(item.build()))
                .build();

        BillingResult result =
                client.launchBillingFlow(activity, params);
        if (result.getResponseCode()
                != BillingClient.BillingResponseCode.OK) {
            error("無法開啟 Google Play 付款："
                    + result.getDebugMessage());
        }
    }

    void consume(String purchaseToken) {
        BillingClient client = billingClient;
        String token = clean(purchaseToken);
        if (client == null || !client.isReady() || token.isEmpty()) {
            error("報告已保存，但付款憑證尚未完成消耗；下次啟動會再處理");
            return;
        }

        ConsumeParams params = ConsumeParams.newBuilder()
                .setPurchaseToken(token)
                .build();
        client.consumeAsync(params, (result, consumedToken) -> {
            if (result.getResponseCode()
                    == BillingClient.BillingResponseCode.OK) {
                if (listener != null) {
                    listener.onPurchaseConsumed(consumedToken);
                }
            } else {
                error("報告已保存，但 Google Play 消耗失敗："
                        + result.getDebugMessage());
            }
        });
    }

    @Override public void onPurchasesUpdated(
            BillingResult billingResult,
            List<Purchase> purchases) {
        int code = billingResult.getResponseCode();
        if (code == BillingClient.BillingResponseCode.USER_CANCELED) {
            return;
        }
        if (code != BillingClient.BillingResponseCode.OK) {
            error("付款失敗：" + billingResult.getDebugMessage());
            return;
        }
        handlePurchases(purchases);
    }

    private void queryProduct() {
        BillingClient client = billingClient;
        if (client == null) return;

        QueryProductDetailsParams.Product product =
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_FULL_READING)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build();
        QueryProductDetailsParams params =
                QueryProductDetailsParams.newBuilder()
                        .setProductList(
                                Collections.singletonList(product))
                        .build();

        client.queryProductDetailsAsync(
                params,
                (result, detailsResult) ->
                        handleProductDetails(result, detailsResult));
    }

    private void handleProductDetails(
            BillingResult result,
            QueryProductDetailsResult detailsResult) {
        if (result.getResponseCode()
                != BillingClient.BillingResponseCode.OK) {
            error("讀取付費商品失敗：" + result.getDebugMessage());
            return;
        }
        List<ProductDetails> values =
                detailsResult == null
                        ? Collections.emptyList()
                        : detailsResult.getProductDetailsList();
        if (values == null || values.isEmpty()) {
            error("Play Console 尚未提供完整解讀商品");
            return;
        }

        productDetails = values.get(0);
        ProductDetails.OneTimePurchaseOfferDetails offer =
                firstOffer(productDetails);
        formattedPrice =
                offer == null ? "" : clean(offer.getFormattedPrice());
        if (listener != null) {
            listener.onBillingReady(formattedPrice);
        }
    }

    private void queryOwnedPurchases() {
        BillingClient client = billingClient;
        if (client == null) return;
        QueryPurchasesParams params =
                QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build();
        client.queryPurchasesAsync(
                params,
                (result, purchases) -> {
                    if (result.getResponseCode()
                            == BillingClient.BillingResponseCode.OK) {
                        handlePurchases(purchases);
                    }
                });
    }

    private void handlePurchases(List<Purchase> purchases) {
        if (purchases == null) return;
        for (Purchase purchase : purchases) {
            if (purchase == null
                    || purchase.getProducts() == null
                    || !purchase.getProducts().contains(
                            PRODUCT_FULL_READING)) {
                continue;
            }
            if (purchase.getPurchaseState()
                    == Purchase.PurchaseState.PENDING) {
                if (listener != null) listener.onPurchasePending();
                continue;
            }
            if (purchase.getPurchaseState()
                    == Purchase.PurchaseState.PURCHASED) {
                if (listener != null) {
                    listener.onPurchaseReady(
                            purchase.getPurchaseToken());
                }
            }
        }
    }

    private static ProductDetails.OneTimePurchaseOfferDetails firstOffer(
            ProductDetails details) {
        if (details == null) return null;
        ProductDetails.OneTimePurchaseOfferDetails direct =
                details.getOneTimePurchaseOfferDetails();
        if (direct != null) return direct;
        List<ProductDetails.OneTimePurchaseOfferDetails> offers =
                details.getOneTimePurchaseOfferDetailsList();
        return offers == null || offers.isEmpty()
                ? null
                : offers.get(0);
    }

    private void error(String message) {
        if (listener != null) {
            listener.onBillingError(
                    message == null ? "Google Play 付款發生錯誤" : message);
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
