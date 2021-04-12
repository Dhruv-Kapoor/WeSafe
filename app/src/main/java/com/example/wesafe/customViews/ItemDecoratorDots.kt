package com.example.wesafe.customViews

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ItemDecoratorDots(
    private val radius: Int,
    private val padding: Int,
    private val indicatorHeight: Int,
    @ColorInt val colorInactive: Int,
    @ColorInt val colorActive: Int
) : RecyclerView.ItemDecoration() {
    private val inactivePaint = Paint()
    private val activePaint = Paint()
    private val interpolator = AccelerateDecelerateInterpolator()

    init {
        val strokeWidth = Resources.getSystem().displayMetrics.density * 1

        with(inactivePaint) {
            strokeCap = Paint.Cap.ROUND
            this.strokeWidth = strokeWidth
            style = Paint.Style.STROKE
            isAntiAlias = true
            color = colorInactive
        }
        with(activePaint) {
            strokeCap = Paint.Cap.ROUND
            this.strokeWidth = strokeWidth
            style = Paint.Style.FILL
            isAntiAlias = true
            color = colorActive
        }
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(c, parent, state)
        val adapter = parent.adapter ?: return
        val itemCount = adapter.itemCount

        val totalLength = radius * 2 * itemCount
        val paddingBetweenItems = 0.coerceAtLeast(itemCount - 1) * padding
        val indicatorTotalWidth = totalLength + paddingBetweenItems
        val indicatorStartX = (parent.width - indicatorTotalWidth) / 2f

        val indicatorPosY = parent.height - indicatorHeight / 2f - 30

        drawInactiveDots(c, indicatorStartX, indicatorPosY, itemCount)

        var activePosition = if (parent.layoutManager is GridLayoutManager) {
            (parent.layoutManager as GridLayoutManager).findFirstVisibleItemPosition()
        } else if (parent.layoutManager is LinearLayoutManager) {
            (parent.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        } else {
            null
        } ?: return

        val completeActivePosition = if (parent.layoutManager is GridLayoutManager) {
            (parent.layoutManager as GridLayoutManager).findFirstCompletelyVisibleItemPosition()
        } else if (parent.layoutManager is LinearLayoutManager) {
            (parent.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
        } else {
            null
        } ?: return


        if (completeActivePosition != RecyclerView.NO_POSITION) {
            activePosition = completeActivePosition
        }
        if (activePosition == RecyclerView.NO_POSITION) {
            return
        }

        val activeView = parent.layoutManager?.findViewByPosition(activePosition) ?: return
        val left = activeView.left
        val width = activeView.width

        val progress = interpolator.getInterpolation(left * -1 / width.toFloat())
        drawActiveDot(c, indicatorStartX, indicatorPosY, activePosition, progress)

    }

    private fun drawActiveDot(
        canvas: Canvas,
        indicatorStartX: Float,
        indicatorPosY: Float,
        activePosition: Int
    ) {
        val itemWidth = radius * 2 + padding
        val highlightStart = indicatorStartX + radius + itemWidth * activePosition
        canvas.drawCircle(highlightStart, indicatorPosY, radius.toFloat() + 5, activePaint)
    }

    private fun drawActiveDot(
        canvas: Canvas,
        indicatorStartX: Float,
        indicatorPosY: Float,
        activePosition: Int,
        progress: Float
    ) {
        val itemWidth = radius * 2 + padding
        val highlightStart = indicatorStartX + radius + itemWidth * activePosition
        val partialLength = 4 * radius * progress
        if (progress == 0f) {
            canvas.drawCircle(highlightStart, indicatorPosY, radius.toFloat() + 5, activePaint)
        } else {
            canvas.drawCircle(
                highlightStart + partialLength,
                indicatorPosY,
                radius.toFloat() + 5,
                activePaint
            )
        }
    }

    private fun drawInactiveDots(
        canvas: Canvas,
        indicatorStartX: Float,
        indicatorPosY: Float,
        itemCount: Int
    ) {
        val itemWidth = radius * 2 + padding
        var start = indicatorStartX + radius
        for (i in 0 until itemCount) {
            canvas.drawCircle(start, indicatorPosY, radius.toFloat(), inactivePaint)
            start += itemWidth
        }
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
//        outRect.bottom = indicatorHeight
    }
}