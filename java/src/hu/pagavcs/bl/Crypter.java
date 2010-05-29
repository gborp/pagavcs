package hu.pagavcs.bl;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class Crypter {

	private static final String ENCODING_MODE_1 = ">>>1";
	private static final String P1              = "xcg42AdfgSDG35aljadJA";
	private static final String P2              = "JASD7sgm";
	private static final String P3              = "346sgbnlja7sgm";

	private static final byte[] SALT            = { (byte) 0xae, (byte) 0x34, (byte) 0x10, (byte) 0x18, (byte) 0xde, (byte) 0x73, (byte) 0x10, (byte) 0x12, };

	private static char[]       password;

	private static char[] getPassword() {
		if (password == null) {
			password = (P1 + "peek" + P2 + "poke" + P3).toCharArray();
		}
		return password;
	}

	public static String encrypt(String property) throws GeneralSecurityException {
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
		SecretKey key = keyFactory.generateSecret(new PBEKeySpec(getPassword()));
		Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
		pbeCipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(SALT, 20));
		return ENCODING_MODE_1 + base64Encode(pbeCipher.doFinal(property.getBytes()));
	}

	private static String base64Encode(byte[] bytes) {
		return new BASE64Encoder().encode(bytes);
	}

	public static String decrypt(String property) throws GeneralSecurityException, IOException {
		if (!property.startsWith(ENCODING_MODE_1)) {
			return property;
		}
		property = property.substring(ENCODING_MODE_1.length());
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
		SecretKey key = keyFactory.generateSecret(new PBEKeySpec(getPassword()));
		Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
		pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(SALT, 20));
		return new String(pbeCipher.doFinal(base64Decode(property)));
	}

	private static byte[] base64Decode(String property) throws IOException {
		return new BASE64Decoder().decodeBuffer(property);
	}

}
