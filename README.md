# react-native-camerax

react-native

## Installation

```sh
npm install react-native-camerax
```

## Usage


```js
import React from 'react';
import { getDummyText, toggleCamera } from 'react-native-camerax';
import { View, StyleSheet, Button, Alert } from 'react-native';

export default function App() {
  const handleToggleCamera = async () => {
    try {
      const currentDate = new Date();
      await getDummyText(currentDate.toISOString());
      await toggleCamera();
    } catch (error) {
      console.error(error);
      Alert.alert('Error', 'Failed to toggle camera');
    }
  };

  return (
    <View style={styles.container}>
      <Button title="Toggle Camera" onPress={handleToggleCamera} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
```


## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
