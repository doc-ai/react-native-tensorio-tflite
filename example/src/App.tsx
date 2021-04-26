import * as React from 'react';
import { Platform, StyleSheet, View, Text, Image } from 'react-native';
import TensorioTflite from 'react-native-tensorio-tflite';
const iosElephant = require('../ios/TensorioTfliteExample/Images.xcassets/elephant.imageset/elephant.jpg');
const androidElephant = require('../android/app/src/main/assets/elephant.jpg');
const {
  imageKeyFormat,
  imageKeyData,
  imageTypeAsset,
  // @ts-ignore
} = TensorioTflite.getConstants();
const imageAsset = Platform.OS === 'ios' ? 'elephant' : 'elephant.jpg';
const image = Platform.OS === 'ios' ? iosElephant : androidElephant;

export default function App() {
  const [results, setResults] = React.useState<object | undefined>();

  React.useEffect(() => {
    TensorioTflite.load('image-classification.tiobundle', 'classifier');

    TensorioTflite.run('classifier', {
      image: {
        [imageKeyFormat]: imageTypeAsset,
        [imageKeyData]: imageAsset,
      },
    })
      .then((output) => {
        // @ts-ignore
        return TensorioTflite.topN(5, 0.1, output.classification);
      })
      .then(setResults)
      .catch((error) => {
        console.log(error);
      });

    TensorioTflite.unload('classifier');
  }, []);

  return (
    <View style={styles.container}>
      <View style={styles.imageWrapper}>
        <View style={styles.imageContainer}>
          <Image resizeMode="contain" source={image} style={styles.image} />
        </View>
      </View>
      <Text style={styles.heading}>Results: </Text>
      <View>
        {results &&
          Object.entries(results).map(([key, value]) => (
            <View style={styles.statWrapper}>
              <Text style={styles.results}>{key}: </Text>
              <Text style={styles.results}>{value}</Text>
            </View>
          ))}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  imageContainer: {
    overflow: 'hidden',
    borderRadius: 15,
    borderColor: 'lightgray',
    borderWidth: 2,
    shadowColor: '#000',
    elevation: 3,
  },
  imageWrapper: {
    marginBottom: 25,
    shadowRadius: 20,
    shadowOpacity: 0.25,
    shadowOffset: {
      width: 0,
      height: 10,
    },
  },
  image: {
    width: 300,
    height: 150,
  },
  statWrapper: {
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  heading: {
    fontSize: 16,
    fontWeight: '600',
  },
  results: {
    fontSize: 16,
  },
});
