import React, { memo, useEffect, useRef, useState } from 'react';
import { ToasterView } from './ToasterView';
import { Emitter } from './Emitter';

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
    Params extends Parameters<(typeof config)[Key]>[0]
  >(params: {
    key: Key;
    props?: Params;
    duration?: Duration;
    onDismiss?: () => void;
  }) {
    events.emit('showToast', { ...params, config });
  };
}

export function dismissToast(duration?: number) {
  events.emit('hideToast', duration);
}

interface PresentData {
  readonly config: ToastConfig;
  readonly key: any;
  readonly props?: any;
  readonly duration?: Duration;
  readonly onDismiss?: () => void;
}

export const ToastRoot: React.FC = memo(() => {
  const ref = useRef<ToasterView>(null);
  const [presentData, setPresentData] = useState<PresentData | undefined>();
  useEffect(() => {
    const showToast = events.on('showToast', (params) => {
      setPresentData(params);
    });

    const hideToast = events.on('hideToast', (duration) => {
      ref.current?.hide(duration);
    });
    return () => {
      showToast();
      hideToast();
    };
  }, []);

  useEffect(() => {
    if (!presentData) return;
    ref.current?.show(presentData.duration);
  }, [presentData]);

  if (!presentData) return null;

  return (
    <ToasterView
      ref={ref}
      onDismiss={() => {
        presentData?.onDismiss?.();
        setPresentData(undefined);
      }}
      children={presentData.config[presentData.key]!(presentData.props)}
    />
  );
});
