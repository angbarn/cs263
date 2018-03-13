package u1606484.banksim.weblogic

class LoginSystemTest extends GroovyTestCase {
    private static final LoginSystem login = new LoginSystem()

    static void testAttemptBasicLogin() {
        String incorrectPassword = "no"

        String blankPassword = ""

        StringBuilder longPasswordBuilder = new StringBuilder()
        for (int i = 0; i < 1000; i++) {
            longPasswordBuilder.append("a")
        }
        String longPassword = longPasswordBuilder.toString()

        String correctPassword = "jess continues to be a disappointment"

        boolean incorrectLogin = login.attemptBasicLogin(1, incorrectPassword)
        boolean blankLogin = login.attemptBasicLogin(1, blankPassword)
        boolean longLogin = login.attemptBasicLogin(1, longPassword)
        boolean correctLogin = login.attemptBasicLogin(1, correctPassword)

        assertEquals(new Boolean(false), incorrectLogin)
        assertEquals(new Boolean(false), blankLogin)
        assertEquals(new Boolean(false), longLogin)
        assertEquals(new Boolean(true), correctLogin)
    }

    void testAttemptOtacLogin() {
        fail("Not implemented")
    }
}
