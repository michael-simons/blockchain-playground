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

import java.util.List;

/**
 * A block. A building block of a block chain, so to speak.
 */
public class Block {
	private final int index;

	private final long timestamp;

	private final long proof;

	private final List<Transaction> transactions;

	private final String previousBlockHash;

	public Block(int index, long timestamp, long proof, List<Transaction> transactions, String previousBlockHash) {
		this.index = index;
		this.timestamp = timestamp;
		this.proof = proof;
		this.transactions = transactions;
		this.previousBlockHash = previousBlockHash;
	}

	public Block newCandidateOf(final long newProof) {
		return new Block(index, timestamp, newProof, transactions, previousBlockHash);
	}

	public int getIndex() {
		return index;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public long getProof() {
		return proof;
	}

	public List<Transaction> getTransactions() {
		return transactions;
	}

	public String getPreviousBlockHash() {
		return previousBlockHash;
	}
}
