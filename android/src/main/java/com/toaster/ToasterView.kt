package com.toaster

import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewStructure
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.facebook.react.bridge.*
import com.facebook.react.uimanager.events.RCTEventEmitter


internal fun ToasterView.onToastDismiss() {
  (context as ReactContext).getJSModule(RCTEventEmitter::class.java)
    .receiveEvent(id, "onToastDismiss", Arguments.createMap())
}

class ToasterView(context: Context) : ViewGroup(context), LifecycleEventListener {
  var mHostView = DialogRootViewGroup(context)

  var params: ReadableMap? = null

  private fun getCurrentActivity(): AppCompatActivity {
    return (context as ReactContext).currentActivity as AppCompatActivity
  }

  fun showOrUpdate() {
    println("🥲 ToasterView.showOrUpdate")
    UiThreadUtil.assertOnUiThread()
    val contentView = getCurrentActivity().findViewById<View>(android.R.id.content) as ViewGroup
    mHostView.presentIn(contentView, params)
    mHostView.onDismissed = {
      val parent = mHostView.parent as? ViewGroup
      parent?.removeView(mHostView)
      onToastDismiss()
    }
  }

  @RequiresApi(Build.VERSION_CODES.M)
  override fun dispatchProvideStructure(structure: ViewStructure?) {
    mHostView.dispatchProvideStructure(structure)
  }

  override fun addView(child: View, index: Int) {
    println("🥲 ToasterView.addView parentId: $id id: ${child.id}")
    UiThreadUtil.assertOnUiThread()
    mHostView.addView(child, index)
    ModalHostShadowNode.pendingUpdateHeight[id]?.let {
      println("🥲 ToasterView.addView pending: $it")
      mHostView.setVirtualHeight(it)
      ModalHostShadowNode.pendingUpdateHeight.remove(id)
    }
  }

  override fun getChildCount(): Int = mHostView.childCount

  override fun getChildAt(index: Int): View? = mHostView.getChildAt(index)

  override fun removeView(child: View) {
    println("🥲 ToasterView.removeView id: ${child.id}")
    UiThreadUtil.assertOnUiThread()
    dismiss()
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    println("🥲 ToasterView.onDetachedFromWindow: $id")
    ModalHostShadowNode.attachedViews.remove(id)
    ModalHostShadowNode.pendingUpdateHeight.remove(id)
  }

  override fun removeViewAt(index: Int) {
    println("🥲 ToasterView.removeViewAt: $index id: ${mHostView.getChildAt(index).id}")
    UiThreadUtil.assertOnUiThread()
    dismiss()
  }

  private fun onDropInstance() {
    println("🥲 ToasterView.onDropInstance")
    (context as ReactContext).removeLifecycleEventListener(this)
    dismiss()
  }

  fun dismiss(duration: Long? = null) {
    println("🥲 ToasterView.dismiss")
    UiThreadUtil.assertOnUiThread()
    mHostView.dismiss(duration)
  }
  override fun addChildrenForAccessibility(outChildren: ArrayList<View?>?) {}

  override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent?) = false
  override fun onHostResume() { showOrUpdate() }
  override fun onHostPause() {}

  override fun onHostDestroy() { onDropInstance() }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {}
}
