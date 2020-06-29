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
package com.breadwallet.repository

import android.content.Context
import android.util.Log
import com.breadwallet.model.InAppMessage
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.util.EventUtils
import com.platform.network.InAppMessagesClient

/**
 * Repository for in app messages. Provides methods for fetching new messages and to notify that the
 * message was read.
 */
object MessagesRepository {
    private val TAG = MessagesRepository::class.java.simpleName

    /**
     * Fetch latest in app notification.
     */
    fun getInAppNotification(context: Context): InAppMessage? {
        Log.d(TAG, "getInAppNotification: Looking for new in app notifications")
        val readMessages = BRSharedPrefs.getReadInAppNotificationIds()
        // Filter any notification that we already shown
        val inAppMessages = InAppMessagesClient.fetchMessages(context, InAppMessage.Type.IN_APP_NOTIFICATION)
                .filterNot{ readMessages.contains(it.messageId) }

        if (inAppMessages.isEmpty()) {
            Log.d(TAG, "getInAppNotification: There are no new notifications")
            return null
        }
        // We are not suppose to get more than one in app notification from the backend at the same
        // time but in case it happens we pick the first and will show the others next time we check
        // for notifications.
        val inAppMessage = inAppMessages[0]
        Log.d(TAG, "getInAppNotification: ${inAppMessage.title}")
        EventUtils.pushEvent(EventUtils.EVENT_IN_APP_NOTIFICATION_RECEIVED,
                mapOf(EventUtils.EVENT_ATTRIBUTE_NOTIFICATION_ID to inAppMessage.id))
        return inAppMessage
    }

    /**
     * Mark the given message as read.
     */
    fun markAsRead(messageId: String) {
        BRSharedPrefs.putReadInAppNotificationId(messageId)
    }
}
