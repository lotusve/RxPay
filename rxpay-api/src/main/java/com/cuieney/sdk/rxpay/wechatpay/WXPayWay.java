package com.cuieney.sdk.rxpay.wechatpay;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.cuieney.sdk.rxpay.PaymentStatus;
import com.cuieney.sdk.rxpay.RxBus;
import com.tencent.mm.opensdk.modelpay.PayReq;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import org.json.JSONException;
import org.json.JSONObject;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;


/**
 * wechat Method of payment
 */

public class WXPayWay {


    public static Flowable<PaymentStatus> payMoney(final Activity context, final JSONObject json) {

        return Flowable.create(new FlowableOnSubscribe<PaymentStatus>() {
            @Override
            public void subscribe(final FlowableEmitter<PaymentStatus> e) throws Exception {
                final String appId = getAppId(context);
                final IWXAPI api = WXAPIFactory.createWXAPI(context, appId);
                api.registerApp(appId);
                PayReq req = new PayReq();
                req.appId = appId;
                req.partnerId		= json.getString("partnerId");
                req.prepayId		= json.getString("prepayId");
                req.nonceStr		= json.getString("nonceStr");
                req.timeStamp		= json.getString("timeStamp");
                req.packageValue	= json.getString("packageValue");
                req.sign			= json.getString("sign");
                req.extData			= "app data";
                boolean sendReq = api.sendReq(req);
                if (!sendReq) {
                    e.onNext(new PaymentStatus(false));
                    e.onComplete();
                }else{
                    RxBus.getDefault().toFlowable(PaymentStatus.class)
                            .subscribe(new Consumer<PaymentStatus>() {
                                @Override
                                public void accept(PaymentStatus paymentStatus) throws Exception {
                                    e.onNext(paymentStatus);
                                    e.onComplete();
                                }
                            }, new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable throwable) throws Exception {
                                    e.onNext(new PaymentStatus(false));
                                    e.onComplete();
                                }
                            });
                }


            }
        }, BackpressureStrategy.ERROR);


    }

    public static String getAppId(Activity context) {
        ApplicationInfo info = null;
        try {
            info = context.getApplication().getPackageManager()
                    .getApplicationInfo(context.getPackageName(),
                            PackageManager.GET_META_DATA);
            String wx_appid = info.metaData.getString("WX_APPID");
            if (wx_appid == null) {
                throw new NullPointerException("appid not null");
            }
            return wx_appid;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
