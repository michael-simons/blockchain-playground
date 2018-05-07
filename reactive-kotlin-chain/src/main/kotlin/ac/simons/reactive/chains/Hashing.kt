package ac.simons.reactive.chains

import java.security.MessageDigest

/**
 * Digests a byte array. MessageDigest is not thread safe, so just get a new one.
 */
fun digest(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)

private val HEX_DIGITS = "0123456789abcdef".toCharArray()

/**
 * Encodes a byte array as hex.
 */
fun encode(bytes: ByteArray): String = bytes.fold("", {str, b -> str + HEX_DIGITS[ (b.toInt() shr 4) and 0xf] + HEX_DIGITS[b.toInt() and 0xf]})