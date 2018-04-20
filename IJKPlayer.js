import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { StyleSheet, requireNativeComponent, View, ViewPropTypes } from 'react-native';
import resolveAssetSource from 'react-native/Libraries/Image/resolveAssetSource';

const styles = StyleSheet.create({
  base: {
    overflow: 'hidden',
  },
});

export default class IJKPlayer extends Component {

  constructor(props) {
    super(props);
  }

  setNativeProps(nativeProps) {
    this._root.setNativeProps(nativeProps);
  }

  _assignRoot = (component) => {
    this._root = component;
  };

  _onLoadStart = (event) => {
    if (this.props.onLoadStart) {
      this.props.onLoadStart(event.nativeEvent);
    }
  };

  _onLoad = (event) => {
    if (this.props.onLoad) {
      this.props.onLoad(event.nativeEvent);
    }
  };

  _onError = (event) => {
    if (this.props.onError) {
      this.props.onError(event.nativeEvent);
    }
  };

  _onProgress = (event) => {
    if (this.props.onProgress) {
      this.props.onProgress(event.nativeEvent);
    }
  };

  _onPause = (event) => {
    if (this.props.onPause) {
      this.props.onPause(event.nativeEvent);
    }
  };

  _onStop = (event) => {
    if (this.props.onStop) {
      this.props.onStop(event.nativeEvent);
    }
  };

  _onEnd = (event) => {
    if (this.props.onEnd) {
      this.props.onEnd(event.nativeEvent);
    }
  };

  _onBuffer = (event) => {
    if (this.props.onBuffer) {
      this.props.onBuffer(event.nativeEvent);
    }
  };

  render() {
    const options = this.props.options || [];
    const source = resolveAssetSource(this.props.source) || {};

    let uri = source.uri || '';
    if (uri && uri.match(/^\//)) {
      uri = `file://${uri}`;
    }

    const nativeProps = Object.assign({}, this.props);
    Object.assign(nativeProps, {
      style: [ styles.base, nativeProps.style ],
      src: {
        uri,
        options,
      },
      onVideoLoadStart: this._onLoadStart,
      onVideoLoad: this._onLoad,
      onVideoError: this._onError,
      onVideoProgress: this._onProgress,
      onVideoPause: this._onPause,
      onVideoStop: this._onStop,
      onVideoEnd: this._onEnd,
      onVideoBuffer: this._onBuffer,
    });

    return (
      <RCTIJKPlayer
        ref={this._assignRoot}
        {...nativeProps} />
    );
  }

}

IJKPlayer.propTypes = {
  /* Native only */
  src: PropTypes.object,
  onVideoLoadStart: PropTypes.func,
  onVideoLoad: PropTypes.func,
  onVideoBuffer: PropTypes.func,
  onVideoError: PropTypes.func,
  onVideoProgress: PropTypes.func,
  onVideoPause: PropTypes.func,
  onVideoStop: PropTypes.func,
  onVideoEnd: PropTypes.func,

  /* Wrapper component */
  options: PropTypes.array,
  source: PropTypes.oneOfType([
    PropTypes.shape({
      uri: PropTypes.string
    }),
    // Opaque type returned by require('./video.mp4')
    PropTypes.number
  ]),
  muted: PropTypes.bool,
  paused: PropTypes.bool,
  volume: PropTypes.number,
  onLoadStart: PropTypes.func,
  onLoad: PropTypes.func,
  onBuffer: PropTypes.func,
  onError: PropTypes.func,
  onProgress: PropTypes.func,
  onPause: PropTypes.func,
  onStop: PropTypes.func,
  onEnd: PropTypes.func,

  /* Required by react-native */
  scaleX: PropTypes.number,
  scaleY: PropTypes.number,
  translateX: PropTypes.number,
  translateY: PropTypes.number,
  rotation: PropTypes.number,
  ...ViewPropTypes,
};

const RCTIJKPlayer = requireNativeComponent('RCTIJKPlayer', IJKPlayer, {
  nativeOnly: {
    src: true,
  },
});
