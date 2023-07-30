#import <React/RCTViewManager.h>

@interface RCT_EXTERN_REMAP_MODULE(ToasterView, ToasterViewManager, RCTViewManager)


RCT_EXPORT_VIEW_PROPERTY(onToastDismiss, RCTDirectEventBlock)

RCT_EXPORT_VIEW_PROPERTY(toasterParams, NSDictionary)
RCT_EXTERN_METHOD(dismiss:(nonnull NSNumber *)node duration:(nonnull NSNumber*)duration)

@end
