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

	/**
	 * 암호화된 문자열인지 검증합니다.
	 * 
	 * @param str 검증할 문자열
	 * @return 유효한 암호화된 문자열이면 true, 그렇지 않으면 false
	 */
	public static boolean isValidEncryptedString(String str) {
		if (str == null || str.length() < 32) {
			return false;
		}
		// 16진수 문자열인지 확인
		if (!str.matches("^[0-9a-fA-F]+$")) {
			return false;
		}
		// Hex 인코딩된 문자열의 길이는 32의 배수여야 함 (16바이트 = 32 Hex 문자)
		// AES 블록 크기 16바이트를 Hex로 인코딩하면 32자
		if (str.length() % 32 != 0) {
			return false;
		}
		return true;
	}

	/**
	 * 패스워드를 복호화합니다. 평문인 경우 그대로 반환합니다.
	 * 
	 * @param encryptedPassword 암호화된 패스워드 또는 평문 패스워드
	 * @return 복호화된 패스워드 또는 평문 패스워드
	 */
	public static String decryptPassword(String encryptedPassword) {
		if (encryptedPassword == null || encryptedPassword.trim().isEmpty()) {
			return encryptedPassword;
		}
		
		// 암호화된 문자열 검증: 16진수 문자열이고 길이가 32의 배수이며 최소 32자 이상
		if (!isValidEncryptedString(encryptedPassword)) {
			logger.debug("평문으로 간주 (암호화 형식 검증 실패): 길이={}", encryptedPassword.length());
			return encryptedPassword;
		}
		
		try {
			String decrypted = deCrypt(encryptedPassword);
			// 복호화 실패 시 빈 문자열이 반환되므로, 원본이 평문인 것으로 간주
			if (decrypted == null || decrypted.isEmpty()) {
				logger.debug("복호화 결과가 비어있음 (평문으로 간주)");
				return encryptedPassword;
			}
			return decrypted;
		} catch (Exception e) {
			// deCrypt 내부에서 예외가 발생하더라도 안전하게 처리
			logger.debug("패스워드 복호화 실패 (평문으로 간주): {}", e.getMessage());
			return encryptedPassword;
		}
	}

}
