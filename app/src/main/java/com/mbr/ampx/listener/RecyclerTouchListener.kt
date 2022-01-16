package com.mbr.ampx.listener

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import com.mbr.ampx.dialog.IRecyclerClickListener

class RecyclerTouchListener(context: Context, recyclerView: RecyclerView, private val clickListener: IRecyclerClickListener) : RecyclerView.OnItemTouchListener {

    private val gestureDetector: GestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            val child = recyclerView.findChildViewUnder(e.x, e.y)
            child?.let {
                val position = recyclerView.getChildAdapterPosition(it)
                if (position >= 0) {
                    clickListener.onRecyclerLongClick(position)
                }
            }
        }

    })

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        val child = rv.findChildViewUnder(e.x, e.y)
        if (child != null && gestureDetector.onTouchEvent(e)) {
            val position = rv.getChildAdapterPosition(child)
            if (position >= 0) {
                clickListener.onRecyclerItemClick(position)
            }
        }
        return false
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {

    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {

    }

}