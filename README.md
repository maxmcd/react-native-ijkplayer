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
import IJKPlayer from "react-native-ijkplayer"
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
    onLoadStart={e => console.log(e)}
    onLoad={e => console.log(e)}
    onBuffer={e => console.log(e)}
    onError={e => console.log(e)}
    onProgress={e => console.log(e)}
    onPause={e => console.log(e)}
    onStop={e => console.log(e)}
    onEnd={e => console.log(e)}
/>
```
