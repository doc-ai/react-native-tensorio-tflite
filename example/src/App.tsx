import * as React from 'react';
import { Platform, StyleSheet, View, Text } from 'react-native';
import TensorioTflite from 'react-native-tensorio-tflite';

// @ts-ignore
const { imageKeyFormat, imageKeyData, imageTypeAsset } = TensorioTflite.getConstants();
const imageAsset = Platform.OS === 'ios' ? 'elephant' : 'elephant.jpg';

export default function App() {
  const [results, setResults] = React.useState<object | undefined>();

  React.useEffect(() => {
    TensorioTflite.load('image-classification.tiobundle', 'classifier');
    
    TensorioTflite
      .run('classifier', {
        'image': {
          [imageKeyFormat]: imageTypeAsset,
          [imageKeyData]: imageAsset
        }
      })
      .then(output => {
        // @ts-ignore
        return TensorioTflite.topN(5, 0.1, output['classification'])
      })
      .then(setResults)
      .catch(error => {
        console.log(error)
      });

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
