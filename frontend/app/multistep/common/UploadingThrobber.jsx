import React from 'react';
import PropTypes from 'prop-types';

class UploadingThrobber extends React.Component {
    static propTypes = {
        loading: PropTypes.bool.isRequired
    };

    render(){
        return <img src="/assets/images/uploading.svg" style={{display: this.props.loading ? "inline" : "none",height: "20px" }}/>
    }
}

export default UploadingThrobber;