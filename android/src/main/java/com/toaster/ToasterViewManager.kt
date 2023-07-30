package com.toaster


import com.facebook.react.uimanager.ThemedReactContext

import android.content.res.Resources
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.common.MapBuilder
import com.facebook.react.uimanager.*
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.yoga.YogaPositionType

internal class ModalHostShadowNode : LayoutShadowNode() {
  companion object {
    val attachedViews = mutableMapOf<Int, ToasterView>()
    val pendingUpdateHeight = mutableMapOf<Int, Float>()
  }
  /**
   * We need to set the styleWidth and styleHeight of the one child (represented by the <View></View>
   * within the <RCTModalHostView></RCTModalHostView> in Modal.js. This needs to fill the entire window.
   */
  override fun addChildAt(child: ReactShadowNodeImpl, i: Int) {
    super.addChildAt(child, i)
    val display = Resources.getSystem().displayMetrics
    child.setStyleWidth(display.widthPixels.toFloat())
    child.setPositionType(YogaPositionType.ABSOLUTE)
  }

  private fun savePendingHeight() {
    val newHeight = getChildAt(0).layoutHeight
    println("😀 dispatchUpdates id: $reactTag savePendingHeight: ${newHeight.toInt().toDP()}")
    pendingUpdateHeight[reactTag] = newHeight
  }

  override fun dispatchUpdates(absoluteX: Float, absoluteY: Float, uiViewOperationQueue: UIViewOperationQueue?, nativeViewHierarchyOptimizer: NativeViewHierarchyOptimizer?): Boolean {
    val didChange = super.dispatchUpdates(absoluteX, absoluteY, uiViewOperationQueue, nativeViewHierarchyOptimizer)
    val newHeight = getChildAt(0).layoutHeight
    attachedViews[reactTag]?.mHostView?.let {
      if (it.reactView == null) {
        savePendingHeight()
      } else {
        println("😀 dispatchUpdates id: $reactTag newHeight: ${newHeight.toInt().toDP()}")
        it.setVirtualHeight(newHeight)
        pendingUpdateHeight.remove(reactTag)
      }
    }
    if (attachedViews[reactTag] == null) savePendingHeight()
    return didChange
  }
}

class ToasterViewManager : ViewGroupManager<ToasterView>() {
  override fun getName() = "ToasterView"

  override fun createViewInstance(reactContext: ThemedReactContext): ToasterView {
    return ToasterView(reactContext)
  }

  override fun createViewInstance(reactTag: Int, reactContext: ThemedReactContext, initialProps: ReactStylesDiffMap?, stateWrapper: StateWrapper?): ToasterView {
    val view = super.createViewInstance(reactTag, reactContext, initialProps, stateWrapper)
    println("🥲 createViewInstance id: $reactTag")
    ModalHostShadowNode.attachedViews[view.id] = view
    return view
  }

  @ReactProp(name = "toasterParams")
  fun toasterParams(view: ToasterView, params: ReadableMap) {
    println("🥲 toasterParams $params")
    view.params = params
  }

  override fun getExportedCustomDirectEventTypeConstants(): Map<String, Any>? {
    return MapBuilder.builder<String, Any>()
      .put("onToastDismiss", MapBuilder.of("registrationName", "onToastDismiss"))
      .build()
  }

  override fun getShadowNodeClass(): Class<LayoutShadowNode> {
    return ModalHostShadowNode::class.java as Class<LayoutShadowNode>
  }

  override fun createShadowNodeInstance(): LayoutShadowNode {
    println("🥲 createShadowNodeInstance")
    return ModalHostShadowNode()
  }

  override fun createShadowNodeInstance(context: ReactApplicationContext): LayoutShadowNode {
    println("🥲 createShadowNodeInstance")
    return ModalHostShadowNode()
  }

  override fun onAfterUpdateTransaction(view: ToasterView) {
    super.onAfterUpdateTransaction(view)
    println("🥲 onAfterUpdateTransaction")
    view.showOrUpdate()
  }
}
