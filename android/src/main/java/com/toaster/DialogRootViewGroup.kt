package com.toaster

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.config.ReactFeatureFlags
import com.facebook.react.uimanager.JSPointerDispatcher
import com.facebook.react.uimanager.JSTouchDispatcher
import com.facebook.react.uimanager.RootView
import com.facebook.react.uimanager.UIManagerModule
import com.facebook.react.uimanager.events.EventDispatcher

fun DialogRootViewGroup.eventDispatcher(): EventDispatcher {
  val reactContext = context as ReactContext
  return reactContext.getNativeModule(UIManagerModule::class.java)!!.eventDispatcher
}

class DialogRootViewGroup(context: Context) : ViewGroup(context), RootView {
  private val mJSTouchDispatcher = JSTouchDispatcher(this)
  private var mJSPointerDispatcher: JSPointerDispatcher? = null
  var reactView: View? = null
  private var showDuration: Long = 250
  private var presentDuration: Long = 4000
  private var dismissDuration: Long = 250

  private var dismissAnimation: ViewPropertyAnimator? = null
  private var presentAnimation: ViewPropertyAnimator? = null
  var onDismissed: (() -> Unit)? = null

  init {
    if (ReactFeatureFlags.dispatchPointerEvents) {
      mJSPointerDispatcher = JSPointerDispatcher(this)
    }
  }

  private fun ensureLayoutParams() {
    if (layoutParams != null) return
    layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
  }

  fun setVirtualHeight(h: Float) {
    if (reactView == null) return
    ensureLayoutParams()
    layoutParams!!.height = h.toInt()

    dismissAnimation?.cancel()
    presentAnimation?.cancel()
    if (visibility == GONE) {
      translationY = -h
      visibility = VISIBLE
      presentAnimation = animate()
        .setDuration(showDuration)
        .translationY(0f)
        .withEndAction {
          presentAnimation = null
          dismissAnimation = animate()
            .setStartDelay(showDuration)
            .setDuration(dismissDuration)
            .translationY(-(layoutParams!!.height.toFloat()))
            .withEndAction(::dismiss)
        }
    }
  }

  override fun addView(child: View, index: Int, params: LayoutParams) {
    println("😀 DialogRootViewGroup.addView ${child.id}")
    if (reactView != null) removeView(reactView)
    super.addView(child, -1, params)
    reactView = child
  }

  override fun removeView(view: View?) {
    if (view == reactView) releaseReactView()
    super.removeView(view)
  }

  override fun removeViewAt(index: Int) {
    if (getChildAt(index) === reactView) releaseReactView()
    super.removeViewAt(index)
  }

  override fun onChildStartedNativeGesture(p0: View?, p1: MotionEvent?) {
    mJSTouchDispatcher.onChildStartedNativeGesture(p1, eventDispatcher())
    mJSPointerDispatcher?.onChildStartedNativeGesture(p0, p1, eventDispatcher())
  }

  override fun onChildStartedNativeGesture(p0: MotionEvent?) {
    this.onChildStartedNativeGesture(null, p0)
  }

  override fun onChildEndedNativeGesture(p0: View?, p1: MotionEvent?) {
    mJSTouchDispatcher.onChildEndedNativeGesture(p1, eventDispatcher())
    mJSPointerDispatcher?.onChildEndedNativeGesture()
  }

  override fun handleException(t: Throwable?) {
    (context as ReactContext).handleException(RuntimeException(t))
  }

  override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
    mJSTouchDispatcher.handleTouchEvent(event, eventDispatcher())
    mJSPointerDispatcher?.handleMotionEvent(event, eventDispatcher(), false)
    return super.onInterceptTouchEvent(event)
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    try {
      mJSTouchDispatcher.handleTouchEvent(event, eventDispatcher())
      mJSPointerDispatcher?.handleMotionEvent(event, eventDispatcher(), false)
    } catch (_: Throwable) { }
    super.onTouchEvent(event)
    return true
  }

  override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) = Unit
  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {}

  private fun releaseReactView() {
    reactView = null
  }

  fun dismiss(duration: Long? = null) {
    dismissDuration = duration ?: dismissDuration
    if (dismissAnimation == null && presentAnimation == null) return
    println("😀 DialogRootViewGroup.dismiss $duration")
    dismissAnimation?.cancel()
    onDismissed?.invoke()
    dismissAnimation = null
    presentAnimation = null
    onDismissed = null
  }

  fun presentIn(parent: ViewGroup, params: ReadableMap?) {
    visibility = GONE
    parent.addView(this)
    params?.let {
      showDuration = (it.getDouble("showDuration").toLong())
      presentDuration = (it.getDouble("presentDuration").toLong())
      dismissDuration = (it.getDouble("presentDuration").toLong())
    }
  }
}
