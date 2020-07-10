package com.breadwallet.uiview

import com.agoda.kakao.common.builders.ViewBuilder
import com.agoda.kakao.common.views.KBaseView
import com.agoda.kakao.text.KButton
import com.breadwallet.R

class KeyboardView(
    parentId: Int,
    function: ViewBuilder.() -> Unit
) : KBaseView<KeyboardView>(function) {

    val deleteButton = KButton {
        withId(R.id.delete)
    }

    fun input(value: String) {
        value.forEach { char ->
            when (char) {
                '0' -> num0.click()
                '1' -> num1.click()
                '2' -> num2.click()
                '3' -> num3.click()
                '4' -> num4.click()
                '5' -> num5.click()
                '6' -> num6.click()
                '7' -> num7.click()
                '8' -> num8.click()
                '9' -> num9.click()
                '.' -> decimal.click()
            }
        }
    }

    val num1 = KButton {
        withId(R.id.num1)
    }

    val num2 = KButton {
        withId(R.id.num2)
    }

    val num3 = KButton {
        withId(R.id.num3)
    }

    val num4 = KButton {
        withId(R.id.num4)
    }

    val num5 = KButton {
        withId(R.id.num5)
    }

    val num6 = KButton {
        withId(R.id.num6)
    }

    val num7 = KButton {
        withId(R.id.num7)
    }

    val num8 = KButton {
        withId(R.id.num8)
    }

    val num9 = KButton {
        withId(R.id.num9)
    }

    val num0 = KButton {
        isDescendantOfA {
            withId(parentId)
        }
        withId(R.id.num0)
    }

    val decimal = KButton {
        withId(R.id.decimal)
    }
}
