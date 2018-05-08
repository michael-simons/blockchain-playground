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
package ac.simons.reactive.chains;

/**
 * An arbitrary, untyped transaction which can be contained in the block of a chain.
 */
public class Transaction {
	private final String id;

	private final long timestamp;

	private final String payload;

	public Transaction(String id, long timestamp, String payload) {
		this.id = id;
		this.timestamp = timestamp;
		this.payload = payload;
	}

	public String getId() {
		return id;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public String getPayload() {
		return payload;
	}
}
