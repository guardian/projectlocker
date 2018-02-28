import React from 'react';
import PropTypes from 'prop-types';

class UserEntryView extends React.Component {
    static propTypes = {
        username: PropTypes.string.isRequired
    };

    constructor(props) {
        super(props);
    }

    render() {
        return <span><i className="fa fa-user" style={{ marginRight: "3px", marginLeft: "5px"}}/>
                    <span className="emphasis">{this.props.username}</span></span>
    }
}

export default UserEntryView;