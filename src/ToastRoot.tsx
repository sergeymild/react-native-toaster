import React, { memo, useEffect, useRef, useState } from "react";
//import { ToasterView } from './ToasterView';
import { Emitter } from "./Emitter";
import { Animated, View } from "react-native";

interface ToastEvents {
  showToast: (params: PresentData) => void;
  hideToast: (duration?: number) => void;
}

const events = new Emitter<ToastEvents>();

export type ToastConfig = {
  [k: string]: (params: any) => React.ReactElement;
};

type Duration = { present?: number; dismiss?: number; visible?: number };

export function configureToasts<T extends ToastConfig, K extends keyof T>(
  config: T
) {
  return function <
    Key extends K,
    Params extends Parameters<typeof config[Key]>[0]
  >(params: {
    key: Key;
    props?: Params;
    duration?: Duration;
    onDismiss?: () => void;
  }) {
    events.emit("showToast", { ...params, config });
  };
}

export function dismissToast(duration?: number) {
  events.emit("hideToast", duration);
}

interface PresentData {
  readonly config: ToastConfig;
  readonly key: any;
  readonly props?: any;
  readonly duration?: Duration;
  readonly onDismiss?: () => void;
}

export const ToastRoot: React.FC = memo(() => {
  //const ref = useRef<ToasterView>(null);
  const animatedRef = useRef<Animated.Value>();
  const timeoutRef = useRef<number | undefined>(undefined);
  const isDestroyedRef = useRef(false);
  if (!animatedRef.current) {
    animatedRef.current = new Animated.Value(0);
  }
  const [viewHeight, setViewHeight] = useState<number>(-1);
  const [presentData, setPresentData] = useState<PresentData | undefined>();
  useEffect(() => {
    const showToast = events.on("showToast", (params) => {
      clearTimeout(timeoutRef.current);
      if (!isDestroyedRef.current) {
        setPresentData(undefined);
        return setTimeout(() => setPresentData(params), 0);
      }
      isDestroyedRef.current = false;
      setPresentData(params);
    });

    const hideToast = events.on("hideToast", (duration: number | undefined) => {
      //ref.current?.hide(duration);
      if (isDestroyedRef.current) return;
      isDestroyedRef.current = true;
      Animated.timing(animatedRef.current!, {
        toValue: -viewHeight,
        duration: duration ?? 250,
        useNativeDriver: true,
      }).start(() => {
        setPresentData(undefined);
        setViewHeight(-1);
      });
    });
    return () => {
      showToast();
      hideToast();
    };
  }, []);

  useEffect(() => {
    return () => {
      isDestroyedRef.current = true;
    };
  }, []);

  useEffect(() => {
    clearTimeout(timeoutRef.current);
    if (!presentData) return;
    if (viewHeight === -1) return;
    Animated.timing(animatedRef.current!, {
      toValue: 0,
      duration: presentData.duration?.present ?? 250,
      useNativeDriver: true,
    }).start(() => {
      console.log("[ToastRoot1.end]");
    });
    //ref.current?.show(presentData.duration);
    timeoutRef.current = setTimeout(() => {
      if (isDestroyedRef.current) return;
      isDestroyedRef.current = true;
      Animated.timing(animatedRef.current!, {
        toValue: -viewHeight,
        duration: presentData.duration?.present ?? 250,
        useNativeDriver: true,
      }).start(() => {
        console.log("[ToastRoot.end]");
        setPresentData(undefined);
        setViewHeight(-1);
      });
    }, presentData.duration?.visible ?? 4000);
  }, [presentData, viewHeight]);

  if (!presentData) return null;

  return (
    <Animated.View
      onLayout={(e) => {
        if (viewHeight === -1) {
          setViewHeight(e.nativeEvent.layout.height);
          animatedRef.current?.setValue(-e.nativeEvent.layout.height);
        }
      }}
      style={{
        position: "absolute",
        top: 0,
        left: 0,
        right: 0,
        zIndex: 1000,
        opacity: viewHeight === -1 ? 0 : 1,
        transform: [{ translateY: animatedRef.current }],
      }}
      children={presentData.config[presentData.key]!(presentData.props)}
    />
  );

  // return (
  //   <ToasterView
  //     ref={ref}
  //     onDismiss={() => {
  //       presentData?.onDismiss?.();
  //       setPresentData(undefined);
  //     }}
  //     children={presentData.config[presentData.key]!(presentData.props)}
  //   />
  // );
});
