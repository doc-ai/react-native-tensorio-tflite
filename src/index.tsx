import { NativeModules } from 'react-native';

type TensorioTfliteType = {
  multiply(a: number, b: number): Promise<number>;
};

const { TensorioTflite } = NativeModules;

export default TensorioTflite as TensorioTfliteType;
