package ac.simons.reactive.chains

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.module.SimpleModule

class EventModule() : SimpleModule() {
    init {
        setMixInAnnotation(Event::class.java, EventMixIn::class.java)
    }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "event",
            visible = true
    )
    @JsonSubTypes(value = [
        Type(value = NewBlockEvent::class, name = "new_block"),
        Type(value = NewTransactionEvent::class, name = "new_transaction"),
        Type(value = NewNodeEvent::class, name = "new_node")
    ])
    @JsonIgnoreProperties(ignoreUnknown = true)
    class EventMixIn
}