
/*
  MapUtil exposes a set of helper methods for working with
  ReadableMap (by React Native), Map<String, Object>, and JSONObject.

  MIT License

  Copyright (c) 2020 Marc Mendiola

  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all
  copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  SOFTWARE.
 */

// See https://gist.github.com/mfmendiola/bb8397162df9f76681325ab9f705748b

package com.reactnativetensoriotflite;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableMap;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

public class MapUtil {

  public static JSONObject toJSONObject(ReadableMap readableMap) throws JSONException {
    JSONObject jsonObject = new JSONObject();

    ReadableMapKeySetIterator iterator = readableMap.keySetIterator();

    while (iterator.hasNextKey()) {
      String key = iterator.nextKey();
      ReadableType type = readableMap.getType(key);

      switch (type) {
        case Null:
          jsonObject.put(key, null);
          break;
        case Boolean:
          jsonObject.put(key, readableMap.getBoolean(key));
          break;
        case Number:
          jsonObject.put(key, readableMap.getDouble(key));
          break;
        case String:
          jsonObject.put(key, readableMap.getString(key));
          break;
        case Map:
          jsonObject.put(key, MapUtil.toJSONObject(readableMap.getMap(key)));
          break;
        case Array:
          jsonObject.put(key, ArrayUtil.toJSONArray(readableMap.getArray(key)));
          break;
      }
    }

    return jsonObject;
  }

  public static Map<String, Object> toMap(JSONObject jsonObject) throws JSONException {
    Map<String, Object> map = new HashMap<>();
    Iterator<String> iterator = jsonObject.keys();

    while (iterator.hasNext()) {
      String key = iterator.next();
      Object value = jsonObject.get(key);

      if (value instanceof JSONObject) {
        value = MapUtil.toMap((JSONObject) value);
      }
      if (value instanceof JSONArray) {
        value = ArrayUtil.toArray((JSONArray) value);
      }

      map.put(key, value);
    }

    return map;
  }

  public static Map<String, Object> toMap(ReadableMap readableMap) {
    Map<String, Object> map = new HashMap<>();
    ReadableMapKeySetIterator iterator = readableMap.keySetIterator();

    while (iterator.hasNextKey()) {
      String key = iterator.nextKey();
      ReadableType type = readableMap.getType(key);

      switch (type) {
        case Null:
          map.put(key, null);
          break;
        case Boolean:
          map.put(key, readableMap.getBoolean(key));
          break;
        case Number:
          map.put(key, readableMap.getDouble(key));
          break;
        case String:
          map.put(key, readableMap.getString(key));
          break;
        case Map:
          map.put(key, MapUtil.toMap(readableMap.getMap(key)));
          break;
        case Array:
          map.put(key, ArrayUtil.toArray(readableMap.getArray(key)));
          break;
      }
    }

    return map;
  }

  public static WritableMap toWritableMap(Map<String, Object> map) {
    WritableMap writableMap = Arguments.createMap();
    Iterator iterator = map.entrySet().iterator();

    while (iterator.hasNext()) {
      Map.Entry pair = (Map.Entry)iterator.next();
      Object value = pair.getValue();

      if (value == null) {
        writableMap.putNull((String) pair.getKey());
      } else if (value instanceof Boolean) {
        writableMap.putBoolean((String) pair.getKey(), (Boolean) value);
      } else if (value instanceof Double) {
        writableMap.putDouble((String) pair.getKey(), (Double) value);
      } else if (value instanceof Float) {
        // +@pdow
        writableMap.putDouble((String) pair.getKey(), (Double) ((Float) value).doubleValue());
      } else if (value instanceof Integer) {
        writableMap.putInt((String) pair.getKey(), (Integer) value);
      } else if (value instanceof String) {
        writableMap.putString((String) pair.getKey(), (String) value);
      } else if (value instanceof Map) {
        writableMap.putMap((String) pair.getKey(), MapUtil.toWritableMap((Map<String, Object>) value));
      } else if (value instanceof boolean[] ) {
         // +@pdow
        if ( ((boolean[]) value).length == 1 ) {
          writableMap.putBoolean((String) pair.getKey(), ((boolean[]) value)[0]);
        } else {
          writableMap.putArray((String) pair.getKey(), ArrayUtil.toWritableArray((boolean[]) value));
        }
      } else if (value instanceof double[] ) {
         // +@pdow
        if ( ((double[]) value).length == 1 ) {
          writableMap.putDouble((String) pair.getKey(), ((double[]) value)[0]);
        } else {
          writableMap.putArray((String) pair.getKey(), ArrayUtil.toWritableArray((double[]) value));
        }
      } else if (value instanceof float[] ) {
         // +@pdow
        if ( ((float[]) value).length == 1 ) {
          writableMap.putDouble((String) pair.getKey(), ((float[]) value)[0]);
        } else {
          writableMap.putArray((String) pair.getKey(), ArrayUtil.toWritableArray((float[]) value));
        }
      } else if (value instanceof int[] ) {
        // +@pdow
        if ( ((int[]) value).length == 1 ) {
          writableMap.putInt((String) pair.getKey(), ((int[]) value)[0]);
        } else {
          writableMap.putArray((String) pair.getKey(), ArrayUtil.toWritableArray((int[]) value));
        }
      } else if (value.getClass() != null && value.getClass().isArray()) {
        writableMap.putArray((String) pair.getKey(), ArrayUtil.toWritableArray((Object[]) value));
      }

      iterator.remove();
    }

    return writableMap;
  }
}
