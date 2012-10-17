import static org.junit.Assert.assertEquals;
import hu.pagavcs.client.bl.Base64Decoder;

import java.util.Arrays;

import org.junit.Test;

public class Crypter {

	@Test
	public void test() {

		int numRuns = 5;
		int numBytes = 10;
		java.util.Random rnd = new java.util.Random();
		for (int i = 0; i < numRuns; i++) {
			for (int j = 0; j < numBytes; j++) {
				byte[] arr = new byte[j];
				for (int k = 0; k < j; k++)
					arr[k] = (byte) rnd.nextInt();

				String s = Base64Decoder.encodeBase64(arr);
				byte[] b = Base64Decoder.decodeBase64(s);
				if (!Arrays.equals(arr, b)) {
					assertEquals(arr, b);
				}

			}
		}
	}
}
