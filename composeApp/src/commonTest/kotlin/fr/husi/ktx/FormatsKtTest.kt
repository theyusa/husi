package fr.husi.ktx

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FormatsKtTest {

    @Test
    fun `b64Decode should decode regular base64 content`() {
        val result = "SGVsbG8=".b64Decode().decodeToString()

        assertEquals("Hello", result)
    }

    @Test
    fun `b64Decode should decode url safe base64 without padding`() {
        val expected = byteArrayOf(251.toByte(), 239.toByte(), 255.toByte(), 250.toByte())
        val result = "--__-g".b64Decode()

        assertContentEquals(expected, result)
    }

    @Test
    fun `b64Decode should fallback to mime decoder for wrapped content`() {
        val result = "SGVs\nbG8=".b64Decode().decodeToString()

        assertEquals("Hello", result)
    }

    @Test
    fun `b64Decode should throw IllegalStateException for invalid base64`() {
        val error = assertFailsWith<IllegalStateException> {
            "世".b64Decode()
        }

        assertEquals(error.message?.startsWith("decode base64: "), true)
    }
}
