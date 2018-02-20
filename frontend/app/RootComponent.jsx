import React from 'react';
import LoginComponent from './LoginComponent.jsx';
import PropTypes from 'prop-types';

class RootComponent extends React.Component {
    static propTypes = {
        onLoggedIn: PropTypes.func.isRequired,
        onLoggedOut: PropTypes.func.isRequired,
        currentUsername: PropTypes.string,
        isLoggedIn: PropTypes.bool.isRequired
    };

    render() {
        return(<div>
            <p>Please select an option on the left</p>
            <LoginComponent onLoggedIn={this.props.onLoggedIn}
                            onLoggedOut={this.props.onLoggedOut}
                            username={this.props.currentUsername}
                            currentlyLoggedIn={this.props.isLoggedIn}
            />
        </div>);
    }
}

export default RootComponent;