package cn.ksuser.auth.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cn.ksuser.auth.android.data.model.UserProfile
import cn.ksuser.auth.android.data.model.UserSettings
import cn.ksuser.auth.android.ui.HomeScreen
import cn.ksuser.auth.android.ui.OverviewCard
import cn.ksuser.auth.android.ui.theme.KsuserAuthAndroidTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class UiSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeScreen_rendersCoreSections() {
        composeRule.setContent {
            KsuserAuthAndroidTheme {
                HomeScreen(
                    user = UserProfile(
                        uuid = "u-1",
                        username = "tester",
                        email = "tester@example.com",
                        settings = UserSettings(
                            mfaEnabled = true,
                            preferredMfaMethod = "totp",
                            preferredSensitiveMethod = "password",
                        ),
                    ),
                    onRefresh = {},
                    onOpenProfile = {},
                    onOpenSecurity = {},
                    onOpenSessions = {},
                )
            }
        }

        composeRule.onNodeWithText("Passkey 联调提示").assertIsDisplayed()
        composeRule.onNodeWithText("刷新账号信息").assertIsDisplayed()
    }

    @Test
    fun overviewCard_actionCallbackInvoked() {
        var clicked = 0
        composeRule.setContent {
            KsuserAuthAndroidTheme {
                OverviewCard(
                    title = "Title",
                    subtitle = "Sub",
                    body = "Body",
                    actionLabel = "执行操作",
                    onAction = { clicked++ },
                )
            }
        }

        composeRule.onNodeWithText("执行操作").performClick()
        assertEquals(1, clicked)
    }
}
