package org.stepik.android.adaptive.core.events

import javax.inject.Inject

class ClientImpl<T>
@Inject
constructor(private val listenerContainer: ListenerContainer<T>) : Client<T> {
    override fun subscribe(listener: T) {
        listenerContainer.add(listener)
    }

    override fun unsubscribe(listener: T) {
        listenerContainer.remove(listener)
    }
}
