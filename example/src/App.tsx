import * as React from 'react';
import { StyleSheet, View, Text } from 'react-native';
import TensorioTflite from 'react-native-tensorio-tflite';

export default function App() {
  const [result, setResult] = React.useState<number | undefined>();
  const [results, setResults] = React.useState<object | undefined>();

  React.useEffect(() => {
    TensorioTflite.load('image-classification.tiobundle', 'classifier');
    
    // TensorioTflite.imageKeyData    RNTIOImageKeyData
    // TensorioTflite.imageKeyFormat  RNTIOImageKeyFormat 
    // TensorioTflite.imageTypeAsset  RNTIOImageDataTypeAsset (6)

    TensorioTflite.run('classifier', {
      'image': {
        'RNTIOImageKeyData': 'elephant', 
        'RNTIOImageKeyFormat': 6
      }
    }).then((output) => {
      TensorioTflite.topN(5, 0.1, output['classification']).then(setResults);
    });

    TensorioTflite.unload('classifier');

    TensorioTflite.multiply(3, 7).then(setResult);
  }, []);

  return (
    <View style={styles.container}>
      <Text>{JSON.stringify(results, null, 2)}</Text>
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
