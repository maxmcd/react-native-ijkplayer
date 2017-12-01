import React from 'react';
import {
    Image,
    StatusBar,
    Dimensions,
    StyleSheet,
    TouchableOpacity,
    Text,
    Slider,
    View,
    Animated,
    ActivityIndicatorIOS,
    ProgressBarAndroid,
    Platform,
} from 'react-native';
import RCTIJKPlayer from 'react-native-ijkplayer';
import RCTIJKPlayerWithController from 'react-native-ijkplayer/RCTIJKPlayerWithController';
var {height, width} = Dimensions.get('window');
console.log("width, height", width, height);
import Icon from 'react-native-vector-icons/FontAwesome';
const iconSize = 120;

const styles = StyleSheet.create({
    container: {
        flex: 1,
    },
    player: {
        width: width,
        height: height,
        backgroundColor: 'rgba(0,0,0,1)',
    },
});


export default class Example extends React.Component {
    constructor(props) {
        super(props);
        this.rctijkplayer = null;
        this.state = {
            playBackInfo: {
            },
            fadeAnim: new Animated.Value(1),
            hasController: false,
        };
    }
    componentDidMount() {
        let url = "http://devimages.apple.com.edgekey.net/streaming/examples/bipbop_4x3/gear1/prog_index.m3u8";
        //let url = "http://vali-dns.cp31.ott.cibntv.net/677871F4FD937715A13F83244/03000A0E0459C2C4E07817000000018D377067-DF9D-2082-D194-820093D94914.mp4?ccode=0502&duration=390&expire=18000&psid=14070376641e6f4ab6205607837619e1&ups_client_netip=7d545025&ups_ts=1512037873&ups_userid=1130129069&utid=PGo0ErG9ZHICAXHNpcytfSLP&vid=XMTg5NDUxNDYyNA%3D%3D&vkey=A1d80b0519f959bb53fe0dbfcfeb5f306";
        // let url = "/Users/cong/Downloads/111.mov";
        this.rctijkplayer.start({url: url});
    }
    componentWillUnmount() {
        clearInterval(this.playbackInfoUpdater);
    }

    render() {
        return (<View
                style={styles.container}
                >
                <StatusBar
                animated
                hidden
                />
                <RCTIJKPlayerWithController
                ref={(rctijkplayer) => {
                    this.rctijkplayer = rctijkplayer;
                }}
                style={styles.player}
                height={height/2}
                width={width}
                left={0}
                top={100}
                >
                </RCTIJKPlayerWithController>
                </View>
               );
    }
}
