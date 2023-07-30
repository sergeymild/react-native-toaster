//
//  ToasterViewManager.swift
//
//  Created by Sergei Golishnikov on 14/02/2022.
//  Copyright © 2022 Facebook. All rights reserved.
//

import Foundation
import UIKit
import React


private class ModalHostShadowView: RCTShadowView {
    static var attachedViews: [Int: HostToasterView] = [:]
    override func insertReactSubview(_ subview: RCTShadowView!, at atIndex: Int) {
        super.insertReactSubview(subview, at: atIndex)
        if subview != nil {
            (subview as RCTShadowView).width = YGValue.init(value: Float(RCTScreenSize().width), unit: .point)
            subview.position = .absolute
        }
    }

    override func layoutSubviews(with layoutContext: RCTLayoutContext) {
        super.layoutSubviews(with: layoutContext)
        let tag = self.reactTag.intValue
        let size = reactSubviews()[0].contentFrame.size
        let view = ModalHostShadowView.attachedViews[tag]

        DispatchQueue.main.async {
            view?.containerView.frame.size = size
        }
        debugPrint("😀 layout(with \(tag) \(size)")
    }
}

class ContainerView: UIView {
    var presentDuration: TimeInterval = 0.25
    var showDuration: TimeInterval = 4
    var dismissDuration: TimeInterval = 0.25
    var task: DispatchWorkItem?
    var onDismissCallback: (() -> Void)?
    
    func dismiss(duration: TimeInterval) {
        self.dismissDuration = duration
        debugPrint("😀 TopView.dismiss \(self.dismissDuration)")
        if task == nil {
            if onDismissCallback != nil { self.onDismissCallback?() }
            return
        }
        task?.perform()
    }
    
    func presentIn(
        parent: UIViewController,
        onDismiss: @escaping () -> Void
    ) {
        self.onDismissCallback = onDismiss
        transform = .init(translationX: 0, y: -frame.size.height)
        parent.view.addSubview(self)
        debugPrint("😀 present duration: \(presentDuration)")
        UIView.animate(withDuration: presentDuration) {
            self.transform = .identity
        } completion: { _ in
            self.setupDismiss()
        }
    }
    
    private func setupDismiss() {
        task = DispatchWorkItem(block: { [weak self] in
            guard let self = self else { return }
            if self.task == nil { return }
            self.task = nil
            debugPrint("😀 hide duration: \(self.dismissDuration)")
            UIView.animate(
                withDuration: self.dismissDuration
            ) {
                self.transform = .init(
                    translationX: 0,
                    y: -self.frame.size.height
                )
            } completion: { _ in
                self.removeFromSuperview()
                self.onDismissCallback?()
                self.onDismissCallback = nil
            }
        })
        debugPrint("😀 show duration: \(showDuration)")
        DispatchQueue.main.asyncAfter(
            deadline: .now() + showDuration,
            execute: task!
        )
    }
    
    deinit {
        debugPrint("😀 topView.deinit")
    }
}

@objc(ToasterViewManager)
class ToasterViewManager: RCTViewManager {
    override static func requiresMainQueueSetup() -> Bool {true}
    override func view() -> UIView! {HostToasterView(bridge: bridge)}
    private func getView(withTag tag: NSNumber) -> HostToasterView {
        bridge.uiManager.view(forReactTag: tag) as! HostToasterView
    }

    @objc
    final func dismiss(
        _ node: NSNumber,
        duration: NSNumber
    ) {
        DispatchQueue.main.async {
            debugPrint("😀 Root.dismiss \(duration.doubleValue)")
            let component = self.getView(withTag: node)
            component.containerView.dismiss(duration: duration.doubleValue / 1000.0)
        }
    }

    override func shadowView() -> RCTShadowView! {
        return ModalHostShadowView()
    }
}

private class HostToasterView: UIView {
    internal let containerView = ContainerView()
    private var _touchHandler: RCTTouchHandler?
    private var _reactSubview: UIView?
    private var _bridge: RCTBridge?
    private var _isPresented = false
    
    @objc
    private var onToastDismiss: RCTDirectEventBlock?

    private lazy var presentViewController: UIViewController = {
        var topController = UIApplication.shared.keyWindow?.rootViewController
        while let presentedViewController = topController?.presentedViewController {
            topController = presentedViewController
        }
        return topController!
    }()

    @objc
    var toasterParams: NSDictionary? {
        didSet {
            if let duration = toasterParams?["presentDuration"] as? TimeInterval {
                containerView.presentDuration = duration / 1000.0
            }
            
            if let duration = toasterParams?["showDuration"] as? TimeInterval {
                containerView.showDuration = duration / 1000.0
            }
            
            if let duration = toasterParams?["dismissDuration"] as? TimeInterval {
                containerView.dismissDuration = duration / 1000.0
            }
        }
    }

    init(bridge: RCTBridge) {
        self._bridge = bridge
        super.init(frame: .zero)
        _touchHandler = RCTTouchHandler(bridge: bridge)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func insertReactSubview(_ subview: UIView!, at atIndex: Int) {
        debugPrint("😀insertReactSubview")
        super.insertReactSubview(subview, at: atIndex)
        _touchHandler?.attach(to: subview)
        containerView.addSubview(subview)
        _reactSubview = subview
    }

    override func removeReactSubview(_ subview: UIView!) {
        debugPrint("😀removeReactSubview")
        super.removeReactSubview(subview)
        _touchHandler?.detach(from: subview)
        _reactSubview = nil
    }

    // need to leave it empty
    override func didUpdateReactSubviews() {}

    override func didMoveToWindow() {
        super.didMoveToWindow()
        if (!self.isUserInteractionEnabled && self.superview?.reactSubviews().contains(self) != nil) {
          return;
        }

        if (!_isPresented && self.window != nil) {
            _isPresented = true
            var size: CGSize = .zero
            DispatchQueue.main.async { [weak self] in
                guard let self = self else { return }
                
                self._reactSubview?.setNeedsLayout()
                self._reactSubview?.layoutIfNeeded()
                self._reactSubview?.sizeToFit()
                size = self._reactSubview?.frame.size ?? .zero
                
                containerView.frame.size = size
                containerView.frame.origin = .zero

                debugPrint("😀 attachedViews \(self.reactTag.intValue) \(size)")
                ModalHostShadowView.attachedViews[self.reactTag.intValue] = self

                containerView.presentIn(parent: presentViewController) { [weak self] in
                    self?.onToastDismiss?([:])
                }
            }
        }
    }

    override func didMoveToSuperview() {
        super.didMoveToSuperview()
        debugPrint("😀 didMoveToSuperview")
        if _isPresented && superview == nil {
            destroy()
        }
    }

    func destroy() {
        debugPrint("😀 destroy")
        _isPresented = false

        ModalHostShadowView.attachedViews.removeValue(forKey: self.reactTag.intValue)
        _reactSubview?.removeFromSuperview()
        _touchHandler?.detach(from: _reactSubview)
        _touchHandler = nil
        _bridge = nil
        onToastDismiss = nil
        _reactSubview = nil
        debugPrint("++++ \(ModalHostShadowView.attachedViews.count)")
    }

    deinit {
        debugPrint("😀deinit")
    }

}
