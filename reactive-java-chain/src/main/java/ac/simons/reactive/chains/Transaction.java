package ac.simons.reactive.chains;

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
