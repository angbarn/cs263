package u1606484.banksim.databases

import u1606484.banksim.SecurityService
import u1606484.banksim.databases.FunctionalHelpers.DatabaseBinding

class ApplicationDatabaseManagerTest extends GroovyTestCase {
    static ApplicationDatabaseManager m = new ApplicationDatabaseManager()

    private static final String FIRST_NAME = "Jeremy"
    private static final String LAST_NAME = "Smith"
    private static final String PHONE_NUMBER = "07000000000"
    private static final String PASSWORD = "12345"
    private static final int PASSWORD_PASSES = 1
    private static final String ADDRESS_LINE_ONE = "31 Cherry Street"
    private static final String ADDRESS_LINE_TWO = ""
    private static final String POSTCODE = "GU76 5PQ"
    private static final String COUNTY = "Cambridgeshire"
    private static final String SESSION_KEY = "abcdefghijklmno"
    private static final long EXPIRY_TIME = 1000

    static {
        println "HELLO"

        def empty = [] as DatabaseBinding[]
        // clear database
        m.exec("DELETE FROM customer", empty, false)
        m.exec("DELETE FROM session", empty, false)
        m.exec("DELETE FROM address", empty, false)
        m.exec("DELETE FROM security", empty, false)

        m.newCustomer(PHONE_NUMBER, FIRST_NAME, LAST_NAME,
                PASSWORD, PASSWORD_PASSES,
                ADDRESS_LINE_ONE, ADDRESS_LINE_TWO, POSTCODE, COUNTY)

        assertFalse(m.getSessionKeyData(1).isPresent())
        m.assignSessionKey(1, SESSION_KEY, System.currentTimeMillis() + EXPIRY_TIME)
    }

    void testPhoneNumber() {
        assertEquals(m.fetchPhoneNumber(1).get(), PHONE_NUMBER)
    }

    void testVerifyPassword() {
        assertFalse(m.verifyPassword(1, PASSWORD + "-"))
        assertTrue(m.verifyPassword(1, PASSWORD))
    }

    void testUpdatePassword() {
        String newPassword = PASSWORD + "###"
        m.updatePassword(1, newPassword, SecurityService.getSalt(), PASSWORD_PASSES)
        assertTrue(m.verifyPassword(1, newPassword))
        m.updatePassword(1, PASSWORD, SecurityService.getSalt(), PASSWORD_PASSES)
        assertTrue(m.verifyPassword(1, PASSWORD))
    }


    void testGetSessionKeyData() {
        SessionKeyPackage p = m.getSessionKeyData(1).get()
        assertEquals(0, p.getOtacStage())
        assertEquals(SESSION_KEY, p.getSessionKey())
    }

    void testSetOtacAuthenticated() {
        m.setOtacAuthenticated(SESSION_KEY, 1)
        assertEquals(m.getSessionKeyData(1).get().getOtacStage(), 1)
        m.setOtacAuthenticated(SESSION_KEY, 0)
        assertEquals(m.getSessionKeyData(1).get().getOtacStage(), 0)
    }

    void testExpiry() {
        m.assignSessionKey(1, SESSION_KEY, System.currentTimeMillis() - 200)
        assertFalse(m.getSessionKeyData(1).isPresent())
        m.assignSessionKey(1, SESSION_KEY, System.currentTimeMillis() + 200)
        assertTrue(m.getSessionKeyData(1).isPresent())
    }

    /*
    void testAssignSessionKey() {
    }
    void testFetchLoginKey() {
    }
    */
}
