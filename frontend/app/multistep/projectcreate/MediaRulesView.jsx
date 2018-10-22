import React from 'react';
import PropTypes from 'prop-types';

class MediaRulesView extends React.Component {
    static propTypes = {
        deletable: PropTypes.bool.isRequired,
        deep_archive: PropTypes.bool.isRequired,
        sensitive: PropTypes.bool.isRequired
    };

    render() {
        return <span>
            <span className="media_rules" style={{display: this.props.deep_archive ? "inline-block":"none"}}>Deep Archive</span>
            <span className="media_rules" style={{display: this.props.deletable ? "inline-block":"none"}}>Deletable</span>
            <span className="media_rules" style={{display: this.props.sensitive ? "inline-block":"none"}}>Sensitive</span>
        </span>
    }

}

export default MediaRulesView;