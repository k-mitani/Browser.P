package net.i09158knct.android.browserp.views

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import net.i09158knct.android.browserp.Util

class SwipeLinearLayout : LinearLayout, GestureDetector.OnGestureListener {
    constructor(context: Context) : super(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
    }

    val gestureDetector = GestureDetector(context, this)
    var onSwipeListener: OnSwipeListener? = null

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (gestureDetector.onTouchEvent(ev)) return true
        return super.onInterceptTouchEvent(ev)
    }

    override fun onScroll(p0: MotionEvent?, p1: MotionEvent, p2: Float, p3: Float) = false
    override fun onDown(p0: MotionEvent) = false
    override fun onShowPress(p0: MotionEvent) {}
    override fun onSingleTapUp(p0: MotionEvent) = false
    override fun onLongPress(p0: MotionEvent) {}
    override fun onFling(
        event1: MotionEvent?,
        event2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        return onSwipeListener?.onSwipe(this, event1, event2, velocityX, velocityY) ?: false
    }

    interface OnSwipeListener {
        fun onSwipe(view: View, ev1: MotionEvent?, ev2: MotionEvent?, vx: Float, vy: Float): Boolean
    }
}