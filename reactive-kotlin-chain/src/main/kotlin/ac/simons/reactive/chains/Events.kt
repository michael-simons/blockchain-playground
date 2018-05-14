package ac.simons.reactive.chains

sealed class Event<D>(
        val id: Int,
        val event: String,
        val data: D
)

class NewBlockEvent(id: Int, data: Block) : Event<Block>(id, "new_block", data)

class NewTransactionEvent(id: Int, data: Transaction) : Event<Transaction>(id, "new_transaction", data)

class NewNodeEvent(id: Int, data: Node) : Event<Node>(id, "new_node", data)

