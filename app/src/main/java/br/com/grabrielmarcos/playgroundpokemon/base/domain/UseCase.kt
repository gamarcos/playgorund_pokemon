package br.com.grabrielmarcos.playgroundpokemon.base.domain

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onStart

abstract class UseCase<P, R> {

    @ExperimentalCoroutinesApi
    fun asFlow(
        scope: CoroutineScope = GlobalScope,
        param: P? = null,
        dsl: ResultDispatcher<R>.() -> Unit
    ) {
        val dispatcher = ResultDispatcher<R>()
            .apply(dsl)

        scope.launch {
            withContext(Dispatchers.Main) {
                execute(param)
                    .filter { guard(param) }
                    .onStart { dispatcher.onLoading(Result.Loading) }
                    .catch { error -> dispatcher.onError(Result.Error(error)) }
                    .collect { result -> dispatcher.onSuccess(Result.Success(result)) }
            }
        }
    }

    fun asLiveData(
        scope: CoroutineScope = GlobalScope,
        param: P? = null,
        dsl: ResultDispatcher<R>.() -> Unit
    ): LiveData<R> {
        val dispatcher = ResultDispatcher<R>()
            .apply(dsl)

        return execute(param)
            .filter { guard(param) }
            .onStart { dispatcher.onLoading(Result.Loading) }
            .catch { error -> dispatcher.onError(Result.Error(error)) }
            .asLiveData(scope.coroutineContext)
    }

    protected open suspend fun guard(param: P?): Boolean {
        return true
    }

    @VisibleForTesting
    abstract fun execute(param: P?): Flow<R>
}