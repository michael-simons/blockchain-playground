/*
 * Copyright 2018 michael-simons.eu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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