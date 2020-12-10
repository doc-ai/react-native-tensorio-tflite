import { NativeModules } from 'react-native';

type TensorioTfliteType = {
  multiply(a: number, b: number): Promise<number>;
  
  load(path: string, name: string): Promise<boolean>;
  unload(name: string): Promise<boolean>

  run(name: string, data: object): Promise<object>

  topN(count: number, threshold: number, classifications: object): Promise<object>
};

const { TensorioTflite } = NativeModules;

export default TensorioTflite as TensorioTfliteType;
