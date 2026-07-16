package com.campus.trade.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "alipay")
@Data
public class AlipayProperties {
    private String appId;
    private String gatewayUrl;
    /** 商户私钥（RSA2） */
    private String privateKey;
    /** 支付宝公钥 */
    private String alipayPublicKey;
    /** 同步回调地址（前端页面） */
    private String returnUrl;
    /** 异步通知地址（必须为外网可访问 URL） */
    private String notifyUrl;
}
