package app.thunder.api

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

object ConcurrentRunner {

    fun <T> runAll(tasks: List<() -> T>): List<Result<T>> {
        val executor = Executors.newFixedThreadPool(tasks.size)
        val ready = CountDownLatch(tasks.size)
        val start = CountDownLatch(1)
        try {
            val futures = tasks.map { task ->
                executor.submit<Result<T>> {
                    ready.countDown()
                    start.await()
                    runCatching(task)
                }
            }
            ready.await()
            start.countDown()
            return futures.map { it.get() }
        } finally {
            executor.shutdown()
        }
    }

}
