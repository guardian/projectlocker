import React from 'react'
import PropTypes from 'prop-types';

class UploadNewVersionComponent extends React.Component {
    static propTypes = {
        newVersionCb: PropTypes.func.isRequired,
        cancelCb: PropTypes.func.isRequired
    };

    render() {
        return <p className="error-text">A file with this name already exists.  Would you like to create a new version of the existing object, or create a new one with a different name?
            <a onClick={this.props.newVersionCb} className="clickable-js-link">Create new version</a>
            <a onClick={this.props.cancelCb} className="clickable-js-link">Cancel and upload with a new name</a>
        </p>;
    }
}

export default UploadNewVersionComponent;