/*
 * Copyright 2022-2026 sephy.top
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package top.sephy.infra.utils;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;

import top.sephy.infra.exception.SystemException;

public abstract class SignatureUtils {

    public static final String MD5_WITH_RSA = "MD5withRSA";

    public static final String SHA1_WITH_RSA = "SHA1withRSA";

    public static final String SHA224_WITH_RSA = "SHA224withRSA";

    public static final String SHA256_WITH_RSA = "SHA256withRSA";

    public static final String SHA384_WITH_RSA = "SHA384withRSA";

    public static final String SHA512_WITH_RSA = "SHA512withRSA";

    public static final byte[] sign(byte[] data, PrivateKey privateKey, String algorithm) throws SystemException {
        try {
            Signature signature = Signature.getInstance(algorithm);
            signature.initSign(privateKey);
            signature.update(data);
            return signature.sign();
        } catch (Exception ex) {
            throw new SystemException("签名失败", ex);
        }
    }

    public static final boolean verify(byte[] data, byte[] signatureData, PublicKey publicKey, String algorithm) {
        try {
            Signature signature = Signature.getInstance(algorithm);
            signature.initVerify(publicKey);
            signature.update(data);
            return signature.verify(signatureData);
        } catch (Exception ex) {
            throw new SystemException("验签失败", ex);
        }
    }

    /**
     * 计算 HMAC-SHA256 签名
     *
     * @param data 待签名的数据
     * @param secretKey 密钥
     * @return 十六进制编码的签名字符串
     * @throws SystemException 签名失败时抛出
     */
    public static final String hmacSha256(String data, String secretKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(hash);
        } catch (Exception ex) {
            throw new SystemException("HMAC-SHA256签名失败", ex);
        }
    }

    /**
     * 验证 HMAC-SHA256 签名
     *
     * @param data 原始数据
     * @param signature 待验证的签名（十六进制编码）
     * @param secretKey 密钥
     * @return true 如果签名验证通过，false 否则
     */
    public static final boolean hmacSha256Verify(String data, String signature, String secretKey) {
        try {
            String calculatedSignature = hmacSha256(data, secretKey);
            return calculatedSignature.equalsIgnoreCase(signature);
        } catch (Exception ex) {
            return false;
        }
    }
}
