import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-camera' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const Camera = NativeModules.Camerax
  ? NativeModules.Camerax
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export function toggleCamera(): Promise<boolean> {
  return Camera.toggleCamera();
}

export function getDummyText(dateString: string): Promise<string> {
  return Camera.getDummyText(dateString);
}
