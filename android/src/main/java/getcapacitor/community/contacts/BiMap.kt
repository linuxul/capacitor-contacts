package getcapacitor.community.contacts

// Helper to enable bidirectional mapping.
public class BiMap<K : Any, V : Any> internal constructor(
    // The following map can be used for retrieving the value belonging to a certain key.
    private val mapForValues: Map<K, V>,
    private val defaultKey: K,
    private val defaultValue: V
) {
    // The following map can be used for retrieving the key belonging to a certain value.
    // This map is essentially the inverted version of `mapForValues`
    private val mapForKeys: Map<V, K> = mapForValues.entries.associate { (key, value) -> value to key }

    internal fun getKey(v: V): K = mapForKeys[v] ?: defaultKey

    internal fun getValue(k: K): V = mapForValues[k] ?: defaultValue
}
