package ac.simons.reactive.chains;

import java.util.List;

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
