import * as React from 'react';

import {
  StatusBar,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { configureToasts, ToastRoot, dismissToast } from 'react-native-toaster';

const emitter = configureToasts({
  message: (props: { username: string; surname: string }) => {
    return (
      <View style={{ paddingTop: 100, backgroundColor: 'red' }}>
        <Text>{props.username}</Text>
        <Text>{props.surname}</Text>
      </View>
    );
  },

  warning: (props: { message: string }) => {
    return (
      <View
        style={{
          marginTop: StatusBar.currentHeight ?? 56,
          backgroundColor: 'green',
        }}
      >
        <Text>
          Lorem ipsum dolor sit amet, consectetur adipisicing elit. Adipisci,
          aliquam ducimus eligendi esse et exercitationem id laborum mollitia
          nemo perspiciatis placeat porro quaerat, ratione reiciendis totam ut
          veritatis. Omnis, ut! Lorem ipsum dolor sit amet, consectetur
          adipisicing elit. Consequatur, eaque illo ipsum itaque molestias
          quisquam recusandae sint veniam. Amet autem fugit ipsum iure molestias
          non quaerat quos vel veniam vitae!
        </Text>
      </View>
    );
  },
});

export default function App() {
  return (
    <>
      <View style={styles.container}>
        <TouchableOpacity
          style={{ marginTop: 300 }}
          onPress={() => {
            emitter({
              key: 'warning',
              props: { message: 'warning' },
              duration: { show: 1000, present: 250, dismiss: 250 },
            });
          }}
        >
          <Text>Present</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={{ marginTop: 350 }}
          onPress={() => {
            dismissToast();
            //ref.current?.hide(250);
          }}
        >
          <Text>Dismiss</Text>
        </TouchableOpacity>
      </View>

      <ToastRoot />
    </>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    flexGrow: 1,
    backgroundColor: 'orange',
  },
});
