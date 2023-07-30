import React from 'react';
import {
  findNodeHandle,
  NativeModules,
  requireNativeComponent,
  View,
} from 'react-native';

export const _ToasterView = requireNativeComponent<any>('ToasterView');
const ToasterModule = NativeModules.ToasterView;

interface Props {
  readonly onDismiss?: () => void;
}

interface Duration {
  show?: number;
  present?: number;
  dismiss?: number;
}

interface State {
  show: boolean;
  duration: Duration;
}

export class ToasterView extends React.Component<Props, State> {
  private toastRef = React.createRef<any>();
  constructor(props: Props) {
    super(props);
    this.state = {
      show: false,
      duration: { dismiss: 250, present: 250, show: 4000 },
    };
  }

  show = (duration?: Duration) => {
    this.setState({
      show: true,
      duration: duration ?? this.state.duration,
    });
  };

  hide = (duration?: number) => {
    if (!this.state.show) return;
    const tag = findNodeHandle(this.toastRef.current);
    if (!tag) return;
    console.log('😀 [ToasterView.hide]', duration);
    ToasterModule.dismiss(tag, duration ?? this.state.duration.dismiss ?? 250);
  };

  private onDismiss = () => {
    this.setState({ show: false });
    this.props.onDismiss?.();
  };

  private toasterParams() {
    return {
      showDuration: this.state.duration.show ?? 250,
      presentDuration: this.state.duration.present ?? 4000,
      dismissDuration: this.state.duration.dismiss ?? 250,
    };
  }

  componentWillUnmount() {
    this.hide();
  }

  render() {
    if (!this.state.show) return null;
    return (
      <_ToasterView
        onToastDismiss={this.onDismiss}
        ref={this.toastRef}
        toasterParams={this.toasterParams()}
      >
        <View nativeID={'toaster-root-view'} children={this.props.children} />
      </_ToasterView>
    );
  }
}
