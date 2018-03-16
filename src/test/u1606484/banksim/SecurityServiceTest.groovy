package u1606484.banksim

class SecurityServiceTest extends GroovyTestCase {
    private static
    final SecurityService securityService = new SecurityService()

    void testGetSalt() {
        for (int i = 0; i < 100; i++) {
            int ran = (int) (Math.random() * 500)
            byte[] salt = securityService.getSalt(ran)
            assertEquals(salt.length, ran)
        }
    }

    void testGetHash() {
        byte[] testHash = securityService.getHash("pre-calculated hash".getBytes())
        byte[] resultHash = [0x40, 0xef, 0x1e, 0x5a,
                             0xc0, 0x92, 0x0b, 0x35,
                             0xf8, 0xee, 0x71, 0x9f,
                             0xd1, 0xef, 0xc3, 0x82,
                             0x82, 0x37, 0x4a, 0x6c,
                             0xfe, 0xfb, 0x78, 0x59,
                             0xf1, 0xf8, 0xf9, 0xb2,
                             0xf1, 0x77, 0x14, 0x97] as byte[]

        assertEquals(testHash.length, resultHash.length)

        for (int i = 0; i < testHash.length; i++) {
            assertEquals(testHash[i], resultHash[i])
        }
    }

    void testGetPasswordHash() {
        String plainPassword = "pre-calculated password"
        byte[] testSalt = [0x36, 0xd8, 0x40, 0x08,
                           0x71, 0x90, 0x0e, 0x95,
                           0x0d, 0x4e] as byte[]

        // 0x7072652d63616c63756c617465642070617373776f7264 36d8400871900e950d4e
        byte[] resultHash = [0x5a, 0x18, 0x90, 0x22,
                             0x05, 0xdd, 0xe7, 0x45,
                             0x86, 0x26, 0x31, 0x13,
                             0x6d, 0xf9, 0x98, 0x46,
                             0xb5, 0x10, 0xf3, 0xe1,
                             0x40, 0xd5, 0xf0, 0x38,
                             0x48, 0x81, 0x23, 0x2a,
                             0x47, 0x21, 0xca, 0x6e] as byte[]

        byte[] calculatedHash = securityService.getPasswordHash(plainPassword, testSalt, 1)

        println Arrays.toString(resultHash)
        println Arrays.toString(calculatedHash)

        assertEquals(calculatedHash.length, resultHash.length)

        for (int i = 0; i < calculatedHash.length; i++) {
            assertEquals(resultHash[i], calculatedHash[i])
        }
    }

    void testVerifyPassword() {
        String plainPassword = "pre-calculated password"
        byte[] testSalt = [0x36, 0xd8, 0x40, 0x08,
                           0x71, 0x90, 0x0e, 0x95,
                           0x0d, 0x4e] as byte[]

        // 0x7072652d63616c63756c617465642070617373776f7264 36d8400871900e950d4e
        byte[] resultHash = [0x5a, 0x18, 0x90, 0x22,
                             0x05, 0xdd, 0xe7, 0x45,
                             0x86, 0x26, 0x31, 0x13,
                             0x6d, 0xf9, 0x98, 0x46,
                             0xb5, 0x10, 0xf3, 0xe1,
                             0x40, 0xd5, 0xf0, 0x38,
                             0x48, 0x81, 0x23, 0x2a,
                             0x47, 0x21, 0xca, 0x6e] as byte[]

        assertTrue(securityService.verifyPassword(plainPassword, testSalt, resultHash, 1))
    }
}
