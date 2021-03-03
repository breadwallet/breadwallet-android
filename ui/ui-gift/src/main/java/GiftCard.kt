/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 12/8/20.
 * Copyright (c) 2020 breadwallet LLC
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
package com.breadwallet.ui.uigift

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.invoke
import kotlin.math.absoluteValue

private const val SHARE_IMAGE_WIDTH = 778
private const val SHARE_IMAGE_HEIGHT = 1215

internal object GiftCard {

    suspend fun create(
        context: Context,
        qrCode: Bitmap,
        recipientName: String,
        giftAmount: String,
        fiatValue: String,
        fiatPricePerUnit: String
    ): Bitmap = Default {
        Bitmap.createBitmap(
            SHARE_IMAGE_WIDTH,
            SHARE_IMAGE_HEIGHT,
            Bitmap.Config.ARGB_8888
        ).applyCanvas {
            drawCard(this, qrCode, context, recipientName, giftAmount, fiatValue, fiatPricePerUnit)
        }
    }

    private fun drawCard(
        canvas: Canvas,
        qrCode: Bitmap,
        context: Context,
        recipientName: String,
        giftAmount: String,
        fiatValue: String,
        fiatPricePerUnit: String,
    ) {
        canvas.drawColor(Color.parseColor("#141233"))
        val font = ResourcesCompat.getFont(context, R.font.mobile_font_book)
        val boldFont = ResourcesCompat.getFont(context, R.font.mobile_font_bold)
        val halfWidth = canvas.width / 2f
        val cardMargin = 18f

        // QRCODE
        var nextY = drawNext(0f, qrCode.height.toFloat(), 64f) { y, height ->
            canvas.drawBitmap(qrCode, halfWidth - (height / 2f), y, Paint())
        }

        // Recipient Name
        nextY = drawNextText(recipientName, nextY, 52f, 24f, {
            textAlign = Paint.Align.CENTER
            color = Color.WHITE
            typeface = boldFont
        }) { y, paint ->
            canvas.drawText(recipientName, halfWidth, y, paint)
        }

        // Tag line
        val tagLine = context.getString(R.string.ShareGift_tagLine)
        nextY = drawNextText(tagLine, nextY, 48f, 12f, {
            textAlign = Paint.Align.CENTER
            color = Color.WHITE
            typeface = boldFont
        }) { y, paint ->
            canvas.drawText(tagLine, halfWidth, y, paint)
        }

        // Divider
        nextY = drawNext(nextY, 4f, cardMargin) { y, height ->
            canvas.drawLine(cardMargin, y, canvas.width - cardMargin, y + height, paint {
                color = context.getColor(R.color.light_gray)
            })
        }

        // Logo (do not update nextY)
        drawNext(nextY, 0f, cardMargin) { y, _ ->
            val logo = ContextCompat.getDrawable(context, R.drawable.brd_logo_gradient)!!
            canvas.drawBitmap(logo.toBitmap(240, 80), cardMargin, y, paint())
        }

        // Approx label
        val totalLabel = context.getString(R.string.ShareGift_approximateTotal)
        nextY = drawNextText(totalLabel, nextY, 28f, cardMargin, {
            textAlign = Paint.Align.RIGHT
            color = Color.WHITE
            typeface = font
        }) { y, paint ->
            canvas.drawText(totalLabel, canvas.width - cardMargin, y, paint)
        }

        // Approx val label
        nextY = drawNextText(fiatValue, nextY, 52f, cardMargin, {
            textAlign = Paint.Align.RIGHT
            color = Color.WHITE
            typeface = boldFont
        }) { y, paint ->
            canvas.drawText(fiatValue, canvas.width - cardMargin, y, paint)
        }

        // Bitcoin card
        nextY = drawNext(nextY, 160f, cardMargin) { y, height ->
            val cardWidth = canvas.width - (cardMargin * 2).toInt()
            val cardBitmap = ContextCompat.getDrawable(context, R.drawable.crypto_card_shape)!!.run {
                setTint(Color.parseColor("#F29500"))
                toBitmap(cardWidth, height.toInt())
            }
            canvas.drawBitmap(cardBitmap, cardMargin, y, paint())

            val cardHalfY = y + (height / 2)
            val btcBitmap = ContextCompat.getDrawable(context, R.drawable.ic_btc_transparent)!!
                .toBitmap(90, 90)
            val btcLogoY = cardHalfY - (btcBitmap.height / 2)
            canvas.drawBitmap(btcBitmap, cardMargin * 2f, btcLogoY, paint())

            // Wallet Name
            val walletNameSize = 36f
            val walletName = context.getString(R.string.ShareGift_walletName)
            val leftTextY = drawNextText(walletName, cardHalfY - walletNameSize, walletNameSize, 6f, {
                textAlign = Paint.Align.LEFT
                color = Color.WHITE
                typeface = boldFont
            }) { labelY, paint ->
                val x = (cardMargin * 3) + btcBitmap.width
                canvas.drawText(walletName, x, labelY, paint)
            }

            // BTC Price
            val btcPriceSize = 32f
            drawNextText(fiatPricePerUnit, leftTextY, btcPriceSize, 6f, {
                textAlign = Paint.Align.LEFT
                color = Color.WHITE
                typeface = boldFont
                alpha = 90
            }) { labelY, paint ->
                val x = (cardMargin * 3) + btcBitmap.width
                canvas.drawText(fiatPricePerUnit, x, labelY, paint)
            }

            // Fiat Amount
            val fiatAmountSize = 36f
            val rightTextY = drawNextText(fiatValue, cardHalfY - fiatAmountSize, fiatAmountSize, 6f, {
                textAlign = Paint.Align.RIGHT
                color = Color.WHITE
                typeface = boldFont
            }) { labelY, paint ->
                val x = canvas.width - (cardMargin * 2)
                canvas.drawText(fiatValue, x, labelY, paint)
            }

            // BTC Amount
            val btcAmountSize = 32f
            drawNextText(giftAmount, rightTextY, btcAmountSize, 6f, {
                textAlign = Paint.Align.RIGHT
                color = Color.WHITE
                typeface = boldFont
                alpha = 90
            }) { labelY, paint ->
                val x = canvas.width - (cardMargin * 2)
                canvas.drawText(giftAmount, x, labelY, paint)
            }
        }

        // Footer 1
        nextY += cardMargin
        val msg = context.getString(R.string.ShareGift_footerMessage1)
        nextY = msg.split("\n").fold(nextY) { accY, s ->
            drawNextText(s, accY, 22f, 6f, {
                textAlign = Paint.Align.CENTER
                color = Color.GRAY
                typeface = font
            }) { y, paint ->
                canvas.drawText(s, halfWidth, y, paint)
            }
        }

        // Footer 2
        val msg2 = context.getString(R.string.ShareGift_footerMessage2)
        nextY += cardMargin
        msg2.split("\n").fold(nextY) { accY, s ->
            drawNextText(s, accY, 22f, 6f, {
                textAlign = Paint.Align.CENTER
                color = Color.WHITE
                typeface = font
            }) { y, paint ->
                canvas.drawText(s, halfWidth, y, paint)
            }
        }
    }

    private fun paint(
        block: Paint.() -> Unit = {}
    ) = Paint().apply {
        isAntiAlias = true
        block()
    }

    private inline fun drawNext(
        yOffset: Float,
        height: Float,
        margin: Float,
        crossinline block: (yPos: Float, height: Float) -> Unit
    ): Float {
        val nextYOffset = yOffset + margin
        block(nextYOffset, height)
        return nextYOffset + height + margin
    }

    private inline fun drawNextText(
        msg: String,
        yOffset: Float,
        height: Float,
        margin: Float,
        configurePaint: Paint.() -> Unit,
        crossinline block: (yPos: Float, paint: Paint) -> Unit
    ): Float {
        val bounds = Rect()
        val paint = paint().apply(configurePaint).apply {
            textSize = height
            getTextBounds(msg, 0, msg.length, bounds)
        }
        val baselineY = bounds.top.absoluteValue
        block(yOffset + baselineY + margin, paint)
        return yOffset + height + margin
    }
}
