import React, { useState, useEffect } from 'react';
import {
  getDummyText,
  requestCameraPermission,
  checkCameraPermission,
  startCamera,
  stopCamera,
} from 'react-native-camerax';
import { Text, View, StyleSheet, Button, Alert } from 'react-native';

export default function App() {
  const [dummyText, setDummyText] = useState<string | undefined>();
  const [hasPermission, setHasPermission] = useState<boolean | null>(null);
  const [isCameraActive, setIsCameraActive] = useState(false);
  const [currentDate, setCurrentDate] = useState(new Date());

  useEffect(() => {
    // Fetch dummy text with the current date and time
    getDummyText(currentDate.toISOString()).then(setDummyText);
    checkCameraPermission().then(setHasPermission);
  }, [currentDate]);

  const handleRequestPermission = async () => {
    try {
      const granted = await requestCameraPermission();
      setHasPermission(granted);
      if (!granted) {
        Alert.alert(
          'Permission Denied',
          'Camera permission is required to use the camera'
        );
      }
    } catch (error) {
      console.error(error);
      Alert.alert('Error', 'Failed to request camera permission');
    }
  };

  const handleToggleCamera = async () => {
    try {
      if (isCameraActive) {
        await stopCamera();
        setIsCameraActive(false);
      } else {
        await startCamera();
        setIsCameraActive(true);
      }
    } catch (error) {
      console.error(error);
      Alert.alert('Error', 'Failed to toggle camera');
    }
  };

  const handleUpdateDateTime = () => {
    // Update the date and time
    setCurrentDate(new Date());
  };

  return (
    <View style={styles.container}>
      <Text>{dummyText || 'Loading...'}</Text>
      <Text>
        Camera Permission: {hasPermission ? 'Granted' : 'Not Granted'}
      </Text>
      <Text>Current Date and Time: {currentDate.toLocaleString()}</Text>
      {!hasPermission && (
        <Button
          title="Request Camera Permission"
          onPress={handleRequestPermission}
        />
      )}
      {hasPermission && (
        <Button
          title={isCameraActive ? 'Stop Camera' : 'Start Camera'}
          onPress={handleToggleCamera}
        />
      )}
      <Button title="Update Date and Time" onPress={handleUpdateDateTime} />
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