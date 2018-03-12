package u1606484.banksim;

public class LoginSystem {
	private static final int OTAC_PRECISION = 60 * 5;
	private final DatabaseManager databaseManager;

	public LoginSystem() {
		databaseManager = new DatabaseManager();
	}

	private static byte[] getArbitraryBytes(int len) {
		byte[] ret = new byte[len];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = 0;
		}
		return ret;
	}

	// TODO
	private static byte[] getPassword(int userId) {

//		return getArbitraryBytes((int) (Math.random() * 10 + 5));
		return "hello".getBytes();
	}

	private static boolean verifyPassword(int userId, String passwordAttempt) {
		byte[] attemptHash = OtacGenerator.getHash(passwordAttempt);
		byte[] trueHash = OtacGenerator.getHash(getPassword(userId));

		boolean match = true;

		if (trueHash.length == attemptHash.length) {
			for (int i = 0; i < attemptHash.length; i++) {
				if (trueHash[i] != attemptHash[i]) {
					match = false;
					break;
				}
			}
		} else {
			match = false;
		}

		return match;
	}

	private static String generateOtac(int userId) {
		StringBuilder content = new StringBuilder();
		byte[] nonce = getLoginNonce(userId);

		content.append(userId);
		content.append(Base64Controller.toBase64(nonce));

		return OtacGenerator.generateOtac(OTAC_PRECISION, content.toString());
	}

	// TODO
	private static byte[] getLoginNonce(int userId) {
		int t = 20;
		return getArbitraryBytes(t);
	}

	// TODO
	private static void sendOtac(int userId) {
		String email = userId + "@host.domain";
		System.out.println(email + ": " + generateOtac(userId));
	}

	public static void main(String[] arguments) {
		int targetUserId = 1;
		String passwordAttempt = "hello";
		String otacAttempt = generateOtac(targetUserId);

		if (verifyPassword(targetUserId, passwordAttempt)) {
			String otac = generateOtac(targetUserId);
			sendOtac(targetUserId);

			if (otac.equals(otacAttempt)) {
				System.out.println("lottery");
			} else {
				System.out.println("Computer says no");
			}
		} else {
			System.out.println("Computer says no");
		}
	}

}
