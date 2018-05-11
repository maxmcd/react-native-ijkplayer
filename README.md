# React Native ijkplayer

A react native component for [ijkplayer](https://github.com/Bilibili/ijkplayer)

### iOS

Add podspec to your Podfile.

```ruby
pod 'react-native-ijkplayer', :path => '../node_modules/react-native-ijkplayer/ios/react-native-ijkplayer.podspec'
```

### Android

ijk dependencies is satisfied using gradle for Android

```
cd react-native-ijkplayer/Example/android
./gradlew installDebug
```

### Usage

```js
import IJKPlayer from "react-native-ijkplayer";
() => (
    <IJKPlayer
        style={{}}
        headers={{}}
        source={{
            uri: "http://techslides.com/demos/sample-videos/small.mp4"
        }}
        paused={false}
        muted={flase}
        paused={true}
        volume={1.0}
        onLoadStart={}
        onLoad={}
        onBuffer={}
        onError={}
        onProgress={}
        onPause={}
        onStop={}
        onEnd={}
    />
);
```

### Building

Build for iOS following the instructions at https://github.com/Bilibili/ijkplayer. You must create release builds for both iOS and the simulator.

```bash
cd ~/Library/Developer/Xcode/DerivedData/IJKMediaDemo-*/Build/Products/
lipo -create Release-iphoneos/IJKMediaFramework.framework/IJKMediaFramework Release-iphonesimulator/IJKMediaFramework.framework/IJKMediaFramework -output IJKMediaFramework
cp IJKMediaFramework Release-iphoneos/IJKMediaFramework.framework/
cp -R Release-iphoneos/IJKMediaFramework.framework react-native-ijkplayer/ios/
```
