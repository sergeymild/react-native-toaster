package com.toaster

import com.facebook.react.uimanager.PixelUtil


fun Int.toDP(): Int {
  return PixelUtil.toDIPFromPixel(this.toFloat()).toInt()
}
