package kr.Windmill.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Crypto {

	private static final Logger logger = LoggerFactory.getLogger(Crypto.class);
	private static String privateKey_256 = "DEX_PRIVATE_KEY_THIS_TEST_32BYTE";

	public static String crypt(String plainText) {

		try {
			SecretKeySpec secretKey = new SecretKeySpec(privateKey_256.getBytes("UTF-8"), "AES");
			IvParameterSpec IV = new IvParameterSpec(privateKey_256.substring(0, 16).getBytes());

			Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");

			c.init(Cipher.ENCRYPT_MODE, secretKey, IV);

			byte[] encrpytionByte = c.doFinal(plainText.getBytes("UTF-8"));

			return Hex.encodeHexString(encrpytionByte);
		} catch (Exception e) {
			logger.error("[암호화 에러] {}", e.getMessage(), e);
			return plainText;
		}

	}

	public static String deCrypt(String encodeText) {
		try {

			SecretKeySpec secretKey = new SecretKeySpec(privateKey_256.getBytes("UTF-8"), "AES");
			IvParameterSpec IV = new IvParameterSpec(privateKey_256.substring(0, 16).getBytes());

			Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");

			c.init(Cipher.DECRYPT_MODE, secretKey, IV);

			byte[] decodeByte = Hex.decodeHex(encodeText.toCharArray());

			return new String(c.doFinal(decodeByte), "UTF-8");
		} catch (Exception e) {
			logger.error("[복호화 에러] {}", e.getMessage(), e);
			return "";
		}
	}

}
