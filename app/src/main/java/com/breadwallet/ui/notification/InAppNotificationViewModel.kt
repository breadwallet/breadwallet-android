/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 6/7/19.
 * Copyright (c) 2019 breadwallet LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.breadwallet.ui.notification

import androidx.lifecycle.ViewModel
import com.breadwallet.model.InAppMessage
import com.breadwallet.repository.MessagesRepository
import com.breadwallet.tools.threads.executor.BRExecutor
import com.breadwallet.tools.util.EventUtils

class InAppNotificationViewModel(val notification: InAppMessage) : ViewModel() {

    /**
     * Getter of a map with the id and the message_id to be used for analytics.
     */
    private val idsAsAttributes: Map<String, String>
        get() = mapOf(EventUtils.EVENT_ATTRIBUTE_NOTIFICATION_ID to notification.id,
                EventUtils.EVENT_ATTRIBUTE_MESSAGE_ID to notification.messageId)

    /**
     * Mark message as read, this is done when the message is dismissed or the action button is
     * clicked.
     */
    fun markAsRead(actionButtonClicked: Boolean) {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute {
            MessagesRepository.markAsRead(notification.messageId)

            if (actionButtonClicked) {
                EventUtils.pushEvent(EventUtils.EVENT_IN_APP_NOTIFICATION_CTA_BUTTON,
                        idsAsAttributes.toMutableMap().apply {
                            put(EventUtils.EVENT_ATTRIBUTE_NOTIFICATION_CTA_URL, notification.actionButtonUrl.orEmpty())
                        })
            } else {
                EventUtils.pushEvent(EventUtils.EVENT_IN_APP_NOTIFICATION_DISMISSED, idsAsAttributes)
            }
        }
    }

    /**
     * Mark the message as shown.
     */
    fun markAsShown() {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute {
            EventUtils.pushEvent(EventUtils.EVENT_IN_APP_NOTIFICATION_APPEARED, idsAsAttributes)
        }
    }
}
