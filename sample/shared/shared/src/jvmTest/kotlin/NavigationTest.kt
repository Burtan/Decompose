import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.sample.shared.dynamicfeatures.dynamicfeature.TestFeatureInstaller
import com.arkivanov.sample.shared.root.DefaultRootComponent
import com.arkivanov.sample.shared.root.DefaultRootComponent.DeepLink
import com.arkivanov.sample.shared.root.RootComponent
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class NavigationTest {

    @Test
    fun testNavigation() = runBlocking {
        val lifecycleRegistry = LifecycleRegistry()
        val rootComp = DefaultRootComponent(
            componentContext = DefaultComponentContext(lifecycleRegistry),
            featureInstaller = TestFeatureInstaller(),
            deepLink = DeepLink.None,
        )

        rootComp.onCountersTabClicked()

        val counterChild = rootComp.childStack.value.active.instance
        assertTrue(counterChild is RootComponent.Child.CountersChild)

        rootComp.onCustomNavigationTabClicked()
        assertTrue(rootComp.childStack.value.items.size == 1)
        assertTrue(rootComp.childStack.value.backStack.isEmpty())

        // CountersComponent should not be active anymore!
        delay(5000)

        val customChild = rootComp.childStack.value.active.instance
        assertTrue(customChild is RootComponent.Child.CustomNavigationChild)
    }

}
