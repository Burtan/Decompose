package com.arkivanov.sample.shared.counters.counter

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.overlay.ChildOverlay
import com.arkivanov.decompose.router.overlay.OverlayNavigation
import com.arkivanov.decompose.router.overlay.activate
import com.arkivanov.decompose.router.overlay.childOverlay
import com.arkivanov.decompose.router.overlay.dismiss
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.decompose.value.reduce
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.LifecycleOwner
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.arkivanov.essenty.statekeeper.consume
import com.arkivanov.sample.shared.counters.counter.CounterComponent.Model
import com.arkivanov.sample.shared.dialog.DefaultDialogComponent
import com.arkivanov.sample.shared.dialog.DialogComponent
import com.badoo.reaktive.disposable.scope.DisposableScope
import com.badoo.reaktive.observable.observableInterval
import com.badoo.reaktive.scheduler.Scheduler
import com.badoo.reaktive.scheduler.mainScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

internal class DefaultCounterComponent(
    componentContext: ComponentContext,
    private val title: String,
    private val isBackEnabled: Boolean,
    private val onNext: () -> Unit,
    private val onPrev: () -> Unit,
    tickScheduler: Scheduler = mainScheduler,
) : CounterComponent, ComponentContext by componentContext {

    private val scope = coroutineScope(Dispatchers.Default)
    private val handler =
        instanceKeeper.getOrCreate(KEY_STATE) {
            Handler(
                initialState = stateKeeper.consume(KEY_STATE) ?: State(),
                tickScheduler = tickScheduler,
            )
        }

    override val model: Value<Model> = handler.state.map { it.toModel() }

    private val dialogNavigation = OverlayNavigation<DialogConfig>()

    private val _dialogOverlay =
        childOverlay<DialogConfig, DialogComponent>(
            source = dialogNavigation,
            persistent = false,
            handleBackButton = true,
            childFactory = { config, _ ->
                DefaultDialogComponent(
                    title = "Counter",
                    message = "Value: ${formatCount(config.count)}",
                    onDismissed = dialogNavigation::dismiss,
                )
            }
        )

    override val dialogOverlay: Value<ChildOverlay<*, DialogComponent>> = _dialogOverlay

    override fun onInfoClicked() {
        dialogNavigation.activate(DialogConfig(count = handler.state.value.count))
    }

    init {
        scope.launch {
            while (true) {
                println("I wont get cancelled")
                delay(10)
            }
        }
        lifecycle.subscribe(object : Lifecycle.Callbacks {
            override fun onCreate() {
                println("onCreate")
            }
            override fun onDestroy() {
                println("onDestroy")
            }
            override fun onPause() {
                println("onPause")
            }
            override fun onResume() {
                println("onResume")
            }
            override fun onStart() {
                println("onStart")
            }
            override fun onStop() {
                println("onStop")
            }
        })
        stateKeeper.register(KEY_STATE) { handler.state.value }
    }

    private fun State.toModel(): Model =
        Model(
            title = title,
            text = formatCount(count),
            isBackEnabled = isBackEnabled,
        )

    private fun formatCount(count: Int): String =
        count.toString().padStart(length = 3, padChar = '0')

    override fun onNextClicked() {
        onNext()
    }

    override fun onPrevClicked() {
        onPrev()
    }

    private companion object {
        private const val KEY_STATE = "STATE"
    }

    @Parcelize
    private data class State(
        val count: Int = 0,
    ) : Parcelable

    @Parcelize
    private data class DialogConfig(
        val count: Int,
    ) : Parcelable

    private class Handler(
        initialState: State,
        tickScheduler: Scheduler,
    ) : InstanceKeeper.Instance, DisposableScope by DisposableScope() {
        val state: MutableValue<State> = MutableValue(initialState)

        init {
            observableInterval(periodMillis = 250L, scheduler = tickScheduler).subscribeScoped {
                state.reduce { it.copy(count = it.count + 1) }
            }
        }

        override fun onDestroy() {
            dispose()
        }
    }
}

fun LifecycleOwner.coroutineScope(context: CoroutineContext): CoroutineScope {
    val scope = CoroutineScope(context)
    lifecycle.doOnDestroy(scope::cancel)

    return scope
}
