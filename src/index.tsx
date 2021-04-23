import { NativeModules } from 'react-native';

type TensorioTfliteType = {
  /**
   * Loads a file model or asset model and gives it a name that is used
   * by the remaining functions.
   * @param path An absolute path to a model file or a relative path to a bundled model.
   * @param name A name of your choice to give to the model, used by the remaining functions.
   */

  load(path: string, name: string): Promise<boolean>;

  /**
   * Unloads a model, freeing the resources used by it. Models can have
   * a large memory footprint. Call this method when you are done using
   * a model.
   * @param name The name you gave to the model.
   */

  unload(name: string): Promise<boolean>;

  /**
   * Performs inference with a model.
   * @param name The name you gave to the model.
   * @param data The Object data to perform inference on
   */

  run(name: string, data: object): Promise<object>;

  /**
   * An image classification utility for finding the top N image classification
   * results.
   * @param count The maximum number N of results to return
   * @param threshold The minimum classification score
   * @param classifications The complete classifications object returned by the model
   */

  topN(
    count: number,
    threshold: number,
    classifications: object
  ): Promise<object>;
};

const { TensorioTflite } = NativeModules;

export default TensorioTflite as TensorioTfliteType;
