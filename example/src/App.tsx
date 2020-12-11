import * as React from 'react';
import { Platform, StyleSheet, View, Text } from 'react-native';
import TensorioTflite from 'react-native-tensorio-tflite';

export default function App() {
  const [results, setResults] = React.useState<object | undefined>();

  React.useEffect(() => {
    TensorioTflite.load('image-classification.tiobundle', 'classifier');
    
    // TensorioTflite.imageKeyData    RNTIOImageKeyData
    // TensorioTflite.imageKeyFormat  RNTIOImageKeyFormat 
    // TensorioTflite.imageTypeAsset  RNTIOImageDataTypeAsset (6)

    const imageAsset = Platform.OS === 'ios' ? 'elephant' : 'elephant.jpg';
    const imageFormat = 6; // asset

    TensorioTflite.run('classifier', {
      'image': {
        'RNTIOImageKeyFormat': imageFormat,
        'RNTIOImageKeyData': imageAsset
      }
    }).then((output) => {
      // @ts-ignore
      return TensorioTflite.topN(5, 0.1, output['classification'])
    }).then(setResults);

    TensorioTflite.unload('classifier');
  }, []);

  return (
    <View style={styles.container}>
      <Text>Result: {JSON.stringify(results, null, 2)}</Text>
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
