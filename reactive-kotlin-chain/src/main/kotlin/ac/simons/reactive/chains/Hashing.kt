package ac.simons.reactive.chains

import java.math.BigInteger
import java.security.MessageDigest

/**
 * Digests a byte array. MessageDigest is not thread safe, so just get a new one.
 */
fun digest(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)

/**
 * Encodes a byte array as hex.
 */
fun encode(bytes: ByteArray): String = String.format("%064x", BigInteger(1, bytes));